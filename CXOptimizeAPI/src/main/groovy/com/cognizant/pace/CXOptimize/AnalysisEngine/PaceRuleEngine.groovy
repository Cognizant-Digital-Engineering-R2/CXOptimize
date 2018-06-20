/*
 * Copyright 2014 - 2018 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cognizant.pace.CXOptimize.AnalysisEngine

import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node
import org.slf4j.LoggerFactory


class PaceRuleEngine
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PaceRuleEngine.class)
    static def getGeneralRules(def ruleEngine = null,def configReader)
    {
        def rules = [:]
        def ruleDesc = [:]
        def ruleList
        //Read Rules configured in ES
        if(configReader?.Rules?.genericRules != null)
        {
            ruleList = configReader?.Rules?.genericRules
        }
        else
        {
            ruleList = ElasticSearchUtils.getRules(configReader.esUrl,"genericRules")
        }


        if (ruleEngine == null)
        {
            def width = [:]

            //Setting canvas property shown in HTML Page - need to remove this logic from here  and do it in UI
            width.put('High','30')
            width.put('Medium','45')
            width.put('Low','27')
            def height = '12'

            ruleList.each { it ->
                ruleDesc.put("priority",it.priority)
                ruleDesc.put("desc",it.description)
                ruleDesc.put("width",width[it.priority.toString()])
                ruleDesc.put("height",height)
                rules.put(it.id.toString(),ruleDesc.clone())
            }
        }
        else
        {
            ruleList.each { it ->
                rules.put(it.id.toString(),(it.weight.toInteger()/100))
            }
        }
        return rules
    }

    static def getSoftNavigationRules(def ruleEngine = null,def configReader)
    {
        def rules = [:]
        def ruleDesc = [:]
        def ruleList
        //Read Rules configured in ES
        if(configReader?.Rules?.softNavigationRules != null)
        {
            ruleList = configReader.Rules.softNavigationRules
        }
        else
        {
            ruleList = ElasticSearchUtils.getRules(configReader.esUrl,"softNavigationRules")
        }

        if (ruleEngine == null)
        {
            def width = [:]
            width.put('High','30')
            width.put('Medium','45')
            width.put('Low','27')
            def height = '12'

            ruleList.each { it ->
                ruleDesc.put("priority",it.priority)
                ruleDesc.put("desc",it.description)
                ruleDesc.put("width",width[it.priority.toString()])
                ruleDesc.put("height",height)
                rules.put(it.id.toString(),ruleDesc.clone())
            }
        }
        else
        {
            ruleList.each { it ->
                rules.put(it.id.toString(),(it.weight.toInteger()/100))
            }
        }
        return rules
    }

    static def getNativeAppRules(def ruleEngine = null,def configReader)
    {
        def rules = [:]
        def ruleDesc = [:]
        def ruleList

        if(configReader?.nativeRules != null)
        {
            ruleList = configReader.nativeRules
        }
        else
        {
            ruleList = ElasticSearchUtils.getRules(configReader.esUrl,"nativeAppRules")
        }

        if (ruleEngine == null)
        {
            def width = [:]
            width.put('High','30')
            width.put('Medium','45')
            width.put('Low','27')
            def height = '12'

            ruleList.each { it ->
                ruleDesc.put("priority",it.priority)
                ruleDesc.put("desc",it.description)
                ruleDesc.put("width",width[it.priority.toString()])
                ruleDesc.put("height",height)
                rules.put(it.id.toString(),ruleDesc.clone())
            }
        }
        else
        {
            ruleList.each { it ->
                rules.put(it.id.toString(),(it.weight.toInteger()/100))
            }
        }
        return rules
    }

    static def applyCustomRules(def analysisSample,def configReader)
    {
        println 'Custom Rules'
        StringBuilder recommendation = new StringBuilder()
        def rulesPath = System.getProperty("user.dir") + '/CustomRules'
        def customRules = CommonUtils.getAllRuleFiles(rulesPath)
        def noOfRules = customRules.size()
        if(noOfRules > 0)
        {
            def binding = new Binding()
            binding.setVariable("result",null)
            binding.setVariable("analysisSample",analysisSample)
            binding.setVariable("configReader",configReader)

            def shell = new GroovyShell(binding)
            for (int i = 0 ; i < noOfRules ; i++)
            {
                def expression = new File(customRules[i]).text
                shell.evaluate(expression)
                recommendation.append(binding.getVariable("result"))
                if(i != (noOfRules - 1))
                {
                    recommendation.append(',')
                }

            }

        }
        return recommendation

    }

    static def getIndividualVerdict(def browserList,def key,def aggregation)
    {
        def value
        def uaList = []

        if(aggregation == 'min')
        {
            value =  browserList."$key".min()
        }
        else
        {
            value =  browserList."$key".max()
        }

        def indexValues = browserList.findIndexValues{it."$key" == value}

        def size = indexValues.size()
        for(int i = 0 ;  i < size; i++)
        {
            uaList.add(browserList[indexValues[i].intValue()].userAgent)
        }

        return uaList

    }

    static def getCBTVerdict(def browserList,def navType)
    {

        def verdictMap = [:]
        def indvVerdict = []
        def finalVerdict = []

        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'totalPageLoadTime','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('totalPageLoadTime',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'clientTime','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('clientTime',indvVerdict.clone())
        indvVerdict.clear()
        if(navType != 'Soft')
        {
            indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'pageRenderingTime','min')
            finalVerdict.addAll(indvVerdict.clone())
            verdictMap.put('pageRenderingTime',indvVerdict.clone())
            indvVerdict.clear()
            indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'pagenetworkTime','min')
            finalVerdict.addAll(indvVerdict.clone())
            verdictMap.put('pagenetworkTime',indvVerdict.clone())
            indvVerdict.clear()
            indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'pageDomProcessingTime','min')
            finalVerdict.addAll(indvVerdict.clone())
            verdictMap.put('pageDomProcessingTime',indvVerdict.clone())
            indvVerdict.clear()
            indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'speedIndex','min')
            finalVerdict.addAll(indvVerdict.clone())
            verdictMap.put('speedIndex',indvVerdict.clone())
            indvVerdict.clear()

        }

        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'resourceLoadTime','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('resourceLoadTime',indvVerdict.clone())
        indvVerdict.clear()

        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'firstResStartTime','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('firstResStartTime',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'lastResStartTime','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('lastResStartTime',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'aggResdnsLookupTime','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('aggResdnsLookupTime',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'aggRestcpConnectTime','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('aggRestcpConnectTime',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'pcntResourcesCached','max')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('pcntResourcesCached',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'pcntResourcesCacheValidated','max')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('pcntResourcesCacheValidated',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'pcntResourcesCompressed','max')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('pcntResourcesCompressed',indvVerdict.clone())
        indvVerdict.clear()
        indvVerdict = PaceRuleEngine.getIndividualVerdict(browserList,'resrcCount','min')
        finalVerdict.addAll(indvVerdict.clone())
        verdictMap.put('resrcCount',indvVerdict.clone())
        def maxValue = 0
        def maxKey
        def maxKeyArray = []
        finalVerdict.groupBy().each{key,value ->
            if(value.size() > maxValue)
            {
                maxValue = value.size()
                maxKey = key
            }
        }
        maxKeyArray.add(maxKey)
        verdictMap.each{k,v ->

            if(v.size() > 1 && v.find{it==maxKey} == maxKey)
            {
                verdictMap.put(k,maxKeyArray)
            }
        }
        verdictMap.put('finalVerdict',maxKey)
        return verdictMap
    }

    static def getResourcesPercentageForCBT (def resList, def configReader)
    {
        def staticResrcCount = 0
        def compressibleResrcCount = 0
        def noncompressedResrcCount = 0
        def nonCachedResrcCount = 0
        def nonCacheValidatorResrcCount = 0


        for (int i=0; i< resList.size();i++)
        {
            if (CommonUtils.stringContainsItemFromList(resList.name,configReader.isStaticResources))
            {
                //Static resource count
                staticResrcCount = staticResrcCount + 1

                //Non Compressed Resource Count
                if(CommonUtils.stringContainsItemFromList(resList[i].name,configReader.isCompressibleResources) && resList[i]?.Status == 200)
                {
                    compressibleResrcCount = compressibleResrcCount + 1
                    if(resList[i].containsKey("Content-Encoding") && CommonUtils.stringNotContainsItemFromList(resList[i]?."Content-Encoding",'gzip,deflate,sdcn'))
                    {
                        noncompressedResrcCount = noncompressedResrcCount + 1
                    }

                }

                //Find non cached resoures
                if(resList[i].containsKey("Cache-Control") && resList[i].containsKey("Expires"))
                {
                    if(resList[i]."Cache-Control" == "" && resList[i]."Expires" == "")
                    {
                        nonCachedResrcCount = nonCachedResrcCount + 1
                    }
                    else
                    {
                        if (resList[i]."Expires" == "" && CommonUtils.stringNotContainsItemFromList(resList[i]?."Cache-Control",'no-store,public,private,no-cache,max-age,s-maxage,max-stale,min-fresh'))
                        {
                            nonCachedResrcCount = nonCachedResrcCount + 1
                        }
                        else
                        {
                            //Static Resources with cache and without cache validator
                            if(resList[i].containsKey("Last-Modified") && resList[i].containsKey("ETag") && resList[i]."Last-Modified" == "" && resList[i]."ETag" == "")
                            {
                                nonCacheValidatorResrcCount = nonCacheValidatorResrcCount + 1
                            }

                        }
                    }
                }
            }
        }

        def pcntMap =[:]


        pcntMap.put('pcntCached', Double.parseDouble((((staticResrcCount - nonCachedResrcCount) / (staticResrcCount == 0 ? 1 : staticResrcCount)) * 100).toString()).round())
        pcntMap.put('pcntCacheValidator',Double.parseDouble((((((staticResrcCount - nonCachedResrcCount) - nonCacheValidatorResrcCount) < 0 ? 0 : ((staticResrcCount - nonCachedResrcCount) - nonCacheValidatorResrcCount))/((staticResrcCount - nonCachedResrcCount) <= 0 ? 1 : (staticResrcCount - nonCachedResrcCount))) * 100).toString()).round())
        pcntMap.put('pcntCompressed',Double.parseDouble((((compressibleResrcCount - noncompressedResrcCount)/(compressibleResrcCount == 0 ? 1 : compressibleResrcCount)) * 100).toString()).round())
        return pcntMap
    }

    // Validate Native application sample against defined rules

    static def applyNativeAppRules(def sampleData,def configReader)
    {
        LOGGER.debug 'START applyNativeAppRules'
        LOGGER.debug 'Sample Data for Analysis : ' + sampleData

        float ruleScore = 0.00f
        float indvRuleScore = 0.00f
        StringBuilder recommendation = new StringBuilder()

        //Check if any rule needs to be excluded from analysis - customer setting specific to project
        List<String> excludeRuleArray = Arrays.asList(configReader?.excludedNativeAppRules?.split("\\s*,\\s*") == null ? '' : configReader?.excludedNativeAppRules?.split("\\s*,\\s*"))

        //Read Threshold from configuration or assign default values
        long resourceRTThreshold = (configReader?.resourceRTThreshold == null ? 500 : configReader?.resourceRTThreshold)
        long totalPayloadThreshold = (configReader?.totalPayloadThreshold == null ? 102400 : configReader?.totalPayloadThreshold)
        long resourcePayloadThreshold = (configReader?.resourcePayloadThreshold == null ? 10240 : configReader?.resourcePayloadThreshold)
        long httpCallsThreshold = (configReader?.httpCallsThreshold == null ? 10 : configReader?.httpCallsThreshold)


        recommendation.append('###')

        def rulesWeight = PaceRuleEngine.getNativeAppRules("Y",configReader)

        //(1 - Make fewer HTTP requests )
        if('1' == excludeRuleArray.find{ it == '1'})
        {
            ruleScore = ruleScore + rulesWeight.get('1')
        }
        else
        {
            if(sampleData.totalRequest > httpCallsThreshold)
            {
                recommendation.append(',{"id" :1,"comment" :"Total number of HTTP requests ')
                recommendation.append(sampleData.totalRequest)
                recommendation.append(' higher than configured threshold ').append(httpCallsThreshold)
                indvRuleScore = (1 - (((sampleData.totalRequest - 10) * 0.1) > 1 ? 1 : ((sampleData.totalRequest - 10)  * 0.1)))
                ruleScore = ruleScore + (rulesWeight.get('1') * indvRuleScore)
                recommendation.append('.","value" : 0')
                recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

            }
            else
            {
                ruleScore = ruleScore + rulesWeight.get('1')
            }
        }

        //(2 - Minimize Payload)
        if('2' == excludeRuleArray.find{ it == '2'})
        {
            ruleScore = ruleScore + rulesWeight.get('2')
        }
        else
        {
            if(sampleData.totalRequest > totalPayloadThreshold)
            {
                recommendation.append(',{"id" :2,"comment" :"Total transaction payload size ')
                recommendation.append(sampleData.totalSize)
                recommendation.append(' is higher than configured threshold ').append(totalPayloadThreshold).append(' bytes.')
                indvRuleScore = (1 - (((sampleData.totalRequest - 10) * 0.1) > 1 ? 1 : ((sampleData.totalRequest - 10)  * 0.1)))
                ruleScore = ruleScore + (rulesWeight.get('2') * indvRuleScore)
                recommendation.append('","value" : 0')
                recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

            }
            else
            {
                ruleScore = ruleScore + rulesWeight.get('2')
            }
        }


        if(sampleData.Resources?.size > 0)
        {
            def resourcesUrl = []
            def nonRTResrc = []
            def nonSizeResrc = []
            def badResponse = []
            for(int i=0;i< sampleData.Resources.size;i++)
            {
                if(sampleData.Resources[i].Method == 'GET')
                {
                    resourcesUrl.add(sampleData.Resources[i].name)
                }
                if(sampleData.Resources[i].totalResourceTime > resourceRTThreshold)
                {
                    nonRTResrc.add(sampleData.Resources[i].name + ' - ' + sampleData.Resources[i].totalResourceTime + ' ms.')
                }

                if(sampleData.Resources[i]."Content-Length" > resourcePayloadThreshold)
                {
                    nonSizeResrc.add(sampleData.Resources[i].name + ' - ' + sampleData.Resources[i]."Content-Length" + ' bytes.')
                }
                if(sampleData.Resources[i].Status != 200)
                {
                    badResponse.add(sampleData.Resources[i].name + ' - ' + sampleData.Resources[i].Status)
                }
            }


            //(3 - Avoid Duplicate Resources)
            if('3' == excludeRuleArray.find{ it == '3'})
            {
                ruleScore = ruleScore + rulesWeight.get('3')
            }
            else
            {
                def duplicateResources = resourcesUrl.findAll{resourcesUrl.count(it)>1}.unique()
                if (duplicateResources.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('3')
                }
                else
                {
                    recommendation.append(',{"id" :3,"comment" :"Following resources are called redundantly:<ul>')
                    int dupCnt = 0
                    duplicateResources.each {
                        recommendation.append('<li>').append(it).append('</li>')
                        dupCnt = dupCnt + resourcesUrl.count(it)
                    }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" :').append((dupCnt - duplicateResources.size()))
                    indvRuleScore = (1 - ((duplicateResources.size() * 0.2) > 1 ? 1 : (duplicateResources.size() * 0.2)))
                    ruleScore = ruleScore + (rulesWeight.get('3') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }

            //(4 - Minimize Resource Payload)
            if('4' == excludeRuleArray.find{ it == '4'})
            {
                ruleScore = ruleScore + rulesWeight.get('4')
            }
            else
            {
                if (nonSizeResrc.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('4')
                }
                else
                {
                    recommendation.append(',{"id" :4,"comment" :"Following resources having size greater than configured threshold of ').append(resourcePayloadThreshold).append(' bytes:<ul>')
                    nonSizeResrc.each { recommendation.append('<li>').append(it).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" :0')
                    indvRuleScore = (1 - ((nonSizeResrc.size() * 0.1) > 1 ? 1 : (nonSizeResrc.size() * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('4') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }


            //(5 - Avoid Bad requests Priority:High Difficulty:Easy Source:PageSpeed)
            if('5' == excludeRuleArray.find{ it == '5'})
            {
                ruleScore = ruleScore + rulesWeight.get('5')
            }
            else
            {
                if (badResponse.size <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('5')
                }
                else
                {
                    indvRuleScore = (1 - ((badResponse.size * 0.1) > 1 ? 1 : (badResponse.size * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('5') * indvRuleScore)
                    recommendation.append(',{"id" :5,"comment" :"Following resources are not found which are wasteful, unnecessary requests that lead to a bad user experience :<ul>')
                    badResponse.each { recommendation.append('<li>').append(it).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
            }

            //(6 - Resource RTT)
            if('6' == excludeRuleArray.find{ it == '6'})
            {
                ruleScore = ruleScore + rulesWeight.get('6')
            }
            else
            {
                if (nonRTResrc.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('6')
                }
                else
                {
                    recommendation.append(',{"id" :6,"comment" :"Following resources having roundtrip greater than threshold ').append(resourceRTThreshold).append(' ms:<ul>')
                    nonRTResrc.each { recommendation.append('<li>').append(it).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" :0')
                    indvRuleScore = (1 - ((nonRTResrc.size() * 0.1) > 1 ? 1 : (nonRTResrc.size() * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('6') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }



        }
        else
        {
            ruleScore = ruleScore + (rulesWeight.get('3') + rulesWeight.get('4') + rulesWeight.get('6') + rulesWeight.get('6'))
        }

        recommendation.replace(0,4,'')
        LOGGER.debug 'Transaction : ' +  sampleData.TransactionName + ' Recommendation : ' + recommendation
        LOGGER.debug 'END applyNativeAppRules'
        def score = (ruleScore * 100.00).round()
        return [score,recommendation]
    }

    //Web application general rules

    static def applyGenericRules(def sampleData,def configReader)
    {
        //Condition to handle analysis if DOM is empty
        if (sampleData.dom == null || sampleData.dom == '')
        {
            def result = PaceRuleEngine.applySoftNavigationRules(sampleData,configReader)
            return[result[0],result[1]]
        }
        else
        {

            LOGGER.debug 'START applyGenericRules'
            LOGGER.debug 'Sample Data for Analysis : ' + sampleData
            float ruleScore = 0.00f
            float indvRuleScore = 0.00f
            List<String> excludeRuleArray = Arrays.asList(configReader?.excludedGeneriRules?.split("\\s*,\\s*") == null ? '' : configReader?.excludedGeneriRules?.split("\\s*,\\s*"))
            StringBuilder recommendation = new StringBuilder()
            recommendation.append('###')

            //Get weight for each rules
            def rulesWeight = PaceRuleEngine.getGeneralRules("Y",configReader)


            //(1 - Avoid CSS @import Priority:Medium Difficulty:Easy Source:Page Speed)

            if('1' == excludeRuleArray.find{ it == '1'})
            {
                ruleScore = ruleScore + rulesWeight.get('1')
            }
            else
            {
                int importCnt = (sampleData.dom.toString() =~ /@import/).count + (sampleData.dom.toString() =~ /@IMPORT/).count
                if (importCnt > 0 )
                {
                    indvRuleScore = (1 - ((0.1 * importCnt) > 1 ? 1 : (0.1 * importCnt)))
                    ruleScore = ruleScore + (rulesWeight.get('1') * indvRuleScore)
                    recommendation.append(',{"id" :1,"comment" :"')
                    recommendation.append(CommonUtils.getInstanceCount(importCnt))
                    recommendation.append(' of @IMPORT used.Instead of @IMPORT, use a LINK tag for each stylesheet. This allows the browser to download stylesheets in parallel."')
                    recommendation.append(',"value" : ').append(importCnt)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
                else
                {
                    ruleScore = ruleScore + rulesWeight.get('1')
                }
            }

            //(2 - Minify HTML and Internal JS and CSS Priority:Medium Difficulty:Easy Source:Page Speed)

            //Get HTML document payload
            double htmlSize = sampleData.dom.toString().size()
            def minifyPcnt

            if('2' == excludeRuleArray.find{ it == '2'})
            {
                ruleScore = ruleScore + rulesWeight.get('2')
            }
            else
            {
                if (htmlSize > 14336)  //14336 is used since 14 KB is sent in the first RT via 10 TCP packets
                {
                    def compressor =  new HtmlCompressor()
                    StringBuilder cmpString = new StringBuilder()
                    try
                    {
                        compressor.setCompressCss(true);
                        compressor.setCompressJavaScript(true);
                        cmpString.append(compressor.compress(sampleData.dom.toString()))
                    }
                    catch(Exception e)
                    {
                        cmpString.setLength(0)
                        compressor.setCompressCss(false)
                        compressor.setCompressJavaScript(false)
                        cmpString.append(compressor.compress(sampleData.dom.toString()))
                    }

                    def minHTML = (htmlSize - cmpString.toString().size())
                    minifyPcnt = (minHTML/htmlSize)
                    indvRuleScore = (1 - minifyPcnt)
                    ruleScore = ruleScore + (rulesWeight.get('2') * indvRuleScore)
                    recommendation.append(',{"id" :2,"comment" :"')
                    recommendation.append('Reducing HTML document size less 14 KB will download entire dom in single round trip.Payload of current document can be reduced by ').append((minHTML/1024).toDouble().round()).append(' KB."')
                    recommendation.append(',"value" : ').append((minHTML/1024).toDouble().round())
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
                else
                {
                    ruleScore = ruleScore + rulesWeight.get('2')
                }
            }



            Document html = null
            Document body = null
            Document head = null

            //Get count of Javascripts which uses RegisterSod - Sharepoint lazy loading
            int totSODCount = (sampleData.dom.toString().toLowerCase() =~ />registersod/).count
            int headSODCount = 0

            try
            {
                html = Jsoup.parse(sampleData.dom.toString());
                body = Jsoup.parse(html.getElementsByTag("body").toString())
                head = Jsoup.parse(html.getElementsByTag("head").toString())
                headSODCount = (html.getElementsByTag("head").toString().toLowerCase() =~ />registersod/).count
            }
            catch(Exception e)
            {
                LOGGER.error("Parsing error to seggregate HTML document")
            }

            if( html != null && body != null && head != null)
            {

                //(3 - Minimize iFrames Usage Priority:Medium Difficulty:Moderate Source:PACE Best Practice)
                if('3' == excludeRuleArray.find{ it == '3'})
                {
                    ruleScore = ruleScore + rulesWeight.get('3')
                }
                else
                {
                    int emptyiFrameCnt =  html.select("IFRAME").size()
                    if (emptyiFrameCnt > 0 )
                    {

                        indvRuleScore = (1 - ((0.05 * emptyiFrameCnt) > 1 ? 1 : (0.05 * emptyiFrameCnt)))
                        ruleScore = ruleScore + (rulesWeight.get('3') * indvRuleScore)
                        recommendation.append(',{"id" :3,"comment" :"')
                        recommendation.append(CommonUtils.getInstanceCount(emptyiFrameCnt))
                        recommendation.append(' of IFRAME').append(CommonUtils.getPlural(emptyiFrameCnt))
                        recommendation.append(' used.If contents not important than the main page, set these IFRAME').append(CommonUtils.getPlural(emptyiFrameCnt)).append(' SRC dynamically after higher priority resources are done downloading."')
                        recommendation.append(',"value" : ').append(emptyiFrameCnt)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('3')
                    }
                }


                //(4 - Avoid empty SRC or HREF Priority:High Difficulty:Easy Source:YSLOW)
                //(5 - Specify image dimensions Priority:High Difficulty:Easy Source:PageSpeed)
                //(6 - Do not scale images in HTML Priority:Medium Difficulty:Easy Source:YSLOW)

                def emptyLinkCount = 0
                def noscaleCount = 0
                def noScaleImgs =[]
                def scaleImgs =[]

                def totImgCount = html.select("img").size()


                for (int i = 0; i < totImgCount; i++)
                {
                    if(html.select("img")[i].attr("src") == '')
                    {
                        emptyLinkCount = emptyLinkCount + 1
                    }

                    if(html.select("img")[i].attr("width") == '' && html.select("img")[i].attr("height") == '' && html.select("img")[i].attr("style") == '' && html.select("img")[i].attr("src") != '')
                    {
                        noscaleCount = noscaleCount + 1
                        noScaleImgs.add(html.select("img")[i].attr("src"))
                    }
                    else
                    {
                        scaleImgs.add(html.select("img")[i].attr("src"))
                    }
                }

                for (int i = 0; i < html.select("script[src]").size(); i++)
                {
                    if(html.select("script[src]")[i].attr("src") == '')
                    {
                        emptyLinkCount = emptyLinkCount + 1
                    }
                }

                for (int i = 0; i < html.select("link[href]").size(); i++)
                {
                    if( html.select("link[href]")[i].attr("href") == '')
                    {
                        emptyLinkCount = emptyLinkCount + 1
                    }
                }

                if('4' == excludeRuleArray.find{ it == '4'})
                {
                    ruleScore = ruleScore + rulesWeight.get('4')
                }
                else
                {
                    if (emptyLinkCount > 0)
                    {
                        recommendation.append(',{"id" :4,"comment" :"')
                        recommendation.append(CommonUtils.getInstanceCount(emptyLinkCount))
                        recommendation.append(' of empty SRC or HREF used in IMG,SCRIPT or LINK tag.Remove to avoid unnecessary HTTP call to server."')
                        recommendation.append(',"value" : ').append(emptyLinkCount)
                        recommendation.append(',"score" : 0}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('4')
                    }
                }


                if('5' == excludeRuleArray.find{ it == '5'})
                {
                    ruleScore = ruleScore + rulesWeight.get('5')
                }
                else
                {
                    if (noscaleCount > 0)
                    {
                        indvRuleScore = (1 - ((0.05 * noscaleCount) > 1 ? 1 : (0.05 * noscaleCount)))
                        ruleScore = ruleScore + (rulesWeight.get('5') * indvRuleScore)

                        recommendation.append(',{"id" :5,"comment" :"')
                        recommendation.append(CommonUtils.getInstanceCount(noscaleCount))
                        recommendation.append(' of IMG has no WIDTH or HEIGHT or STYLE defined.Be sure to specify dimensions on the image element or block-level parent to avoid browser reflow or repaint.<ul>')
                        noScaleImgs.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','').replaceAll("\\s","")).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : ').append(noscaleCount)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('5')
                    }
                }


                if('6' == excludeRuleArray.find{ it == '6'})
                {
                    ruleScore = ruleScore + rulesWeight.get('6')
                }
                else
                {
                    if ((totImgCount - noscaleCount) > 0)
                    {
                        indvRuleScore = (1 - ((0.01 * (totImgCount - noscaleCount)) > 1 ? 1 : (0.01 * (totImgCount - noscaleCount))))
                        ruleScore = ruleScore + (rulesWeight.get('6') * indvRuleScore)

                        recommendation.append(',{"id" :6,"comment" :"')
                        recommendation.append(totImgCount - noscaleCount)
                        recommendation.append(' IMG tag').append(CommonUtils.getPlural(totImgCount - noscaleCount)).append(' has scaling defined.Make sure right size image used and avoid scaling in HTML.<ul>')
                        scaleImgs.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : ').append((totImgCount - noscaleCount))
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('6')
                    }
                }


                //(7 - Remove unnecessary comments Priority:Low Difficulty:Easy Source:PACE Best Practice)
                if('7' == excludeRuleArray.find{ it == '7'})
                {
                    ruleScore = ruleScore + rulesWeight.get('7')
                }
                else
                {
                    int totComments = 0
                    int commentSize = 0
                    double cmtPercnt
                    try
                    {
                        Node node = html
                        for (int i = 0; i < node.childNodes().size(); i++)
                        {
                            Node child = node.childNode(i);
                            if (child.nodeName().equals("#comment"))
                            {
                                totComments = totComments + 1
                                commentSize = commentSize + child.getData().size()
                            }
                        }
                    }
                    catch(Exception e)
                    {
                    }

                    if (totComments > 0 )
                    {

                        cmtPercnt =  (commentSize/(htmlSize)).round(2)
                        indvRuleScore = ((1 - cmtPercnt) > 1 ? 1 : (1 - cmtPercnt))
                        ruleScore = ruleScore + (rulesWeight.get('7') * indvRuleScore)
                        recommendation.append(',{"id" :7,"comment" :"')
                        recommendation.append(CommonUtils.getInstanceCount(totComments))
                        recommendation.append(' of comment').append(CommonUtils.getPlural(totComments)).append(' in the HTML document.Remove unneccesory comments to reduce document size."')
                        recommendation.append(',"value" : ').append((commentSize/1024).toDouble().round())
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('7')
                    }
                }


                //(8 - Avoid a character set in the meta tag Priority:High Difficulty:Easy Source:PageSpeed)
                if('8' == excludeRuleArray.find{ it == '8'})
                {
                    ruleScore = ruleScore + rulesWeight.get('8')
                }
                else
                {
                    if(head.select("meta").attr("content").contains("charset"))
                    {
                        recommendation.append(',{"id" :8,"comment" :"')
                        recommendation.append('Specifying a character set in a meta tag disables the lookahead downloader in IE8.To improve resource download parallelization move the character set to the HTTP ContentType response header."')
                        recommendation.append(',"value" : 0')
                        recommendation.append(',"score" : 0}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('8')
                    }
                }


                //(9 - Make CSS external Priority:High Difficulty:Easy Source:YSLOW)
                if('9' == excludeRuleArray.find{ it == '9'})
                {
                    ruleScore = ruleScore + rulesWeight.get('9')
                }
                else
                {
                    def intCSSCount = html.select("style").size()
                    if (intCSSCount > 0 )
                    {

                        indvRuleScore = (1 - ((0.1 * intCSSCount) > 1 ? 1 : (0.1 * intCSSCount)))
                        ruleScore = ruleScore + (rulesWeight.get('9') * indvRuleScore)
                        recommendation.append(',{"id" :9,"comment" :"')
                        recommendation.append(CommonUtils.getInstanceCount(intCSSCount))
                        recommendation.append(' of internal Stylesheet').append(CommonUtils.getPlural(intCSSCount)).append(' ').append(CommonUtils.getisorare(intCSSCount)).append(' used.')
                        if(intCSSCount > 1)
                        {
                            recommendation.append('Combine all internal Stylesheets into single external file."')
                        }
                        else
                        {
                            recommendation.append('Move internal stylesheet to external if stylesheet not simple."')
                        }
                        recommendation.append(',"value" : ').append(intCSSCount)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('9')
                    }
                }


                //(10 - Put CSS at the top Priority:High Difficulty:Easy Source:YSLOW)
                if('10' == excludeRuleArray.find{ it == '10'})
                {
                    ruleScore = ruleScore + rulesWeight.get('10')
                }
                else
                {
                    def cssBodyCount = body.select("style").size()
                    if (cssBodyCount > 0 )
                    {
                        indvRuleScore = (1 - ((0.1 * cssBodyCount) > 1 ? 1 : (0.1 * cssBodyCount)))
                        ruleScore = ruleScore + (rulesWeight.get('10') * indvRuleScore)
                        recommendation.append(',{"id" :10,"comment" :"')
                        recommendation.append(CommonUtils.getInstanceCount(cssBodyCount))
                        recommendation.append(' of stylesheet').append(CommonUtils.getPlural(cssBodyCount)).append(' in BODY.Specifying external stylesheet').append(/ and inline style blocks in the body of an HTML document can negatively affect the browser's rendering performance."/)
                        recommendation.append(',"value" : ').append(cssBodyCount)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('10')
                    }
                }


                //(11 - Make JS external Priority:High Difficulty:Easy Source:YSLOW)
                if('11' == excludeRuleArray.find{ it == '11'})
                {
                    ruleScore = ruleScore + rulesWeight.get('11')
                }
                else
                {
                    def intJSCount = html.select("script").size() - (html.select("script[src]").size() + totSODCount)
                    if (intJSCount > 0 )
                    {
                        indvRuleScore = (1 - ((0.05 * intJSCount) > 1 ? 1 : (0.05 * intJSCount)))
                        ruleScore = ruleScore + (rulesWeight.get('11') * indvRuleScore)
                        recommendation.append(',{"id" :11,"comment" :"')
                        recommendation.append(CommonUtils.getInstanceCount(intJSCount))
                        recommendation.append(' of internal javascript').append(CommonUtils.getPlural(intJSCount)).append(' ').append(CommonUtils.getisorare(intJSCount)).append(' used.')
                        if(intJSCount > 1)
                        {
                            recommendation.append('Combine all internal Javascripts into single external file."')
                        }
                        else
                        {
                            recommendation.append('Move internal javascript to external if javascript not simple."')
                        }
                        recommendation.append(',"value" : ').append(intJSCount)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('11')
                    }
                }


                //(12 - Put JavaScript at bottom Priority:High Difficulty:Moderate Source:YSLOW)
                if('12' == excludeRuleArray.find{ it == '12'})
                {
                    ruleScore = ruleScore + rulesWeight.get('12')
                }
                else
                {
                    int jscntHead = (head.select("script").size() - (head.select("script[async]").size() + head.select("script[defer]").size() + headSODCount))

                    if (jscntHead > 0 )
                    {
                        def jsList = []
                        head.select("script:not(script[async],script[defer])").each { it ->
                            if(it.attr("src") != "")
                            {
                                jsList.add(it.attr("src"))
                            }

                        }
                        indvRuleScore = (1 - ((0.05 * jscntHead) > 1 ? 1 : (0.05 * jscntHead)))
                        ruleScore = ruleScore + (rulesWeight.get('12') * indvRuleScore)
                        recommendation.append(',{"id" :12,"comment" :"')
                        recommendation.append(jscntHead)
                        recommendation.append(' javascript').append(CommonUtils.getPlural(jscntHead)).append(' ').append(CommonUtils.getisorare(jscntHead)).append(' called in HEAD without ASYNC or DEFER attribute can block parallel download of resources.<ul>')
                        jsList.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : ').append(jscntHead)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('12')
                    }
                }

                //(13 - Prefer asynchronous resources Priority:High Difficulty:Moderate Source:Page Speed)
                if('13' == excludeRuleArray.find{ it == '13'})
                {
                    ruleScore = ruleScore + rulesWeight.get('13')
                }
                else
                {
                    int asyncJScnt = html.select("script[async]").size()
                    if (asyncJScnt > 0 )
                    {
                        def jsList = []
                        html.select("script[async]").each {
                            if(it.attr("src") != "")
                            {
                                jsList.add(it.attr("src"))
                            }
                        }
                        indvRuleScore = (1 - ((0.02 * asyncJScnt) > 1 ? 1 : (0.02 * asyncJScnt)))
                        ruleScore = ruleScore + (rulesWeight.get('13') * indvRuleScore)
                        recommendation.append(',{"id" :13,"comment" :"')
                        recommendation.append(asyncJScnt)
                        recommendation.append(' javascript').append(CommonUtils.getPlural(asyncJScnt)).append(' uses ASYNC attribute in HTML script tag.Use script DOM element to define ASYNC javascript to work across all browsers.<ul>')
                        jsList.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : ').append(asyncJScnt)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('13')
                    }
                }
            }


            //(14 - Reduce the number of DOM elements Priority:Low Difficulty:Moderate Source:YSLOW)
            if('14' == excludeRuleArray.find{ it == '14'})
            {
                ruleScore = ruleScore + rulesWeight.get('14')
            }
            else
            {
                if (sampleData.domElementCount > configReader.domCountThreshold)
                {
                    recommendation.append(',{"id" :14,"comment" :"Total DOM Element count of ')
                    recommendation.append(sampleData.domElementCount)
                    recommendation.append(' greater than threhold ')
                    recommendation.append(configReader.domCountThreshold)
                    recommendation.append(' .Consider reducing DOM elements by removing unnecessary DIV and nested tables for layout purpose."')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - ((((sampleData.domElementCount - 900)/250) * 0.1) > 1 ? 1 : (((sampleData.domElementCount - 900)/250) * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('14') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
                else
                {
                    ruleScore = ruleScore + rulesWeight.get('14')
                }
            }


            double minifyPcntTotal = 0
            int minifyCnt = 0
            if(sampleData.Resources?.size > 0)
            {
                def redirectResources = []
                def nonCachedResources = []
                def noCacheValidator = []
                def resourceDomains = []
                def resourceCategory = []
                def resourceCategoryByDomains = []
                def resourcesUrl = []
                def minifyResources =[]
                def nonGzipResources =[]
                def noKeepAlive = []
                def noVary = []
                def badRequests = []
                double minifyPcntRcs
                def maxAge = []
                def splitStr

                for(int i=0;i< sampleData.Resources.size;i++)
                {
                    //To avoid 304 requests collected as part of ResourceTiming API getting into analysis only resource with greater than 5 ms are considered
                    if(sampleData.Resources[i].duration > (configReader?.resourceDurationThreshold == null ? 10 : configReader?.resourceDurationThreshold))
                    {


                        if (sampleData.Resources[i].name.startsWith('http')) {
                            //Get all domains for resources
                            resourceDomains.add(sampleData.Resources[i].HostName)

                            //Aggregate Resource category but exclude AJAX calls in the analysis since this can impact the consistency of the analysis
                            if (CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i].initiatorType, 'xmlhttprequest')) {
                                resourceCategory.add(sampleData.Resources[i].ResourceType.toString().toLowerCase())
                            }
                            //Aggregate resource to domain
                            resourceCategoryByDomains.add(sampleData.Resources[i].HostName + ',' + sampleData.Resources[i].ResourceType.toString().toLowerCase())

                            //Find resource which are redirected
                            if (sampleData.Resources[i].redirectTime > 0 || sampleData.Resources[i]?.Status == '302') {
                                redirectResources.add(sampleData.Resources[i].name)
                            }

                            //Aggregate all static resources
                            if (CommonUtils.stringContainsItemFromList(sampleData.Resources[i].name, configReader.isStaticResources))
                            {
                                //All static resources
                                resourcesUrl.add(sampleData.Resources[i].name)

                                //Find Resources with Status 404 or 410

                                if (CommonUtils.stringContainsItemFromList((sampleData.Resources[i].containsKey("Status") ? sampleData.Resources[i]."Status" : '200'), '404,410'))
                                {
                                    badRequests.add(sampleData.Resources[i].name)
                                }
                                else
                                {
                                    //Find resources without keep-alive connection
                                    if (CommonUtils.stringNotContainsItemFromList((sampleData.Resources[i].containsKey("Connection") ? sampleData.Resources[i]."Connection" : 'keep-alive'), 'keep-alive')) {
                                        noKeepAlive.add(sampleData.Resources[i].name)
                                    }

                                    //Static Resources without cache validator
                                    if (sampleData.Resources[i].containsKey("Last-Modified") && sampleData.Resources[i].containsKey("ETag") && sampleData.Resources[i]."Last-Modified" == "" && sampleData.Resources[i]."ETag" == "") {
                                        noCacheValidator.add(sampleData.Resources[i].name)
                                    }



                                    //Find non cached resoures
                                    if (sampleData.Resources[i].containsKey("Cache-Control") && sampleData.Resources[i].containsKey("Expires"))
                                    {
                                        if (sampleData.Resources[i]."Cache-Control" == "" && sampleData.Resources[i]."Expires" == "")
                                        {
                                            nonCachedResources.add(sampleData.Resources[i].name)
                                        }
                                        else
                                        {
                                            if (sampleData.Resources[i]."Expires" == "" && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Cache-Control", 'no-store,public,private,no-cache,max-age,s-maxage,max-stale,min-fresh')) {
                                                nonCachedResources.add(sampleData.Resources[i].name)
                                            }
                                            else
                                            {
                                                if(CommonUtils.stringContainsItemFromList(sampleData.Resources[i]?."Cache-Control", 'max-age,s-maxage'))
                                                {
                                                    if(CommonUtils.stringContainsItemFromList(sampleData.Resources[i]?."Cache-Control", 's-maxage'))
                                                    {
                                                        splitStr = ((sampleData.Resources[i]?."Cache-Control".toString().split('s-maxage#')[1]).split('#')[0])
                                                        if(splitStr.toInteger() <= 3600)
                                                        {
                                                            maxAge.add(sampleData.Resources[i]?.name)
                                                        }
                                                    }
                                                    else
                                                    {
                                                        splitStr = ((sampleData.Resources[i]?."Cache-Control".split('age#')[1]).split('#')[0])

                                                        if(splitStr.toInteger() <= 3600)
                                                        {
                                                            maxAge.add(sampleData.Resources[i]?.name)
                                                        }


                                                    }

                                                }
                                            }
                                        }

                                    }

                                    //Find non compressed static resources
                                    //Find minify candidates for CSS and JS

                                    if (CommonUtils.stringContainsItemFromList(sampleData.Resources[i].name, configReader.isCompressibleResources) && sampleData.Resources[i]?.Status == 200)
                                    {
                                        //Find resources without Vary header with Accept-Encoding
                                        if (sampleData.Resources[i].containsKey("Vary") && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Vary", 'accept-encoding')) {
                                            noVary.add(sampleData.Resources[i].name)
                                        }

                                        if (CommonUtils.stringContainsItemFromList(sampleData.Resources[i].name, '.svg')) {
                                            if (sampleData.Resources[i].containsKey("Content-Encoding") && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Content-Encoding", 'gzip,deflate,sdcn')) {
                                                nonGzipResources.add(sampleData.Resources[i].name.toString())
                                            }

                                        } else {
                                            if (sampleData.Resources[i]?.OrgSize > 0) {
                                                minifyPcntRcs = (((sampleData.Resources[i]?.OrgSize ? 0 : sampleData.Resources[i]?.OrgSize) - (sampleData.Resources[i]?.MinfSize ? 0 : sampleData.Resources[i]?.MinfSize)) / (sampleData.Resources[i]?.OrgSize ? 1 : sampleData.Resources[i]?.OrgSize))
                                                if (minifyPcntRcs >= 0.05) {
                                                    minifyPcntTotal = minifyPcntTotal + minifyPcntRcs
                                                    minifyCnt = minifyCnt + 1
                                                    minifyResources.add(sampleData.Resources[i].name.toString() + ' - ' + (minifyPcntRcs * 100) + ' %')
                                                }
                                            }

                                            if (sampleData.Resources[i].containsKey("Content-Encoding") && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Content-Encoding", 'gzip,deflate,sdcn')) {
                                                nonGzipResources.add(sampleData.Resources[i].name.toString())
                                            }
                                        }
                                    }

                                }

                            }
                        }
                    }

                }



                //(15 - Avoid Bad requests Priority:High Difficulty:Easy Source:PageSpeed)
                if('15' == excludeRuleArray.find{ it == '15'})
                {
                    ruleScore = ruleScore + rulesWeight.get('15')
                }
                else
                {
                    if (badRequests.size <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('15')
                    }
                    else
                    {
                        indvRuleScore = (1 - ((badRequests.size * 0.1) > 1 ? 1 : (badRequests.size * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('15') * indvRuleScore)
                        recommendation.append(',{"id" :15,"comment" :"Following resources are not found which are wasteful, unnecessary requests that lead to a bad user experience :<ul>')
                        badRequests.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                    }
                }



                //(16 - Make fewer HTTP requests Priority:High Difficulty:Moderate Source:YSLOW)
                if('16' == excludeRuleArray.find{ it == '16'})
                {
                    ruleScore = ruleScore + rulesWeight.get('16')
                }
                else
                {
                    if(resourceCategory.size() > 5)
                    {
                        recommendation.append(',{"id" :16,"comment" :"Total number of HTTP requests ')
                        recommendation.append(resourceCategory.size())
                        recommendation.append('.Resource Distribution : <ul>')
                        for (String key : resourceCategory.groupBy().keySet())
                        {
                            recommendation.append('<li>Total ').append(key).append(' : ').append(resourceCategory.groupBy()[key].size()).append('</li>')
                        }
                        recommendation.append('</ul>')
                        //recommendation.append('.Combine Stylesheets,Javascripts and use CSS Sprites and image maps.<ul>')

                        //06/06/16 - modified logic to adjust score going greater than 100
                        indvRuleScore = (1 - ((((resourceCategory.size() - 20) < 0 ? 0 : (resourceCategory.size() - 20))  * 0.1) > 1 ? 1 : (((resourceCategory.size() - 20) < 0 ? 0 : (resourceCategory.size() - 20))  * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('16') * indvRuleScore)

                        //(Combine external JavaScript Priority:High Difficulty:Moderate Source:YSLOW)
                        StringBuilder jsMerge = new StringBuilder()
                        jsMerge.append('<ul>Candidates for merging JS:')
                        boolean jsFlag = false
                        def jsReduction = 0
                        for (String key :  resourceCategoryByDomains.groupBy().keySet())
                        {
                            if(key.contains('.js') && resourceCategoryByDomains.groupBy()[key].size() > 1)
                            {
                                jsMerge.append('<li>Total JS called from ').append(key.split(',')[0]).append(' : ').append(resourceCategoryByDomains.groupBy()[key].size()).append('</li>')
                                jsReduction = jsReduction + ( resourceCategoryByDomains.groupBy()[key].size() - 1)
                                jsFlag = true
                            }
                        }
                        jsMerge.append('</ul>')

                        if(!jsFlag)
                        {
                        }
                        else
                        {
                            recommendation.append(jsMerge)
                        }

                        //(Combine external CSS Priority:High Difficulty:Moderate Source:YSLOW)
                        StringBuilder cssMerge = new StringBuilder()
                        cssMerge.append('<ul>Candidates for merging CSS:')
                        boolean cssFlag = false
                        def cssReduction = 0
                        for (String key :  resourceCategoryByDomains.groupBy().keySet())
                        {
                            if(key.contains('.css') && resourceCategoryByDomains.groupBy()[key].size() > 1)
                            {
                                cssMerge.append('<li>Total CSS called from ').append(key.split(',')[0]).append(' : ' +resourceCategoryByDomains.groupBy()[key].size()).append('</li>')
                                cssReduction = cssReduction + ( resourceCategoryByDomains.groupBy()[key].size() - 1)
                                cssFlag = true
                            }
                        }
                        cssMerge.append('</ul>')

                        if(!cssFlag)
                        {
                        }
                        else
                        {
                            recommendation.append(cssMerge)
                        }
                        recommendation.append('","value" : ').append((jsReduction + cssReduction))
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('16')
                    }
                }


                //(17 - Optimize images Priority:High Difficulty:Moderate Source:PageSpeed)
                if('17' == excludeRuleArray.find{ it == '17'})
                {
                    ruleScore = ruleScore + rulesWeight.get('17')
                }
                else
                {
                    def pngJpgCount = (resourceCategory.groupBy()['.png']?.size() == null ? 0 : resourceCategory.groupBy()['.png']?.size()) + (resourceCategory.groupBy()['.jpg']?.size() == null ? 0 : resourceCategory.groupBy()['.jpg']?.size()) + (resourceCategory.groupBy()['.jpeg']?.size() == null ? 0 : resourceCategory.groupBy()['.jpeg']?.size()) + (resourceCategory.groupBy()['.svg']?.size() == null ? 0 : resourceCategory.groupBy()['.svg']?.size())
                    if(pngJpgCount > 9)
                    {
                        recommendation.append(',{"id" :17,"comment" :"Consider Inline Images or CSS Sprite to avoid HTTP calls for images:<ul>')
                        recommendation.append('<li>pngcrush -rem alla -nofilecheck -reduce -brute original.png optimized.png</li>')
                        recommendation.append('<li>jpegtran -copy none -optimize -perfect -outfile new-image.jpg orig-image.jpg</li></ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - (((pngJpgCount - 9) * 0.11) > 1 ? 1 : ((pngJpgCount - 9) * 0.11)))
                        ruleScore = ruleScore + (rulesWeight.get('17') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('17')
                    }
                }


                //(18 -  Use PNG/JPG instead of GIF Priority:High Difficulty:Moderate Source:YSLOW)
                if('18' == excludeRuleArray.find{ it == '18'})
                {
                    ruleScore = ruleScore + rulesWeight.get('18')
                }
                else
                {
                    def gifCount = (resourceCategory.groupBy()['.gif']?.size() == null ? 0 : resourceCategory.groupBy()['.gif']?.size())
                    //if(resourceCategory.groupBy().keySet().contains('.gif') || resourceCategory.groupBy().keySet().contains('.GIF'))
                    if(gifCount > 0)
                    {
                        recommendation.append(',{"id" :18,"comment" :"Convert GIF to PNG or JPG using imagemagic if animation is not needed<ul>')
                        sampleData.Resources.name.each {
                            if(it.contains('.gif') || it.contains('.GIF'))
                            {
                                recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>')
                            }
                        }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - ((gifCount * 0.05) > 1 ? 1 : (gifCount * 0.05)))
                        ruleScore = ruleScore + (rulesWeight.get('18') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('18')
                    }
                }


                //(19 - Parallelize downloads across hostnames Priority:High Difficulty:Hard Source:Page Speed)
                if('19' == excludeRuleArray.find{ it == '19'})
                {
                    ruleScore = ruleScore + rulesWeight.get('19')
                }
                else
                {
                    //05/18/16 added condition for resources grater than 6 for parallel download
                    if(resourceDomains.groupBy().size() > 0  && resourceDomains.groupBy().size() < 5  && sampleData.Resources?.size > 6)
                    {
                        recommendation.append(',{"id" :19,"comment" :"All resources are downloaded from same domain or less than 4 domains.Consider Domain sharding for parallel download of resources (i.e static1.domain.com & static2.domain.com)."')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (resourceDomains.groupBy().size()/4)
                        ruleScore = ruleScore + (rulesWeight.get('19') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                    else
                    {
                        ruleScore = ruleScore + rulesWeight.get('19')
                    }
                }


                //(20 - Avoid Landing Page redirects Priority:High Difficulty:Hard Source:Page Speed)
                //(21 - Avoid URL redirects resources Priority:Medium Difficulty:Moderate Source:YSLOW)
                if('20' == excludeRuleArray.find{ it == '20'})
                {
                    ruleScore = ruleScore + rulesWeight.get('20')
                }
                else
                {
                    if (sampleData.redirectTime == 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('20')
                    }
                    else
                    {
                        recommendation.append(',{"id" :20,"comment" :"If your service requires redirects, perform the redirection server-side rather than client side, in order to reduce client-side round trip requests."')
                        recommendation.append(',"value" : 0')
                        recommendation.append(',"score" : 0}')
                    }
                }


                if('21' == excludeRuleArray.find{ it == '21'})
                {
                    ruleScore = ruleScore + rulesWeight.get('21')
                }
                else
                {
                    if (redirectResources.size == 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('21')
                    }
                    else
                    {
                        recommendation.append(',{"id" :21,"comment" :"Avoid redirect for resource').append(CommonUtils.getPlural(redirectResources.size)).append(':<ul>')
                        redirectResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : ').append(redirectResources.size())
                        indvRuleScore = (1 - ((redirectResources.size() * 0.05) > 1 ? 1 : (redirectResources.size() * 0.05)))
                        ruleScore = ruleScore + (rulesWeight.get('21') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }


                //(22 - Set Proper Caching Hearder Priority:High Difficulty:Easy Source:Page Speed)
                if('22' == excludeRuleArray.find{ it == '22'})
                {
                    ruleScore = ruleScore + rulesWeight.get('22')
                }
                else
                {
                    if (nonCachedResources.size <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('22')
                    }
                    else
                    {
                        indvRuleScore = (1 - ((nonCachedResources.size * 0.1) > 1 ? 1 : (nonCachedResources.size * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('22') * indvRuleScore)
                        recommendation.append(',{"id" :22,"comment" :"Make sure Expire or Cache-Control header set for static resources:<ul>')
                        nonCachedResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                    }
                }

                //(23 - Specify a cache validator :High Difficulty:Easy Source:Page Speed)
                if('23' == excludeRuleArray.find{ it == '23'})
                {
                    ruleScore = ruleScore + rulesWeight.get('23')
                }
                else
                {
                    if (noCacheValidator.size <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('23')
                    }
                    else
                    {
                        indvRuleScore = (1 - ((noCacheValidator.size * 0.1) > 1 ? 1 : (noCacheValidator.size * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('23') * indvRuleScore)
                        recommendation.append(',{"id" :23,"comment" :"Static resources with Last-Modified or ETag header will allow browsers to take advantage of the full benefits of caching.Below resources doesnt have the header set:<ul>')
                        noCacheValidator.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                    }
                }

                //(24 - Specify a Vary: Accept-Encoding header :High Difficulty:Easy Source:Page Speed)
                if('24' == excludeRuleArray.find{ it == '24'})
                {
                    ruleScore = ruleScore + rulesWeight.get('24')
                }
                else
                {
                    if (noVary.size <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('24')
                    }
                    else
                    {
                        indvRuleScore = (1 - ((noVary.size * 0.1) > 1 ? 1 : (noVary.size * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('24') * indvRuleScore)
                        recommendation.append(',{"id" :24,"comment" :"Following resources doest have Vary: Accept-Encoding header instructs the proxy to store both a compressed and uncompressed version of the resource :<ul>')
                        noVary.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                    }
                }


                //(25 - Enable Keep Alive Priority:High Difficulty:Easy Source:Page Speed)
                if('25' == excludeRuleArray.find{ it == '25'})
                {
                    ruleScore = ruleScore + rulesWeight.get('25')
                }
                else
                {
                    if (noKeepAlive.size <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('25')
                    }
                    else
                    {
                        recommendation.append(',{"id" :25,"comment" :"Set Connection: Keep-Alive for the following resources to avoid DNS lookup:<ul>')
                        noKeepAlive.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - (((noKeepAlive.size) * 0.11) > 1 ? 1 : ((noKeepAlive.size) * 0.11)))
                        ruleScore = ruleScore + (rulesWeight.get('25')* indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }

                def duplicateResources = resourcesUrl.findAll{resourcesUrl.count(it)>1}.unique()

                //(26 - Avoid Duplicate Resources Priority:High Difficulty:Easy Source:Page Speed)
                if('26' == excludeRuleArray.find{ it == '26'})
                {
                    ruleScore = ruleScore + rulesWeight.get('26')
                }
                else
                {
                    if (duplicateResources.size() <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('26')
                    }
                    else
                    {
                        recommendation.append(',{"id" :26,"comment" :"Following resources are called redundantly:<ul>')
                        duplicateResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - ((duplicateResources.size() * 0.2) > 1 ? 1 : (duplicateResources.size() * 0.2)))
                        ruleScore = ruleScore + (rulesWeight.get('26') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }

                def resourcesWithQS = resourcesUrl.findAll { it.contains('?')}

                //(27 - Avoid Resources with Query String Priority:High Difficulty:Easy Source:Page Speed)
                if('27' == excludeRuleArray.find{ it == '27'})
                {
                    ruleScore = ruleScore + rulesWeight.get('27')
                }
                else
                {
                    if (resourcesWithQS.size() <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('27')
                    }
                    else
                    {
                        recommendation.append(',{"id" :27,"comment" :"Resources with a ? or & in the URL are not cached by some proxy caching servers, and moving the query string and encode the parameters into the URL :<ul>')
                        resourcesWithQS.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - ((resourcesWithQS.size() * 0.1) > 1 ? 1 : (resourcesWithQS.size() * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('27') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }


                def flashExists = resourcesUrl.findAll { it.contains('.swf')}

                //(28 - Avoid Flash Priority:High Difficulty:Easy Source:Page Speed)
                if('28' == excludeRuleArray.find{ it == '28'})
                {
                    ruleScore = ruleScore + rulesWeight.get('28')
                }
                else
                {
                    if (flashExists.size() <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('28')
                    }
                    else
                    {
                        recommendation.append(',{"id" :28,"comment" :"Replace Flash with HTML5, CSS, and JavaScript:<ul>')
                        flashExists.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - ((flashExists.size() * 0.1) > 1 ? 1 : (flashExists.size() * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('28') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }

                //(29 - Compress Static files Priority:High Difficulty:Easy Source:Page Speed)
                if('29' == excludeRuleArray.find{ it == '29'})
                {
                    ruleScore = ruleScore + rulesWeight.get('29')
                }
                else
                {
                    if (nonGzipResources.size() <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('29')
                    }
                    else
                    {
                        recommendation.append(',{"id" :29,"comment" :"Following static resources were not compressed:<ul>')
                        nonGzipResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - ((nonGzipResources.size() * 0.1) > 1 ? 1 : (nonGzipResources.size() * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('29') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }

                //(30 - Minify CSS & JS Priority:High Difficulty:Easy Source:Page Speed)
                if('30' == excludeRuleArray.find{ it == '30'})
                {
                    ruleScore = ruleScore + rulesWeight.get('30')
                }
                else
                {
                    if (minifyResources.size() <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('30')
                    }
                    else
                    {
                        recommendation.append(',{"id" :30,"comment" :"Following static resources can be minified:<ul>')
                        minifyResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : ').append((minifyPcntTotal/minifyCnt) * 100)
                        indvRuleScore = (1 - ((minifyResources.size() * 0.1) > 1 ? 1 : (minifyResources.size() * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('30') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }

                //(31 - Max Age < 3600 seconds Priority:High Difficulty:Easy Source:Page Speed)
                if('31' == excludeRuleArray.find{ it == '31'})
                {
                    ruleScore = ruleScore + rulesWeight.get('31')
                }
                else
                {
                    if (maxAge.size() <= 0)
                    {
                        ruleScore = ruleScore + rulesWeight.get('31')
                    }
                    else
                    {
                        recommendation.append(',{"id" :31,"comment" :"Following static resources were having cache expiry less than hour:<ul>')
                        maxAge.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                        recommendation.append('</ul>"')
                        recommendation.append(',"value" : 0')
                        indvRuleScore = (1 - ((maxAge.size() * 0.1) > 1 ? 1 : (maxAge.size() * 0.1)))
                        ruleScore = ruleScore + (rulesWeight.get('31') * indvRuleScore)
                        recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                    }
                }



            }
            else
            {
                //Added 05/18/16 to tally the score for calls with 0 resources
                ruleScore = ruleScore + (rulesWeight.get('15') + rulesWeight.get('16') + rulesWeight.get('17') + rulesWeight.get('18') + rulesWeight.get('19') + rulesWeight.get('20') + rulesWeight.get('21') + rulesWeight.get('22') + rulesWeight.get('23') + rulesWeight.get('24') + rulesWeight.get('25') + rulesWeight.get('26') + rulesWeight.get('27') + rulesWeight.get('28') + rulesWeight.get('29') + rulesWeight.get('30') + + rulesWeight.get('31'))

            }

            recommendation.replace(0,4,'')
            LOGGER.debug 'Transaction : ' +  sampleData.TransactionName + ' Recommendation : ' + recommendation
            LOGGER.debug 'END applyGenericRules'
            def score = (ruleScore * 100.00).round()
            return [score,recommendation]
        }
    }

    static def applySoftNavigationRules(def sampleData,def configReader)
    {
        LOGGER.debug 'START applySoftNavigationRules'
        LOGGER.debug 'Sample Data for Analysis : ' + sampleData

        float ruleScore = 0.00f
        float indvRuleScore = 0.00f
        List<String> excludeRuleArray = Arrays.asList(configReader?.excludedSoftNavigationRules?.split("\\s*,\\s*") == null ? '' : configReader?.excludedSoftNavigationRules?.split("\\s*,\\s*"))
        StringBuilder recommendation = new StringBuilder()
        recommendation.append('###')

        def rulesWeight = PaceRuleEngine.getSoftNavigationRules("Y", configReader)

        double minifyPcntTotal = 0
        int minifyCnt = 0
        if(sampleData.Resources?.size > 0)
        {
            def redirectResources = []
            def nonCachedResources = []
            def noCacheValidator = []
            def resourceDomains = []
            def resourceCategory = []
            def resourceCategoryByDomains = []
            def resourcesUrl = []
            def minifyResources =[]
            def nonGzipResources =[]
            def noKeepAlive = []
            def noVary = []
            def badRequests = []
            double minifyPcntRcs
            def maxAge = []
            def splitStr
            def isStaticResources = configReader.staticResourceExtension + ',' + configReader.imageResourceExtension


            for(int i=0;i< sampleData.Resources.size;i++)
            {
                //To avoid 304 requests collected as part of ResourceTiming API getting into analysis only resource with greater than 5 ms are considered
                if(sampleData.Resources[i].duration > (configReader?.resourceDurationThreshold == null ? 10 : configReader?.resourceDurationThreshold))
                {


                    if (sampleData.Resources[i].name.startsWith('http')) {
                        //Get all domains for resources
                        resourceDomains.add(sampleData.Resources[i].HostName)

                        //Aggregate Resource category but exclude AJAX calls in the analysis since this can impact the consistency of the analysis
                        if (CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i].initiatorType, 'xmlhttprequest')) {
                            resourceCategory.add(sampleData.Resources[i].ResourceType.toString().toLowerCase())
                        }
                        //Aggregate resource to domain
                        resourceCategoryByDomains.add(sampleData.Resources[i].HostName + ',' + sampleData.Resources[i].ResourceType.toString().toLowerCase())

                        //Find resource which are redirected
                        if (sampleData.Resources[i].redirectTime > 0 || sampleData.Resources[i]?.Status == '302') {
                            redirectResources.add(sampleData.Resources[i].name)
                        }

                        //Aggregate all static resources
                        if (CommonUtils.stringContainsItemFromList(sampleData.Resources[i].name, configReader.isStaticResources))
                        {
                            //All static resources
                            resourcesUrl.add(sampleData.Resources[i].name)

                            //Find Resources with Status 404 or 410

                            if (CommonUtils.stringContainsItemFromList((sampleData.Resources[i].containsKey("Status") ? sampleData.Resources[i]."Status" : '200'), '404,410'))
                            {
                                badRequests.add(sampleData.Resources[i].name)
                            }
                            else
                            {
                                //Find resources without keep-alive connection
                                if (CommonUtils.stringNotContainsItemFromList((sampleData.Resources[i].containsKey("Connection") ? sampleData.Resources[i]."Connection" : 'keep-alive'), 'keep-alive')) {
                                    noKeepAlive.add(sampleData.Resources[i].name)
                                }

                                //Static Resources without cache validator
                                if (sampleData.Resources[i].containsKey("Last-Modified") && sampleData.Resources[i].containsKey("ETag") && sampleData.Resources[i]."Last-Modified" == "" && sampleData.Resources[i]."ETag" == "") {
                                    noCacheValidator.add(sampleData.Resources[i].name)
                                }



                                //Find non cached resoures
                                if (sampleData.Resources[i].containsKey("Cache-Control") && sampleData.Resources[i].containsKey("Expires"))
                                {
                                    if (sampleData.Resources[i]."Cache-Control" == "" && sampleData.Resources[i]."Expires" == "")
                                    {
                                        nonCachedResources.add(sampleData.Resources[i].name)
                                    }
                                    else
                                    {
                                        if (sampleData.Resources[i]."Expires" == "" && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Cache-Control", 'no-store,public,private,no-cache,max-age,s-maxage,max-stale,min-fresh')) {
                                            nonCachedResources.add(sampleData.Resources[i].name)
                                        }
                                        else
                                        {
                                            if(CommonUtils.stringContainsItemFromList(sampleData.Resources[i]?."Cache-Control", 'max-age,s-maxage'))
                                            {
                                                if(CommonUtils.stringContainsItemFromList(sampleData.Resources[i]?."Cache-Control", 's-maxage'))
                                                {
                                                    splitStr = ((sampleData.Resources[i]?."Cache-Control".toString().split('s-maxage#')[1]).split('#')[0])
                                                    if(splitStr.toInteger() <= 3600)
                                                    {
                                                        maxAge.add(sampleData.Resources[i]?.name)
                                                    }
                                                }
                                                else
                                                {
                                                    splitStr = ((sampleData.Resources[i]?."Cache-Control".split('age#')[1]).split('#')[0])

                                                    if(splitStr.toInteger() <= 3600)
                                                    {
                                                        maxAge.add(sampleData.Resources[i]?.name)
                                                    }


                                                }

                                            }
                                        }
                                    }

                                }

                                //Find non compressed static resources
                                //Find minify candidates for CSS and JS

                                if (CommonUtils.stringContainsItemFromList(sampleData.Resources[i].name, configReader.isCompressibleResources) && sampleData.Resources[i]?.Status == 200)
                                {
                                    //Find resources without Vary header with Accept-Encoding
                                    if (sampleData.Resources[i].containsKey("Vary") && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Vary", 'accept-encoding')) {
                                        noVary.add(sampleData.Resources[i].name)
                                    }

                                    if (CommonUtils.stringContainsItemFromList(sampleData.Resources[i].name, '.svg')) {
                                        if (sampleData.Resources[i].containsKey("Content-Encoding") && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Content-Encoding", 'gzip,deflate,sdcn')) {
                                            nonGzipResources.add(sampleData.Resources[i].name.toString())
                                        }

                                    } else {
                                        if (sampleData.Resources[i]?.OrgSize > 0) {
                                            minifyPcntRcs = (((sampleData.Resources[i]?.OrgSize ? 0 : sampleData.Resources[i]?.OrgSize) - (sampleData.Resources[i]?.MinfSize ? 0 : sampleData.Resources[i]?.MinfSize)) / (sampleData.Resources[i]?.OrgSize ? 1 : sampleData.Resources[i]?.OrgSize))
                                            if (minifyPcntRcs >= 0.05) {
                                                minifyPcntTotal = minifyPcntTotal + minifyPcntRcs
                                                minifyCnt = minifyCnt + 1
                                                minifyResources.add(sampleData.Resources[i].name.toString() + ' - ' + (minifyPcntRcs * 100) + ' %')
                                            }
                                        }

                                        if (sampleData.Resources[i].containsKey("Content-Encoding") && CommonUtils.stringNotContainsItemFromList(sampleData.Resources[i]?."Content-Encoding", 'gzip,deflate,sdcn')) {
                                            nonGzipResources.add(sampleData.Resources[i].name.toString())
                                        }
                                    }
                                }

                            }

                        }
                    }
                }

            }

            //(15 - Avoid Bad requests Priority:High Difficulty:Easy Source:PageSpeed)
            if('15' == excludeRuleArray.find{ it == '15'})
            {
                ruleScore = ruleScore + rulesWeight.get('15')
            }
            else
            {
                if (badRequests.size <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('15')
                }
                else
                {
                    indvRuleScore = (1 - ((badRequests.size * 0.1) > 1 ? 1 : (badRequests.size * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('15') * indvRuleScore)
                    recommendation.append(',{"id" :15,"comment" :"Following resources are not found which are wasteful, unnecessary requests that lead to a bad user experience :<ul>')
                    badRequests.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
            }



            //(16 - Make fewer HTTP requests Priority:High Difficulty:Moderate Source:YSLOW)
            if('16' == excludeRuleArray.find{ it == '16'})
            {
                ruleScore = ruleScore + rulesWeight.get('16')
            }
            else
            {
                if(resourceCategory.size() > 3)
                {
                    recommendation.append(',{"id" :16,"comment" :"Total number of HTTP requests ')
                    recommendation.append(resourceCategory.size())
                    recommendation.append('.Resource Distribution : <ul>')
                    for (String key : resourceCategory.groupBy().keySet())
                    {
                        recommendation.append('<li>Total ').append(key).append(' : ').append(resourceCategory.groupBy()[key].size()).append('</li>')
                    }
                    recommendation.append('</ul>')
                    //recommendation.append('.Combine Stylesheets,Javascripts and use CSS Sprites and image maps.<ul>')

                    //06/06/16 - modified logic to adjust score going greater than 100
                    indvRuleScore = (1 - ((((resourceCategory.size() - 20) < 0 ? 0 : (resourceCategory.size() - 20))  * 0.1) > 1 ? 1 : (((resourceCategory.size() - 20) < 0 ? 0 : (resourceCategory.size() - 20))  * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('16') * indvRuleScore)

                    //(Combine external JavaScript Priority:High Difficulty:Moderate Source:YSLOW)
                    StringBuilder jsMerge = new StringBuilder()
                    jsMerge.append('<ul>Candidates for merging JS:')
                    boolean jsFlag = false
                    def jsReduction = 0
                    for (String key :  resourceCategoryByDomains.groupBy().keySet())
                    {
                        if(key.contains('.js') && resourceCategoryByDomains.groupBy()[key].size() > 1)
                        {
                            jsMerge.append('<li>Total JS called from ').append(key.split(',')[0]).append(' : ').append(resourceCategoryByDomains.groupBy()[key].size()).append('</li>')
                            jsReduction = jsReduction + ( resourceCategoryByDomains.groupBy()[key].size() - 1)
                            jsFlag = true
                        }
                    }
                    jsMerge.append('</ul>')

                    if(!jsFlag)
                    {
                    }
                    else
                    {
                        recommendation.append(jsMerge)
                    }

                    //(Combine external CSS Priority:High Difficulty:Moderate Source:YSLOW)
                    StringBuilder cssMerge = new StringBuilder()
                    cssMerge.append('<ul>Candidates for merging CSS:')
                    boolean cssFlag = false
                    def cssReduction = 0
                    for (String key :  resourceCategoryByDomains.groupBy().keySet())
                    {
                        if(key.contains('.css') && resourceCategoryByDomains.groupBy()[key].size() > 1)
                        {
                            cssMerge.append('<li>Total CSS called from ').append(key.split(',')[0]).append(' : ' +resourceCategoryByDomains.groupBy()[key].size()).append('</li>')
                            cssReduction = cssReduction + ( resourceCategoryByDomains.groupBy()[key].size() - 1)
                            cssFlag = true
                        }
                    }
                    cssMerge.append('</ul>')

                    if(!cssFlag)
                    {
                    }
                    else
                    {
                        recommendation.append(cssMerge)
                    }
                    recommendation.append('","value" : ').append((jsReduction + cssReduction))
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
                else
                {
                    ruleScore = ruleScore + rulesWeight.get('16')
                }
            }


            //(17 - Optimize images Priority:High Difficulty:Moderate Source:PageSpeed)
            if('17' == excludeRuleArray.find{ it == '17'})
            {
                ruleScore = ruleScore + rulesWeight.get('17')
            }
            else
            {
                def pngJpgCount = (resourceCategory.groupBy()['.png']?.size() == null ? 0 : resourceCategory.groupBy()['.png']?.size()) + (resourceCategory.groupBy()['.jpg']?.size() == null ? 0 : resourceCategory.groupBy()['.jpg']?.size()) + (resourceCategory.groupBy()['.jpeg']?.size() == null ? 0 : resourceCategory.groupBy()['.jpeg']?.size()) + (resourceCategory.groupBy()['.svg']?.size() == null ? 0 : resourceCategory.groupBy()['.svg']?.size())
                if(pngJpgCount > 9)
                {
                    recommendation.append(',{"id" :17,"comment" :"Consider Inline Images or CSS Sprite to avoid HTTP calls for images:<ul>')
                    recommendation.append('<li>pngcrush -rem alla -nofilecheck -reduce -brute original.png optimized.png</li>')
                    recommendation.append('<li>jpegtran -copy none -optimize -perfect -outfile new-image.jpg orig-image.jpg</li></ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - (((pngJpgCount - 9) * 0.11) > 1 ? 1 : ((pngJpgCount - 9) * 0.11)))
                    ruleScore = ruleScore + (rulesWeight.get('17') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
                else
                {
                    ruleScore = ruleScore + rulesWeight.get('17')
                }
            }


            //(18 -  Use PNG/JPG instead of GIF Priority:High Difficulty:Moderate Source:YSLOW)
            if('18' == excludeRuleArray.find{ it == '18'})
            {
                ruleScore = ruleScore + rulesWeight.get('18')
            }
            else
            {
                def gifCount = (resourceCategory.groupBy()['.gif']?.size() == null ? 0 : resourceCategory.groupBy()['.gif']?.size())
                //if(resourceCategory.groupBy().keySet().contains('.gif') || resourceCategory.groupBy().keySet().contains('.GIF'))
                if(gifCount > 0)
                {
                    recommendation.append(',{"id" :18,"comment" :"Convert GIF to PNG or JPG using imagemagic if animation is not needed<ul>')
                    sampleData.Resources.name.each {
                        if(it.contains('.gif') || it.contains('.GIF'))
                        {
                            recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>')
                        }
                    }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - ((gifCount * 0.05) > 1 ? 1 : (gifCount * 0.05)))
                    ruleScore = ruleScore + (rulesWeight.get('18') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
                else
                {
                    ruleScore = ruleScore + rulesWeight.get('18')
                }
            }



            //(21 - Avoid URL redirects resources Priority:Medium Difficulty:Moderate Source:YSLOW)

            if('21' == excludeRuleArray.find{ it == '21'})
            {
                ruleScore = ruleScore + rulesWeight.get('21')
            }
            else
            {
                if (redirectResources.size == 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('21')
                }
                else
                {
                    recommendation.append(',{"id" :21,"comment" :"Avoid redirect for resource').append(CommonUtils.getPlural(redirectResources.size)).append(':<ul>')
                    redirectResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : ').append(redirectResources.size())
                    indvRuleScore = (1 - ((redirectResources.size() * 0.05) > 1 ? 1 : (redirectResources.size() * 0.05)))
                    ruleScore = ruleScore + (rulesWeight.get('21') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }


            //(22 - Set Proper Caching Hearder Priority:High Difficulty:Easy Source:Page Speed)
            if('22' == excludeRuleArray.find{ it == '22'})
            {
                ruleScore = ruleScore + rulesWeight.get('22')
            }
            else
            {
                if (nonCachedResources.size <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('22')
                }
                else
                {
                    indvRuleScore = (1 - ((nonCachedResources.size * 0.1) > 1 ? 1 : (nonCachedResources.size * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('22') * indvRuleScore)
                    recommendation.append(',{"id" :22,"comment" :"Make sure Expire or Cache-Control header set for static resources:<ul>')
                    nonCachedResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
            }

            //(23 - Specify a cache validator :High Difficulty:Easy Source:Page Speed)
            if('23' == excludeRuleArray.find{ it == '23'})
            {
                ruleScore = ruleScore + rulesWeight.get('23')
            }
            else
            {
                if (noCacheValidator.size <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('23')
                }
                else
                {
                    indvRuleScore = (1 - ((noCacheValidator.size * 0.1) > 1 ? 1 : (noCacheValidator.size * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('23') * indvRuleScore)
                    recommendation.append(',{"id" :23,"comment" :"Static resources with Last-Modified or ETag header will allow browsers to take advantage of the full benefits of caching.Below resources doesnt have the header set:<ul>')
                    noCacheValidator.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
            }

            //(24 - Specify a Vary: Accept-Encoding header :High Difficulty:Easy Source:Page Speed)
            if('24' == excludeRuleArray.find{ it == '24'})
            {
                ruleScore = ruleScore + rulesWeight.get('24')
            }
            else
            {
                if (noVary.size <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('24')
                }
                else
                {
                    indvRuleScore = (1 - ((noVary.size * 0.1) > 1 ? 1 : (noVary.size * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('24') * indvRuleScore)
                    recommendation.append(',{"id" :24,"comment" :"Following resources doest have Vary: Accept-Encoding header instructs the proxy to store both a compressed and uncompressed version of the resource :<ul>')
                    noVary.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')

                }
            }


            //(25 - Enable Keep Alive Priority:High Difficulty:Easy Source:Page Speed)
            if('25' == excludeRuleArray.find{ it == '25'})
            {
                ruleScore = ruleScore + rulesWeight.get('25')
            }
            else
            {
                if (noKeepAlive.size <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('25')
                }
                else
                {
                    recommendation.append(',{"id" :25,"comment" :"Set Connection: Keep-Alive for the following resources to avoid DNS lookup:<ul>')
                    noKeepAlive.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - (((noKeepAlive.size) * 0.11) > 1 ? 1 : ((noKeepAlive.size) * 0.11)))
                    ruleScore = ruleScore + (rulesWeight.get('25')* indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }

            def duplicateResources = resourcesUrl.findAll{resourcesUrl.count(it)>1}.unique()

            //(26 - Avoid Duplicate Resources Priority:High Difficulty:Easy Source:Page Speed)
            if('26' == excludeRuleArray.find{ it == '26'})
            {
                ruleScore = ruleScore + rulesWeight.get('26')
            }
            else
            {
                if (duplicateResources.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('26')
                }
                else
                {
                    recommendation.append(',{"id" :26,"comment" :"Following resources are called redundantly:<ul>')
                    duplicateResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - ((duplicateResources.size() * 0.2) > 1 ? 1 : (duplicateResources.size() * 0.2)))
                    ruleScore = ruleScore + (rulesWeight.get('26') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }

            def resourcesWithQS = resourcesUrl.findAll { it.contains('?')}

            //(27 - Avoid Resources with Query String Priority:High Difficulty:Easy Source:Page Speed)
            if('27' == excludeRuleArray.find{ it == '27'})
            {
                ruleScore = ruleScore + rulesWeight.get('27')
            }
            else
            {
                if (resourcesWithQS.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('27')
                }
                else
                {
                    recommendation.append(',{"id" :27,"comment" :"Resources with a ? or & in the URL are not cached by some proxy caching servers, and moving the query string and encode the parameters into the URL :<ul>')
                    resourcesWithQS.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - ((resourcesWithQS.size() * 0.1) > 1 ? 1 : (resourcesWithQS.size() * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('27') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }

            def flashExists = resourcesUrl.findAll { it.contains('.swf')}

            //(28 - Avoid Flash Priority:High Difficulty:Easy Source:Page Speed)
            if('28' == excludeRuleArray.find{ it == '28'})
            {
                ruleScore = ruleScore + rulesWeight.get('28')
            }
            else
            {
                if (flashExists.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('28')
                }
                else
                {
                    recommendation.append(',{"id" :28,"comment" :"Replace Flash with HTML5, CSS, and JavaScript:<ul>')
                    flashExists.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - ((flashExists.size() * 0.1) > 1 ? 1 : (flashExists.size() * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('28') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }

            //(29 - Compress Static files Priority:High Difficulty:Easy Source:Page Speed)
            if('29' == excludeRuleArray.find{ it == '29'})
            {
                ruleScore = ruleScore + rulesWeight.get('29')
            }
            else
            {
                if (nonGzipResources.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('29')
                }
                else
                {
                    recommendation.append(',{"id" :29,"comment" :"Following static resources were not compressed:<ul>')
                    nonGzipResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - ((nonGzipResources.size() * 0.1) > 1 ? 1 : (nonGzipResources.size() * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('29') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }

            //(30- Minify CSS & JS Priority:High Difficulty:Easy Source:Page Speed)
            if('30' == excludeRuleArray.find{ it == '30'})
            {
                ruleScore = ruleScore + rulesWeight.get('30')
            }
            else
            {
                if (minifyResources.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('30')
                }
                else
                {
                    recommendation.append(',{"id" :30,"comment" :"Following static resources can be minified:<ul>')
                    minifyResources.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : ').append((minifyPcntTotal/minifyCnt) * 100)
                    indvRuleScore = (1 - ((minifyResources.size() * 0.1) > 1 ? 1 : (minifyResources.size() * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('30') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }

            //(31 - Max Age < 3600 seconds Priority:High Difficulty:Easy Source:Page Speed)
            if('31' == excludeRuleArray.find{ it == '31'})
            {
                ruleScore = ruleScore + rulesWeight.get('31')
            }
            else
            {
                if (maxAge.size() <= 0)
                {
                    ruleScore = ruleScore + rulesWeight.get('31')
                }
                else
                {
                    recommendation.append(',{"id" :31,"comment" :"Following static resources were having cache expiry less than hour:<ul>')
                    maxAge.each { recommendation.append('<li>').append(it.replaceAll('[",\\{\\}\\[\\]]+','')).append('</li>') }
                    recommendation.append('</ul>"')
                    recommendation.append(',"value" : 0')
                    indvRuleScore = (1 - ((maxAge.size() * 0.1) > 1 ? 1 : (maxAge.size() * 0.1)))
                    ruleScore = ruleScore + (rulesWeight.get('31') * indvRuleScore)
                    recommendation.append(',"score" : ').append((indvRuleScore * 100).toDouble().round()).append('}')
                }
            }




        }
        else
        {
            //Added 05/18/16 to tally the score for calls with 0 resources
            ruleScore = ruleScore + (rulesWeight.get('15') + rulesWeight.get('16') + rulesWeight.get('17') + rulesWeight.get('18') + rulesWeight.get('21') + rulesWeight.get('22') + rulesWeight.get('23') + rulesWeight.get('24') + rulesWeight.get('25') + rulesWeight.get('26') + rulesWeight.get('27') + rulesWeight.get('28') + rulesWeight.get('29') + rulesWeight.get('30') + rulesWeight.get('31'))

        }

        recommendation.replace(0,4,'')
        LOGGER.debug 'Transaction : ' +  sampleData.TransactionName + ' Recommendation : ' + recommendation
        LOGGER.debug 'END applySoftNavigationRules'
        def score = (ruleScore * 100.00).round()
        return [score,recommendation]
    }

    static def calculateBlockingTime(def resourceList)
    {
        double receive,dns,wait,blocked,time,ssl,connect
        double totalBlocked = 0;
        if(resourceList.size() > 0)
        {
            for(int i=0;i < resourceList.size();i++ )
            {
                time = Math.round(resourceList[i].duration)
                dns = ((CommonUtils.isNullorZero(resourceList[i].domainLookupEnd) || CommonUtils.isNullorZero(resourceList[i].domainLookupStart)) ?  0 : Math.round((resourceList[i].domainLookupEnd  - resourceList[i].domainLookupStart)))
                connect = ((CommonUtils.isNullorZero(resourceList[i].connectEnd) || CommonUtils.isNullorZero(resourceList[i].connectStart)) ?  0 : Math.round(resourceList[i].connectEnd - resourceList[i].connectStart))
                ssl = 0
                try
                {
                    // calc ssl only if req is secure connection
                    if (resourceList[i]?.name != null && resourceList[i].name.toLowerCase().startsWith("https:"))
                    {
                        ssl = ((CommonUtils.isNullorZero(resourceList[i].secureConnectionStart) || CommonUtils.isNullorZero(resourceList[i].connectEnd)) ?  0 : Math.round(resourceList[i].connectEnd - resourceList[i].secureConnectionStart))
                        ssl = Math.round(ssl);
                        if (connect > 0 && ssl > 0)
                        {
                            ssl = connect - ssl
                        }
                    }
                    else
                    {
                        ssl = 0
                    }
                }
                catch (Exception ex)
                {
                }

                //send = ((CommonUtils.isNullorZero(harSample.Resources[i].requestStart) || CommonUtils.isNullorZero(harSample.Resources[i].connectEnd)) ?  0 : Math.round(harSample.Resources[i].requestStart - harSample.Resources[i].connectEnd))
                //LOGGER.debug 'OUTPUT Completed Send'

                wait = ((CommonUtils.isNullorZero(resourceList[i].responseStart) || CommonUtils.isNullorZero(resourceList[i].requestStart)) ?  0 : Math.round(resourceList[i].responseStart - resourceList[i].requestStart))


                receive = ((CommonUtils.isNullorZero(resourceList[i].responseStart) || CommonUtils.isNullorZero(resourceList[i].responseEnd)) ?  0 : Math.round(resourceList[i].responseEnd - resourceList[i].responseEnd))

                //blocked = time - (dns + connect + ssl + receive + send + wait)
                blocked = time - (dns + connect + ssl + receive + wait)

                if(blocked == time)
                {
                    blocked = 0
                }

                if (blocked < 0 )
                {
                    time = time - blocked
                    blocked = 0
                }
                totalBlocked = totalBlocked + blocked
            }
        }
        return totalBlocked
    }

    static def calculateBackendTime(def configReader,def totalTime,def resourceListOrg,boolean analysis = true)
    {
        StringBuilder backendAnalysis = new StringBuilder()
        double totalBlocked = 0.0
        if(resourceListOrg.size > 0)
        {
            def resourceList =[]
            def resourceListNew =[]
            def resource = [:]


            if(configReader?.isNativeApp != null && configReader.isNativeApp == true)
            {
                resourceListOrg.each { it ->
                    resource.put('domain',it.HostName)
                    resource.put('start',it.startTime)
                    resource.put('end',it.responseEnd)
                    resourceList.add(resource.clone())
                    if(CommonUtils.notNullAndZero(it?.startTime) && CommonUtils.notNullAndZero(it?.requestStart))
                    {
                        totalBlocked = totalBlocked + (it.requestStart - it.startTime)
                    }
                }

            }
            else
            {
                resourceListOrg.each { it ->
                    resource.put('domain',it.HostName)
                    resource.put('start',it.fetchStart)
                    resource.put('end',it.responseEnd)
                    resourceList.add(resource.clone())
                    if(CommonUtils.notNullAndZero(it?.startTime) && CommonUtils.notNullAndZero(it?.requestStart))
                    {
                        totalBlocked = totalBlocked + (it.requestStart - it.startTime)
                    }
                }

            }

            if(analysis)
            {
                resourceList.sort{x,y->
                    if(x.start == y.start){
                        x.end <=> y.end
                    }else{
                        x.start <=> y.start
                    }
                }

                for(int i = 0 ; i < resourceList.size(); i++ )
                {
                    if (i != (resourceList.size() - 1) && resourceList[i].start == resourceList[i + 1].start)
                    {

                    }
                    else
                    {
                        resourceListNew.add(resourceList[i].clone())
                    }

                }

                resourceList.clear()

                double furthestEnd = 0

                for(int i = 0 ; i < resourceListNew.size(); i++ )
                {
                    if (resourceListNew[i].start < furthestEnd)
                    {
                        resourceListNew[i].start = furthestEnd
                    }
                    if(resourceListNew[i].start < resourceListNew[i].end)
                    {
                        furthestEnd = resourceListNew[i].end
                        resourceList.add(resourceListNew[i].clone())
                    }

                }

                resourceListNew.clear()

                double backend = 0
                double resTime = 0
                for(int i = 0 ; i < resourceList.size(); i++ )
                {
                    if (i != (resourceList.size() - 1) && resourceList[i].start == resourceList[i + 1].start)
                    {

                    }
                    else
                    {
                        resTime = (resourceList[i].end - resourceList[i].start)
                        resourceList[i].duration = resTime
                        backend = backend + resTime
                        resourceListNew.add(resourceList[i].clone())
                    }

                }

                def domain = resourceListNew.groupBy{it.domain}.collectEntries {[(it.key): it.value.sum {it.duration}]}

                int cnt = 0
                domain.each{k,v ->
                    if(cnt ==0)
                    {
                        backendAnalysis.append('{"domain" : "').append(k).append('",').append('"timetaken" :').append(v.round()).append('}')
                        cnt++
                    }
                    else
                    {
                        backendAnalysis.append(',{"domain" : "').append(k).append('",').append('"timetaken" :').append(v.round()).append('}')
                    }
                }

            }
        }
        return [backendAnalysis,totalBlocked]

    }

    static StringBuilder analyzeServerTime(def currTxnMetrics,def prevTxnMetrics)
    {
        StringBuilder observation = new StringBuilder()
        currTxnMetrics = currTxnMetrics.hits.hits[0]._source
        prevTxnMetrics = prevTxnMetrics.hits.hits[0]._source
        try
        {

            if ((currTxnMetrics.serverTime_ttlb - prevTxnMetrics.serverTime_ttlb) > 0)
            {
                observation.append('<ul><li>Server Processing increased by ').append((currTxnMetrics.serverTime_ttlb - prevTxnMetrics.serverTime_ttlb)).append(' ms.Use APM or Profiling tools to analyze server call.</li></ul>')
            }
            else
            {
                observation.append('<ul><li>Server Time inline with baseline run.</li></ul>')
            }

        }
        catch(Exception e)
        {
            observation.append('<ul><li>Problem analysing Server Time.</li></ul>')
        }



        return observation

    }

    static StringBuilder analyzeNetworkTime(def currTxnMetrics,def prevTxnMetrics)
    {

        currTxnMetrics = currTxnMetrics.hits.hits[0]._source
        prevTxnMetrics = prevTxnMetrics.hits.hits[0]._source

        StringBuilder observation = new StringBuilder()

        try
        {
            if (((currTxnMetrics.fetchStartTime + currTxnMetrics.redirectTime + currTxnMetrics.cacheFetchTime + currTxnMetrics.dnsLookupTime + currTxnMetrics.downloadTime) - (prevTxnMetrics.fetchStartTime + prevTxnMetrics.redirectTime + prevTxnMetrics.cacheFetchTime + prevTxnMetrics.dnsLookupTime + prevTxnMetrics.downloadTime)) > 0)
            {
                observation.append('<ul><li>Network Time increased by ').append(((currTxnMetrics.fetchStartTime + currTxnMetrics.redirectTime + currTxnMetrics.cacheFetchTime + currTxnMetrics.dnsLookupTime + currTxnMetrics.downloadTime) - (prevTxnMetrics.fetchStartTime + prevTxnMetrics.redirectTime + prevTxnMetrics.cacheFetchTime + prevTxnMetrics.dnsLookupTime + prevTxnMetrics.downloadTime))).append(' ms.</li><ul>')

                if (currTxnMetrics.fetchStartTime > prevTxnMetrics.fetchStartTime)
                {
                    observation.append('<li>Fetch Start Time increased by ').append((currTxnMetrics.fetchStartTime - prevTxnMetrics.fetchStartTime)).append(' ms.</li>')
                }

                if (currTxnMetrics.redirectTime > prevTxnMetrics.redirectTime)
                {
                    observation.append('<li>Redirect Time increased by ' + (currTxnMetrics.redirectTime - prevTxnMetrics.redirectTime) + ' ms.</li>')
                }

                if (currTxnMetrics.cacheFetchTime > prevTxnMetrics.cacheFetchTime)
                {
                    observation.append('<li>Cache Fetch Time increased by ').append((currTxnMetrics.cacheFetchTime - prevTxnMetrics.cacheFetchTime)).append(' ms.</li>')
                }

                if (currTxnMetrics.dnsLookupTime > prevTxnMetrics.dnsLookupTime)
                {
                    observation.append('<li>DNSLookup Time increased by ').append((currTxnMetrics.dnsLookupTime - prevTxnMetrics.dnsLookupTime)).append(' ms.</li>')
                }
                if (currTxnMetrics.tcpConnectTime > prevTxnMetrics.tcpConnectTime)
                {
                    observation.append('<li>TCP Connect Time increased by ').append((currTxnMetrics.tcpConnectTime - prevTxnMetrics.tcpConnectTime)).append(' ms.Consider KeepAlive connections</li>')
                }

                if (currTxnMetrics.downloadTime > prevTxnMetrics.downloadTime)
                {
                    observation.append('<li>Network Time to download HTML document increased by ').append((currTxnMetrics.downloadTime - prevTxnMetrics.downloadTime)).append(' ms.</li>')
                }

                observation.append('</ul></ul>')
            }
            else
            {
                observation.append('<ul><li>Network Time inline with baseline run</li></ul>')
            }

        }
        catch(Exception e)
        {
            observation.append('<ul><li>Problem analysing Network Time</li></ul>')
        }


        return observation

    }

    static StringBuilder analyzeDOMProcessingTime(def currTxnMetrics,def prevTxnMetrics)
    {

        StringBuilder observation = new StringBuilder()

        try
        {
            if ((currTxnMetrics.hits.hits[0]._source.domLoadingTime - prevTxnMetrics.hits.hits[0]._source.domLoadingTime) > 0)
            {
                observation.append('<ul><li>DOM Content Loading time increased by ').append((currTxnMetrics.hits.hits[0]._source.domLoadingTime - prevTxnMetrics.hits.hits[0]._source.domLoadingTime)).append(' ms.</li><ul>')

                if (currTxnMetrics.hits.hits[0]._source.Resources.size() > prevTxnMetrics.hits.hits[0]._source.Resources.size())
                {
                    observation.append('<li>Number of resources increased by ').append((currTxnMetrics.hits.hits[0]._source.Resources.size() - prevTxnMetrics.hits.hits[0]._source.Resources.size())).append('.</li>')
                    observation.append(PaceRuleEngine.analyzeAppCache(currTxnMetrics,prevTxnMetrics))
                }

                if (currTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count > prevTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count)
                {
                    observation.append('<li>Number of static resources increased for the current run by ').append((currTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count - prevTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count)).append(' (Current Count : ').append(currTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count).append(' ,Baseline Count : ').append(prevTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count).append(').Consider reducing number of HTTP calls by combining css and js</li>')
                }

                if(currTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count > prevTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count)
                {
                    observation.append('<li>Number of non-static resources increased for the current run by ').append((currTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count - prevTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count)).append(' (Current Count : ').append(currTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count).append(' ,Baseline Count : ').append(prevTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count).append(').Consider reducing number of HTTP calls</li>')
                }

                if((currTxnMetrics.hits.hits[0]._source.domContentLoadedEventStart - currTxnMetrics.hits.hits[0]._source.domInteractive) > 5)
                {
                    if((prevTxnMetrics.hits.hits[0]._source.domContentLoadedEventStart - prevTxnMetrics.hits.hits[0]._source.domInteractive) > 5)
                    {
                        observation.append('<li>Both current and baseline run has parser blocking Javascript.Avoid blocking java script calls by using async</li>')
                    }
                    else
                    {
                        observation.append('<li>Only current run has parser blocking Javascript.Avoid blocking java script calls by using async</li>')
                    }
                }


                observation.append('</ul></ul>')
            }
            else
            {
                observation.append('<ul><li>DOM Loading time inline with baseline</li></ul>')
            }

        }
        catch(Exception e)
        {
            observation.append('<ul><li>Problem analysing DOM Loading time</li></ul>')
        }



        return observation

    }

    static StringBuilder analyzeRenderingTime(def currTxnMetrics,def prevTxnMetrics)
    {
        StringBuilder observation = new StringBuilder()
        def currRunMap = [:]
        def prevRunMap = [:]
        double CurrValue,PrevValue
        try {
            if (((currTxnMetrics.hits.hits[0]._source.renderingTime + currTxnMetrics.hits.hits[0]._source.onloadTime) - (prevTxnMetrics.hits.hits[0]._source.renderingTime + prevTxnMetrics.hits.hits[0]._source.onloadTime)) > 0) {
                observation.append('<ul><li>Rendering increased by ').append(((currTxnMetrics.hits.hits[0]._source.renderingTime + currTxnMetrics.hits.hits[0]._source.onloadTime) - (prevTxnMetrics.hits.hits[0]._source.renderingTime + prevTxnMetrics.hits.hits[0]._source.onloadTime))).append(' ms.</li><ul>')

                if (currTxnMetrics.hits.hits[0]._source.renderingTime > prevTxnMetrics.hits.hits[0]._source.renderingTime) {
                    observation.append('<li>DOM Complete time increased by ').append((currTxnMetrics.hits.hits[0]._source.renderingTime - prevTxnMetrics.hits.hits[0]._source.renderingTime)).append(' ms.</li>')
                }
                if (currTxnMetrics.hits.hits[0]._source.onloadTime > prevTxnMetrics.hits.hits[0]._source.onloadTime) {
                    observation.append('<li>Onload time increased by ').append((currTxnMetrics.hits.hits[0]._source.onloadTime - prevTxnMetrics.hits.hits[0]._source.onloadTime)).append(' ms.</li>')
                }
                if (currTxnMetrics.aggregations.Resources.Total_duration.value > prevTxnMetrics.aggregations.Resources.Total_duration.value) {
                    observation.append('<li>Aggregate resources download duration increased by ').append((currTxnMetrics.aggregations.Resources.Total_duration.value - prevTxnMetrics.aggregations.Resources.Total_duration.value).doubleValue().round()).append(' ms.</li>')
                }

                def currRunDurationForEveryHost = currTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets
                def prevRunDurationForEveryHost = prevTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets

                for (int i = 0; i < currRunDurationForEveryHost.size(); i++) {
                    currRunMap.put(currRunDurationForEveryHost[i].key, currRunDurationForEveryHost[i].TotalDurationByHost.value)
                }

                for (int i = 0; i < prevRunDurationForEveryHost.size(); i++) {
                    prevRunMap.put(prevRunDurationForEveryHost[i].key, prevRunDurationForEveryHost[i].TotalDurationByHost.value)
                }

                for (String key : currRunMap.keySet()) {
                    CurrValue = currRunMap.get(key)
                    PrevValue = (prevRunMap.get(key) == null ? 0 : prevRunMap.get(key))
                    if (CurrValue > PrevValue) {
                        observation.append('<li>Total duration to ').append(key).append(' increased by ').append((CurrValue - PrevValue).doubleValue().round()).append(' ms.</li>')
                    }
                }

                if (currTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count > prevTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count) {
                    observation.append('<li>Number of images increased by ').append((currTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count - prevTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count)).append('.Reduce image hits by implementing inline image/css sprites</li>')
                }
                if (currTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count > prevTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count) {
                    observation.append('<li>Number of CSS and JS files increased by ').append((currTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count - prevTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count)).append('.Combine JS and CSS from same domains.</li>')
                }

                def redirectTimeAnalysis = PaceRuleEngine.analyzeRedirectTime(currTxnMetrics, prevTxnMetrics)
                def tcpTimeAnalysis = PaceRuleEngine.analyzetcpConnectTime(currTxnMetrics, prevTxnMetrics)
                def dnsLookupTimeAnalysis = PaceRuleEngine.analyzeDNSLookUp(currTxnMetrics, prevTxnMetrics)

                observation.append(redirectTimeAnalysis + tcpTimeAnalysis + dnsLookupTimeAnalysis + '</ul></ul>')
            } else {
                observation.append('<ul><li>Rendering Time inline with baseline run</li></ul>')
            }
        }
        catch(Exception e)
        {
            observation.append('<ul><li>Problem analysing Rendering Time</li></ul>')
        }

        return observation
    }

    static StringBuilder analyzeRedirectTime(def currTxnMetrics,def prevTxnMetrics)
    {
        StringBuilder observation = new StringBuilder()
        def currRunMap = [:]
        def prevRunMap = [:]
        double CurrValue,PrevValue

        try {
            if (currTxnMetrics.aggregations.Resources.Total_redirectTime.value > prevTxnMetrics.aggregations.Resources.Total_redirectTime.value) {
                observation.append('<li>Aggregate redirectTime increased by ').append((currTxnMetrics.aggregations.Resources.Total_redirectTime.value - prevTxnMetrics.aggregations.Resources.Total_redirectTime.value).doubleValue().round()).append(' ms.</li>')
            }

            if (currTxnMetrics.aggregations.Filter.NumOfResrcRedirectCount.doc_count > prevTxnMetrics.aggregations.Filter.NumOfResrcRedirectCount.doc_count) {
                observation.append('<li>Total number of resources redirected increased by ').append((currTxnMetrics.aggregations.Filter.NumOfResrcRedirectCount.doc_count - prevTxnMetrics.aggregations.Filter.NumOfResrcRedirectCount.doc_count)).append(' for the current run.Avoid redirects for resources</li>')
            }

            def currRunRedirectTimeForEveryHost = currTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets
            def prevRunRedirectTimeForEveryHost = prevTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets

            for (int i = 0; i < currRunRedirectTimeForEveryHost.size(); i++) {
                currRunMap.put(currRunRedirectTimeForEveryHost[i].key, currRunRedirectTimeForEveryHost[i].TotalredirectTimeByHost.value)
            }

            for (int i = 0; i < prevRunRedirectTimeForEveryHost.size(); i++) {
                prevRunMap.put(prevRunRedirectTimeForEveryHost[i].key, prevRunRedirectTimeForEveryHost[i].TotalredirectTimeByHost.value)
            }

            for (String key : currRunMap.keySet()) {
                CurrValue = currRunMap.get(key)
                PrevValue = (prevRunMap.get(key) == null ? 0 : prevRunMap.get(key))
                if (CurrValue > PrevValue) {
                    observation.append('<li>Total redirectTime to ').append(key).append(' increased by ').append((CurrValue - PrevValue).doubleValue().round()).append(' ms.</li>')
                }
            }
        }
        catch(Exception e)
        {
            observation.append('<li>Problem analysing redirect time</li>')
        }
        return observation

    }

    static StringBuilder analyzetcpConnectTime(def currTxnMetrics,def prevTxnMetrics)
    {
        StringBuilder observation = new StringBuilder()
        def currRunMap = [:]
        def prevRunMap = [:]
        double CurrValue,PrevValue
        try
        {
            if (currTxnMetrics.aggregations.Resources.Total_tcpConnectTime.value > prevTxnMetrics.aggregations.Resources.Total_tcpConnectTime.value)
            {
                observation.append('<li>Aggregate tcpConnectTime increased by ').append((currTxnMetrics.aggregations.Resources.Total_tcpConnectTime.value - prevTxnMetrics.aggregations.Resources.Total_tcpConnectTime.value).doubleValue().round()).append(' ms</li>')
            }

            def currRunTCPMetricForEveryHost = currTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets
            def prevRunTCPMetricForEveryHost = prevTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets

            for (int i=0;i<currRunTCPMetricForEveryHost.size();i++)
            {
                currRunMap.put(currRunTCPMetricForEveryHost[i].key,currRunTCPMetricForEveryHost[i].TotaltcpConnectTimeByHost.value)
            }

            for (int i=0;i<prevRunTCPMetricForEveryHost.size();i++)
            {
                prevRunMap.put(prevRunTCPMetricForEveryHost[i].key,prevRunTCPMetricForEveryHost[i].TotaltcpConnectTimeByHost.value)
            }

            for (String key : currRunMap.keySet())
            {
                CurrValue = currRunMap.get(key)
                PrevValue = (prevRunMap.get(key) == null ? 0 : prevRunMap.get(key))
                if (CurrValue > PrevValue)
                {
                    observation.append('<li>Total tcpConnectTime to ').append(key).append(' increased by ').append((CurrValue-PrevValue).doubleValue().round()).append(' ms.</li>')
                }
            }
        }
        catch(Exception e)
        {
            observation.append('<li>Problem analysing tcp connect time</li>')
        }
        return observation

    }

    static StringBuilder analyzeDNSLookUp(def currTxnMetrics,def prevTxnMetrics)
    {
        StringBuilder observation = new StringBuilder()
        int currRunDNSLookupCount,prevRunDNSLookupCount
        def currRunMap = [:]
        def prevRunMap = [:]
        double CurrValue,PrevValue
        try
        {
            currRunDNSLookupCount=currTxnMetrics.aggregations.Filter.NumOfNonCachedResrc.doc_count
            prevRunDNSLookupCount=prevTxnMetrics.aggregations.Filter.NumOfNonCachedResrc.doc_count

            if (currRunDNSLookupCount>prevRunDNSLookupCount)
            {
                observation.append('<li>Total Number of DNS LookUps increased for the Current run by ').append((currRunDNSLookupCount-prevRunDNSLookupCount)).append(' (Current Count : ').append(currRunDNSLookupCount).append(' Baseline Count : ').append(prevRunDNSLookupCount).append(').</li>')
            }
            if (currTxnMetrics.aggregations.Resources.Total_dnsLookupTime.value > prevTxnMetrics.aggregations.Resources.Total_dnsLookupTime.value)
            {
                observation.append('<li>Aggregate DNSLookUp time increased by ').append((currTxnMetrics.aggregations.Resources.Total_dnsLookupTime.value - prevTxnMetrics.aggregations.Resources.Total_dnsLookupTime.value).doubleValue().round(3)).append(' ms.</li>')
            }

            def currRunDNSMetricForEveryHost = currTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets
            def prevRunDNSMetricForEveryHost = prevTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets

            for (int i=0;i<currRunDNSMetricForEveryHost.size();i++)
            {
                currRunMap.put(currRunDNSMetricForEveryHost[i].key,currRunDNSMetricForEveryHost[i].TotaldnsLookupTimeByHost.value)
            }

            for (int i=0;i<prevRunDNSMetricForEveryHost.size();i++)
            {
                prevRunMap.put(prevRunDNSMetricForEveryHost[i].key,prevRunDNSMetricForEveryHost[i].TotaldnsLookupTimeByHost.value)
            }

            for ( String key : currRunMap.keySet() )
            {
                CurrValue = currRunMap.get(key)
                PrevValue = (prevRunMap.get(key) == null ? 0 : prevRunMap.get(key))

                if (CurrValue > PrevValue)
                {
                    observation.append('<li>Total DNSLookUp time increased by ').append((CurrValue-PrevValue).doubleValue().round()).append(' ms for host - ').append(key).append('.</li>')
                }
            }

        }
        catch(Exception e)
        {
            observation.append('<li>Problem analysing dnslookup time</li>')
        }

        return observation
    }

    static StringBuilder analyzeAppCache(def currTxnMetrics,def prevTxnMetrics)
    {
        StringBuilder observation = new StringBuilder()

        int currRunNumOfCachedResrc,currRunNumOfNonCachedResrc,currRunNumOfStaticResrc,currRunNumOfNonStaticResrc=0
        int prevRunNumOfCachedResrc,prevRunNumOfNonCachedResrc,prevRunNumOfStaticResrc,prevRunNumOfNonStaticResrc=0

        try
        {
            currRunNumOfCachedResrc =  currTxnMetrics.aggregations.Filter.NumOfCachedResrc.doc_count
            //currRunNumOfNonCachedResrc = currTxnMetrics.aggregations.Filter.NumOfNonCachedResrc.doc_count
            currRunNumOfStaticResrc = currTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count
            currRunNumOfNonStaticResrc = currTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count

            prevRunNumOfCachedResrc =  prevTxnMetrics.aggregations.Filter.NumOfCachedResrc.doc_count
            //prevRunNumOfNonCachedResrc = prevTxnMetrics.aggregations.Filter.NumOfNonCachedResrc.doc_count
            prevRunNumOfStaticResrc = prevTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count
            prevRunNumOfNonStaticResrc = prevTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count


            if (prevRunNumOfCachedResrc > currRunNumOfCachedResrc)
            {
                observation.append('<li>CacheHitRatio for Current Run is ').append(Math.round((currRunNumOfCachedResrc/currRunNumOfStaticResrc)*100.00)).append('% and Baseline Run is ').append(Math.round((prevRunNumOfCachedResrc/prevRunNumOfStaticResrc)*100.00)).append('%.</li>')
            }
            if (currTxnMetrics.aggregations.Resources.Total_cacheFetchTime.value > prevTxnMetrics.aggregations.Resources.Total_cacheFetchTime.value)
            {
                observation.append('<li>Aggregate cacheFetchTime increased by ').append(((currTxnMetrics.aggregations.Resources.Total_cacheFetchTime.value - prevTxnMetrics.aggregations.Resources.Total_cacheFetchTime.value).doubleValue()).round()).append(' ms.</li>')
            }
            if (currRunNumOfNonStaticResrc > prevRunNumOfNonStaticResrc)
            {
                observation.append('<li>Total number non-static resources increased by ').append((currRunNumOfNonStaticResrc - prevRunNumOfNonStaticResrc)).append('.</li>')
            }
        }
        catch(Exception e)
        {
            observation.append('<li>Problem analysing cache time</li>')
        }

        return observation
    }

    static StringBuilder analyzeResources(def currTxnMetrics,def prevTxnMetrics)
    {

        StringBuilder observation = new StringBuilder()

        if (currTxnMetrics.hits.hits[0]._source.Resources.size() > prevTxnMetrics.hits.hits[0]._source.Resources.size())
        {
            observation.append('<li>Number of resources increased by ').append((currTxnMetrics.hits.hits[0]._source.Resources.size() - prevTxnMetrics.hits.hits[0]._source.Resources.size())).append('.</li>')
            observation.append(PaceRuleEngine.analyzeAppCache(currTxnMetrics,prevTxnMetrics))
        }

        if (currTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count > prevTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count)
        {
            observation.append('<li>Number of static resources increased for the current run by ').append((currTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count - prevTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count)).append(' (Current Count : ').append(currTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count).append(' ,Baseline Count : ').append(prevTxnMetrics.aggregations.Filter.NumOfStaticResrc.doc_count).append(').Consider reducing number of HTTP calls by combining css and js</li>')
        }

        if(currTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count > prevTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count)
        {
            observation.append('<li>Number of non-static resources increased for the current run by ').append((currTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count - prevTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count)).append(' (Current Count : ').append(currTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count).append(' ,Baseline Count : ').append(prevTxnMetrics.aggregations.Filter.NumOfNonStaticResrc.doc_count).append(').Consider reducing number of HTTP calls</li>')
        }

        def currRunMap = [:]
        def prevRunMap = [:]
        double CurrValue,PrevValue

        if (currTxnMetrics.aggregations.Resources.Total_duration.value > prevTxnMetrics.aggregations.Resources.Total_duration.value)
        {
            observation.append('<li>Aggregate resources download duration increased by ').append((currTxnMetrics.aggregations.Resources.Total_duration.value - prevTxnMetrics.aggregations.Resources.Total_duration.value).doubleValue().round()).append(' ms.</li>')
        }

        def currRunDurationForEveryHost = currTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets
        def prevRunDurationForEveryHost = prevTxnMetrics.aggregations.Filter.HostAggregation.MetricForEveryHost.buckets

        for (int i=0;i<currRunDurationForEveryHost.size();i++)
        {
            currRunMap.put(currRunDurationForEveryHost[i].key,currRunDurationForEveryHost[i].TotalDurationByHost.value)
        }

        for (int i=0;i<prevRunDurationForEveryHost.size();i++)
        {
            prevRunMap.put(prevRunDurationForEveryHost[i].key,prevRunDurationForEveryHost[i].TotalDurationByHost.value)
        }

        for (String key : currRunMap.keySet())
        {
            CurrValue = currRunMap.get(key)
            PrevValue = (prevRunMap.get(key) == null ? 0 : prevRunMap.get(key))
            if (CurrValue > PrevValue)
            {
                observation.append('<li>Total duration to ').append(key).append(' increased by ').append((CurrValue-PrevValue).doubleValue().round()).append(' ms.</li>')
            }
        }

        if(currTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count > prevTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count)
        {
            observation.append('<li>Number of images increased by ').append((currTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count - prevTxnMetrics.aggregations.Filter.NumOfResrcImages.doc_count)).append('.Reduce image hits by implementing inline image/css sprites</li>')
        }
        if(currTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count > prevTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count)
        {
            observation.append('<li>Number of CSS and JS files increased by ').append((currTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count - prevTxnMetrics.aggregations.Filter.NumOfNonResrcImages.doc_count)).append('.Combine JS and CSS from same domains.</li>')
        }

        def redirectTimeAnalysis = PaceRuleEngine.analyzeRedirectTime(currTxnMetrics,prevTxnMetrics)
        def tcpTimeAnalysis = PaceRuleEngine.analyzetcpConnectTime(currTxnMetrics,prevTxnMetrics)
        def dnsLookupTimeAnalysis = PaceRuleEngine.analyzeDNSLookUp(currTxnMetrics,prevTxnMetrics)

        observation.append(redirectTimeAnalysis).append(tcpTimeAnalysis).append(dnsLookupTimeAnalysis)

        return observation

    }

    static StringBuilder analyzeNativeApp(def currTxnMetrics,def prevTxnMetrics)
    {

        StringBuilder observation = new StringBuilder()


        if (currTxnMetrics.hits.hits[0]._source.Resources.size() > prevTxnMetrics.hits.hits[0]._source.Resources.size())
        {
            observation.append('<li>Number of resources increased by ').append((currTxnMetrics.hits.hits[0]._source.Resources.size() - prevTxnMetrics.hits.hits[0]._source.Resources.size())).append('.</li>')
        }

        return observation

    }

}