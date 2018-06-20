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

import groovy.io.FileType
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class CommonUtils
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CommonUtils.class)
    static boolean isNullorZero(def input)
    {
        if(input == null || input == 0)
        {
            return true
        }
        return false
    }

    static boolean notNullAndZero(def input)
    {
        if(input == null || input == 0)
        {
            return false
        }
        return true
    }

    static boolean stringContainsItemFromList(def inputString, def match)
    {
        log.debug("Started CommonUtils.stringContainsItemFromList")
        log.debug("Input String :" + inputString)
        log.debug("Match String :" + match)
        List<String> items = Arrays.asList(match.split("\\s*,\\s*"))
        inputString = inputString.toString().toLowerCase().split("\\?")[0]
        for(int i =0; i < items.size(); i++)
        {
            //if(inputString.toString().toLowerCase().contains(items[i]))
            if(inputString.contains(items[i]))
            {
                log.debug("Return : true")
                return true;
            }
        }
        log.debug("Return : false")
        log.debug("End CommonUtils.stringContainsItemFromList")
        return false;
    }
    static boolean stringNotContainsItemFromList(def inputString, def match)
    {
        log.debug("Started CommonUtils.stringNotContainsItemFromList")
        log.debug("Input String :" + inputString)
        log.debug("Match String :" + match)
        List<String> items = Arrays.asList(match.split("\\s*,\\s*"))
        inputString = inputString.toString().toLowerCase().split("\\?")[0]
        for(int i =0; i < items.size(); i++)
        {

            //if(inputString.toString().toLowerCase().contains(items[i]))
            if(inputString.contains(items[i]))
            {
                log.debug("Return : false")
                return false
            }
        }
        log.debug("Return : true")
        log.debug("End CommonUtils.stringNotContainsItemFromList")
        return true
    }

    static def splitURLandPath(String fullURL)
    {
        def esfullurl = (fullURL).split("/")
        return [esfullurl[0] + '//' + esfullurl[2] + '/',GlobalConstants.STATSINDEX]

    }

    static def getConfiguration(def args)
    {
        def configReader = [:]

        configReader = ElasticSearchUtils.extractConfig(args.ClientName,args.ProjectName,args.Scenario,args.esUrl)
        if (configReader!= null)
        {
            configReader.put('esUrl',args.esUrl)
            def isStaticResources = configReader.staticResourceExtension + ',' + configReader.imageResourceExtension
            def isCompressibleResources = (configReader?.compressibleResourceExtension == null ? '.js,.css,.html,.ttf,.otf,.docx,.doc,.svg' : configReader?.compressibleResourceExtension)
            configReader.put('isStaticResources',isStaticResources)
            configReader.put('isCompressibleResources',isCompressibleResources)
            if(args.AnalysisType == 'Time')
            {
                configReader.put('BaselineStart',CommonUtils.getESTimeFormatForTimeStamp(args.BaselineStart))
                configReader.put('BaselineEnd',CommonUtils.getESTimeFormatForTimeStamp(args.BaselineEnd))
                configReader.put('CurrentStart',CommonUtils.getESTimeFormatForTimeStamp(args.CurrentStart))
                configReader.put('CurrentEnd',CommonUtils.getESTimeFormatForTimeStamp(args.CurrentEnd))
            }

            if(args.AnalysisType == 'Run')
            {
                configReader.put('CurrentRun',(args?.CurrentRun == null ? configReader.RunID : args.CurrentRun))
                configReader.put('BaselineRun',(args?.BaselineRun == null ? configReader.BaselineRunID : args.BaselineRun))
            }

            if(args.AnalysisType == 'Transaction')
            {
                configReader.put('CurrentRun',(args?.CurrentRun == null ? configReader.RunID : args.CurrentRun))
                configReader.put('BaselineRun','0')
            }

            configReader.put('AnalysisType',args.AnalysisType)
        }
        return configReader

    }

    static StringBuilder getInstanceCount(int count)
    {
        StringBuilder retVal = new StringBuilder()
        if (count == 1)
        {
            retVal.append(count)
            retVal.append(' instance')
        }
        else
        {
            retVal.append(count)
            retVal.append(' instances')
        }

    }

    static String getPlural(int count)
    {
        StringBuilder retVal = new StringBuilder()
        if (count == 1)
        {
            retVal.append('')
        }
        else
        {
            retVal.append("'s")
        }

    }
    static def getisorare(int count)
    {
        if (count==1)
        {
            return "is"
        }
        else
        {
            return "are"
        }

    }

    static def getISO8601StringForTimeStamp(def timestamp)
    {
        def dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(timestamp);
    }

    static def getStringForTimeStamp(def timestamp,def timezone='UTC')
    {
        def dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
        if(timezone == 'UTC')
        {
            dateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))
        }
        else
        {
            dateFormat.setTimeZone(TimeZone.getTimeZone(timezone))
        }

        return dateFormat.format(timestamp)
    }

    static def getESTimeFormatForTimeStamp(def timestamp)
    {
        def dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSSZ", Locale.US)
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        return dateFormat.format(Long.parseLong(timestamp.toString()))
    }

    static def getAllRuleFiles(def path)
    {
        def list = []
        def dir = new File(path)
        dir.eachFileRecurse (FileType.FILES) { file ->
            list << file.getAbsolutePath()
        }
        return list

    }

    static def convertMilliToSeconds(def value)
    {
        def retVal = (Float.parseFloat(value.toString())/1000).round(2)
        return retVal
    }

    static def getDifference(def first,def second)
    {

        if(second != 0)
        {
            if(((first == null ? 0 : first) - (second == null ? 0 : second)) < 0)
            {
                return 0
            }
            else
            {
                return (first - second)
            }
        }
        else
        {
            return 0
        }

    }
    static def getDifferenceResource(def first,def second)
    {
        double rtVal = 0.0
        if(first > 0 && second > 0)
        {

            return Double.parseDouble((first - second).toString())
        }
        else
        {
            return rtVal
        }

    }

    static long getRoundedNumber(def input)
    {
        return Math.round(Double.parseDouble(input.toString()))
    }
}