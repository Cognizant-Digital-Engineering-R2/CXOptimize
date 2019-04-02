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

import groovy.json.JsonOutput
import org.json.JSONObject
import org.slf4j.LoggerFactory

import java.math.RoundingMode

class PaceAnalysisEngine
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PaceAnalysisEngine.class)
    static List<Object> generateSummaryReport(def args)
    {
        LOGGER.debug('Entering generateSummaryReport for {}',args.toString())
        def configFlag = 'false'
        StringBuilder report = new StringBuilder()
        StringBuilder reportCopy = new StringBuilder()
        def analysis = []
        args.put('override',args?.override == 'true' ? 'true' : 'false')

        def configReader = CommonUtils.getConfiguration(args)
        LOGGER.debug('CONFIGURATION DETAILS for {} {} {} : {}',args.ClientName,args.ProjectName,args.Scenario,configReader)
        if (configReader != null)
        {
            configFlag = 'true'
            analysis = PaceAnalysisEngine.generateAnalysisReport(args,configReader)
            if(analysis[3])
            {
                report.append(analysis[0])
                LOGGER.debug('ANALYSIS JSON :{}',report)

                reportCopy.append(report)
                report.setLength(0)
                //println reportCopy.toString()
                if(args?.scale == null || args.scale == 'milliseconds')
                {
                    report.append(PaceReportEngine.summaryJSON(configReader,reportCopy.toString(),args?.timezone == null ? 'UTC' : args?.timezone))
                }
                else
                {
                    report.append(PaceReportEngine.summaryJSONInSeconds(configReader,reportCopy.toString(),args?.timezone == null ? 'UTC' : args?.timezone))
                }
                LOGGER.debug('SUMMARY JSON : {}',report)

                //println report.toString()
                //
                if(args?.cicd == null)
                {
                    def errorMsg = []
                    def statusList = []
                    def compliance = [:]
                    def summaryJson = new JSONObject(report.toString())
                    def summaryMap = JsonUtils.jsonToMap(summaryJson)
                    def ignoreTransactionList = []

                    compliance.put('totalScoreComplianceWeight', configReader?.totalScoreComplianceWeight == null ? 25 : configReader?.totalScoreComplianceWeight)
                    compliance.put('compareComplianceWeight', configReader?.compareComplianceWeight == null ? 25 : configReader?.compareComplianceWeight)
                    compliance.put('indvScoreComplianceWeight', configReader?.indvScoreComplianceWeight == null ? 25 : configReader?.indvScoreComplianceWeight)
                    compliance.put('slaComplianceWeight', configReader?.slaComplianceWeight == null ? 25 : configReader?.slaComplianceWeight)
                    compliance.put('totalScoreThreshold', configReader?.totalScoreThreshold == null ? 80 : configReader?.totalScoreThreshold)
                    compliance.put('compareComplianceThreshold', configReader?.compareComplianceThreshold == null ? 80 : configReader?.compareComplianceThreshold)
                    compliance.put('indvScoreComplianceThreshold', configReader?.indvScoreComplianceThreshold == null ? 80 : configReader?.indvScoreComplianceThreshold)
                    compliance.put('slaComplianceThreshold', configReader?.slaComplianceThreshold == null ? 80 : configReader?.slaComplianceThreshold)

                    if(configReader?.excludeTransactionsList != null && configReader?.excludeTransactionsList != '')
                    {
                        configReader.excludeTransactionsList.toString().split(',').each { ignoreTransactionList.add(it) }
                    }


                    if (summaryMap.execSummary.totalScore > (configReader?.totalScoreThreshold == null || configReader?.totalScoreThreshold == '' ? 80 : configReader?.totalScoreThreshold))
                    {
                        compliance.put('totalScoreCompliance', true)
                    } else {
                        compliance.put('totalScoreCompliance', false)
                        compliance.put('totalScoreComplianceReason', 'Total Score :' + summaryMap.execSummary.totalScore + ' less than configured threshold ' + (configReader?.totalScoreThreshold == null ? 80 : configReader?.totalScoreThreshold))
                    }

                    summaryMap.Transactions.each { it ->
                        if (!ignoreTransactionList.count(it.name)) {
                            if (Double.parseDouble(it.deviation.toString()) > (configReader?.devThreshold == null || configReader?.devThreshold =='' ? 5 : configReader?.devThreshold)) {
                                statusList.add(false)
                                errorMsg.add(it.name + ' comparison percentage deviation of ' + it.deviation + '% is greater than allowed threshold of ' + (configReader?.devThreshold == null ? 5 : configReader?.devThreshold) + '%.')
                            } else {
                                statusList.add(true)

                            }
                        }

                    }

                    def compareCompliance = ((statusList.count(true) / statusList.size()) * 100).setScale(0,RoundingMode.UP)
                    if (compareCompliance > (configReader?.compareComplianceThreshold == null || configReader?.compareComplianceThreshold == '' ? 80 : configReader?.compareComplianceThreshold)) {
                        compliance.put('compareComplianceStatus', true)
                        compliance.put('compareCompliancePercentage', compareCompliance)
                    } else {
                        compliance.put('compareComplianceStatus', false)
                        compliance.put('compareCompliancePercentage', compareCompliance)
                        compliance.put('compareComplianceReason', errorMsg.clone())
                    }

                    errorMsg.removeAll { it || !it }
                    statusList.removeAll { it || !it }

                    summaryMap.Transactions.each { it ->
                        if (!ignoreTransactionList.count(it.name)) {
                            if (Double.parseDouble(it.score.toString()) < (configReader?.individualScoreThreshold == null || configReader?.individualScoreThreshold == '' ? 80 : configReader?.individualScoreThreshold)) {
                                statusList.add(false)
                                errorMsg.add(it.name + ' score of ' + it.score + ' is lesser than allowed threshold of ' + (configReader?.individualScoreThreshold == null || configReader?.individualScoreThreshold == '' ? 80 : configReader?.individualScoreThreshold))
                            } else {
                                statusList.add(true)
                            }
                        }
                    }


                    def indvScoreCompliance = ((statusList.count(true) / statusList.size()) * 100).setScale(0,RoundingMode.UP)
                    if (indvScoreCompliance > (configReader?.indvScoreComplianceThreshold == null || configReader?.indvScoreComplianceThreshold == '' ? 80 : configReader?.indvScoreComplianceThreshold)) {
                        compliance.put('indvScoreComplianceStatus', true)
                        compliance.put('indvScoreCompliancePercentage', indvScoreCompliance)
                    } else {
                        compliance.put('indvScoreComplianceStatus', false)
                        compliance.put('indvScoreCompliancePercentage', indvScoreCompliance)
                        compliance.put('indvScoreComplianceReason', errorMsg.clone())
                    }

                    errorMsg.removeAll { it || !it }
                    statusList.removeAll { it || !it }

                    if(configReader?.SLACheck == null || configReader?.SLACheck == 'onload')
                    {
                        summaryMap.Transactions.each { it ->
                            if (!ignoreTransactionList.count(it.name)) {
                                if (Double.parseDouble(it.restime.toString()) > Double.parseDouble(it.sla.toString())) {
                                    statusList.add(false)
                                    errorMsg.add(it.name + ' with response time of ' + it.restime + ' ms exceeds configured SLA of ' + it.sla + ' ms')
                                } else {
                                    statusList.add(true)
                                }
                            }
                        }

                    }
                    else
                    {
                        summaryMap.Transactions.each { it ->
                            if (!ignoreTransactionList.count(it.name)) {
                                if (Double.parseDouble(it.visuallyComplete.toString()) > Double.parseDouble(it.sla.toString())) {
                                    statusList.add(false)
                                    errorMsg.add(it.name + ' with response time of ' + it.visuallyComplete + ' ms exceeds configured SLA of ' + it.sla + ' ms')
                                } else {
                                    statusList.add(true)
                                }
                            }
                        }
                    }


                    def slaCompliance = ((statusList.count(true) / statusList.size()) * 100).setScale(0,RoundingMode.UP)
                    if (slaCompliance > (configReader?.slaComplianceThreshold == null || configReader?.slaComplianceThreshold == '' ? 80 : configReader?.slaComplianceThreshold)) {
                        compliance.put('slaComplianceStatus', true)
                        compliance.put('slaCompliancePercentage', slaCompliance)
                    } else {
                        compliance.put('slaComplianceStatus', false)
                        compliance.put('slaCompliancePercentage', slaCompliance)
                        compliance.put('slaComplianceReason', errorMsg.clone())
                    }

                    summaryMap.put('compliance', compliance)
                    report.setLength(0)
                    report.append(JsonOutput.toJson(summaryMap))
                }

            }
            else
            {
                report.append(analysis[0])
            }
            //
        }
        else
        {
            report.append('{"ClientName": "').append(args.ClientName).append('","ProjectName": "').append(args.ProjectName).append('","Scenario": "').append(args.Scenario).append('","Reason": "No configuration available"}')
        }

        LOGGER.debug('Exiting generateSummaryReport')

        return [report,analysis[1],analysis[2],configFlag]
    }

    static def generateAnalysisReport(def args,def configReader = null)
    {
        LOGGER.debug 'START generateAnalysisReport'

        StringBuilder report = new StringBuilder()
        StringBuilder reportCopy = new StringBuilder()
        def analysisExists
        def analysisReport
        StringBuilder auditLog = new StringBuilder()
        def auditFlag = false
        def tranListLog = []
        boolean analysisStatus = true

        args.put("contentType","json")

        if (configReader == null)
        {
            configReader = CommonUtils.getConfiguration(args)
            LOGGER.debug 'CONFIGURATION DETAILS : ' + args.ClientName + ':' + args.ProjectName + ':' + args.Scenario
            LOGGER.debug configReader
        }

        if (configReader!= null)
        {
            if(args.AnalysisType == 'Time')
            {
                analysisReport = PaceAnalysisEngine.getDetailedAnalysisReport(configReader)
                if (analysisReport.size() > 0)
                {
                    LOGGER.debug 'OUTPUT getDetailedAnalysisReport : ' + analysisReport
                    report.append(PaceReportEngine.getJsonString(configReader, analysisReport))
                    LOGGER.debug 'OUTPUT getJsonString : ' + report
                } else
                {
                    report.append('{"ClientName": "').append(args.ClientName).append('","ProjectName": "').append(args.ProjectName).append('","Scenario": "').append(args.Scenario).append('","BaselineStart": "').append(configReader.BaselineStart).append('","BaselineEnd": "').append(configReader.BaselineEnd).append('","CurrentStart": "').append(configReader.CurrentStart).append('","CurrentEnd": "').append(configReader.CurrentEnd).append('","Reason" : "No data available for the given time period"}')
                    analysisStatus = false
                }
            }

            if(args.AnalysisType == 'Run' || args.AnalysisType == 'Transaction')
            {
                analysisExists = ElasticSearchUtils.analysisReportExists(configReader,configReader.CurrentRun.toString(),configReader.BaselineRun.toString())
                LOGGER.debug 'OUTPUT .analysisReportExists' + analysisExists

                //07/07/2016 - Added logic to auto override analysis for report that doesnt have any data
                if ((args?.override == null || args.override.toBoolean() == false) && analysisExists.hits.hits[0]?."_source"?.TransactionMetrics?.size > 0 && (configReader?.configUpdated == null ||  configReader?.configUpdated == false))
                {
                    LOGGER.debug 'Using Existing Analysis Report'
                    if (args.contentType != 'json')
                    {
                        report.append(PaceReportEngine.getHTMLContent(configReader,analysisExists.hits.hits[0]."_source"))
                    }
                    else
                    {
                        report.append(JsonOutput.toJson(analysisExists.hits.hits[0]."_source"))
                    }
                }
                else
                {
                    analysisReport = PaceAnalysisEngine.getDetailedAnalysisReport(configReader)
                    if(analysisReport.size() > 0)
                    {
                        auditFlag = true
                        LOGGER.debug 'OUTPUT getDetailedAnalysisReport : ' + analysisReport

                        report.append(PaceReportEngine.getJsonString(configReader,analysisReport))
                        LOGGER.debug 'OUTPUT getJsonString : ' +  report

                        def persistAnalysis = ElasticSearchUtils.persistAnalysisReport(configReader,report,configReader.CurrentRun.toString(),configReader.BaselineRun.toString(),(args?.override == null ? false : args.override.toBoolean()))
                        LOGGER.debug(persistAnalysis.toString())

                        if(configReader?.configUpdated == true)
                        {
                            //StringBuilder updateConfig = new StringBuilder()
                            def updateConfig = new JSONObject()
                            updateConfig.put("ClientName",args.ClientName)
                            updateConfig.put("ProjectName",args.ProjectName)
                            updateConfig.put("Scenario",args.Scenario)
                            updateConfig.put("configUpdated",false)

                            PaceAnalysisEngine.updateConfiguration(updateConfig,GlobalConstants.ESUrl)

                        }

                        if (args.contentType != 'json')
                        {
                            reportCopy.append(report)
                            report.setLength(0)
                            report.append(PaceReportEngine.getHTMLContent(configReader,reportCopy.toString()))
                        }
                    }
                    else
                    {
                        report.append('{"ClientName": "').append(args.ClientName).append('","ProjectName": "').append(args.ProjectName).append('","Scenario": "').append(args.Scenario).append('","RunID": "').append(args.CurrentRun).append(' No such RunID available"}')
                        analysisStatus = false
                    }
                }

            }
        }
        else
        {
            report.append('{"ClientName": "').append(args.ClientName).append('","ProjectName": "').append(args.ProjectName).append('","Scenario": "').append(args.Scenario).append('","error": "No configuration available"}')
            analysisStatus = false

        }

        if (args.override.toBoolean() == true || auditFlag == true || args.AnalysisType == 'Time')
        {
            if(analysisStatus)
            {
                def jsonObj = new JSONObject(report.toString())
                def reportMap = JsonUtils.jsonToMap(jsonObj)
                auditLog.append(reportMap.TransactionMetrics.size)
                reportMap.TransactionMetrics.each{it ->
                    tranListLog.add(it.Name + '#' + it.totalPageLoadTime."95 Percentile" + '#' + it.genericRulesRecommendations.size + '#' + it.score)
                }
            }


        }
        LOGGER.debug 'END generateAnalysisReport'
        return [report,auditLog,tranListLog,analysisStatus]
    }


    static def getDetailedAnalysisReport(def configReader)
    {
        LOGGER.debug 'START getDetailedAnalysisReport'
        def txnCalcMetrics
        def comparisonReport
        def crossBrowserReport
        def backendTime = []
        def detailSample
        def detailBreakUp
        def id
        def txnMetrics = [:]
        def rulesOutput = null
        def row = [:]
        LOGGER.debug 'Analysis sample in config :' + configReader?.samplePercentile
		def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)
		LOGGER.debug 'Analysis will be done for ' + comparisonPerc + ' sample'
        txnCalcMetrics = ElasticSearchUtils.getAggregatedResponseTime(configReader,configReader.CurrentRun.toString(),null,'Current')
        LOGGER.debug 'OUTPUT getAggregatedResponseTime :' + txnCalcMetrics

        comparisonReport = PaceAnalysisEngine.getComparisonReport(configReader,null,txnCalcMetrics)
        LOGGER.debug 'OUTPUT getComparisonReport :' + comparisonReport
        txnCalcMetrics.each {key,value ->
            //detailBreakUp = (ElasticSearchUtils.getSampleForDetailedAnalysis(configReader, key, (value."$comparisonPerc").toString(),configReader.CurrentRun.toString(),'Current')).hits.hits[0].'_source'
 			detailSample = (ElasticSearchUtils.getSampleForDetailedAnalysis(configReader, key, (value."$comparisonPerc").toString(),configReader.CurrentRun.toString(),'Current')).hits.hits[0]
            id = detailSample.'_id'
            detailBreakUp = detailSample.'_source'
            LOGGER.debug 'OUTPUT getSampleForDetailedAnalysis :' + detailBreakUp
            if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
            {
                rulesOutput = PaceRuleEngine.applyNativeAppRules(detailBreakUp, configReader)
                LOGGER.debug 'OUTPUT applyNativeAppRules :' + rulesOutput
                row.put('NavType','NativeApp')
                row.put('Platform',detailBreakUp.Platform)
                row.put('UserAgent','MobileNativeApp')
                row.put('Average',value.Average)
                row.put('Pcnt90',value.Pcnt90)
                row.put('Pcnt95',value.Pcnt95)
                row.put('Count',value.Count)
                row.put('ScreenName',detailBreakUp.ScreenName)
                row.put('userPerceivedTime',detailBreakUp.userPerceivedTime)
                row.put('resourceLoadTime',(detailBreakUp?.resourceLoadTime == null ? 0 :  Double.parseDouble(detailBreakUp.resourceLoadTime.toString()).round()))
                row.put('totalRequest',detailBreakUp.totalRequest)
                row.put('totalSize',detailBreakUp.totalSize)
                row.put('score',rulesOutput[0])
                row.put('recommendation',rulesOutput[1])
                backendTime = PaceRuleEngine.calculateBackendTime(configReader,detailBreakUp.totalPageLoadTime,detailBreakUp.Resources)
                row.put('backendAnalysis',backendTime[0])
                row.put('previous',comparisonReport."$key".previous)
                row.put('current',comparisonReport."$key".current)
                row.put('deviation',comparisonReport."$key".deviation)
                row.put('comparativeAnalysis',comparisonReport."$key".comparativeAnalysis)
            }
            else
            {
                if (detailBreakUp?.NavType == null || detailBreakUp?.NavType == 'Hard' || detailBreakUp?.NavType == 'hard' || detailBreakUp?.NavType == '')
                {
                    rulesOutput = PaceRuleEngine.applyGenericRules(detailBreakUp, configReader)
                    LOGGER.debug 'OUTPUT applyGenericRules :' + rulesOutput
                    row.put('NavType','Hard')
                    row.put('Platform',detailBreakUp.Platform)
                    row.put('UserAgent',detailBreakUp.UserAgent)
                    row.put('BrowserName',detailBreakUp?.BrowserName)
                    row.put('DeviceType',detailBreakUp?.DeviceType)
                    row.put('Average',value.Average)
                    row.put('Pcnt90',value.Pcnt90)
                    row.put('Pcnt95',value.Pcnt95)
                    row.put('Max',Double.parseDouble(value.Max.toString()).round())
                    row.put('Min',Double.parseDouble(value.Min.toString()).round())
                    row.put('Count',value.Count)
                    row.put('fetchStartTime',detailBreakUp.fetchStartTime)
                    row.put('redirectTime',detailBreakUp.redirectTime)
                    row.put('cacheFetchTime',detailBreakUp.cacheFetchTime)
                    row.put('dnsLookupTime',detailBreakUp.dnsLookupTime)
                    row.put('tcpConnectTime',detailBreakUp.tcpConnectTime)
                    row.put('serverTime',detailBreakUp.serverTime_ttfb)
                    row.put('downloadTime',detailBreakUp.downloadTime)
                    row.put('domProcessingTime',detailBreakUp.domProcessingTime)
                    row.put('onloadTime',detailBreakUp.onloadTime)
                    row.put('clientTime',detailBreakUp.clientTime)
                    row.put('resourceLoadTime',(detailBreakUp?.resourceLoadTime == null ? 0 :  Double.parseDouble(detailBreakUp.resourceLoadTime.toString()).round()))
                    row.put('score',rulesOutput[0])
                    row.put('recommendation',rulesOutput[1])
                    backendTime = PaceRuleEngine.calculateBackendTime(configReader,detailBreakUp.totalPageLoadTime,detailBreakUp.Resources)
                    row.put('backendAnalysis',backendTime[0])
                    //row.put('resourceBlockTime',Double.parseDouble(PaceRuleEngine.calculateBlockingTime(detailBreakUp.Resources).toString()).round())
                    row.put('resourceBlockTime',backendTime[1].round())
                    row.put('speedIndex',(detailBreakUp?.speedIndex == null ? 0 : Double.parseDouble(detailBreakUp.speedIndex.toString()).round()))
                    row.put('previous',comparisonReport."$key".previous)
                    row.put('current',comparisonReport."$key".current)
                    row.put('deviation',comparisonReport."$key".deviation)
                    row.put('previousHC',comparisonReport."$key".pHTTPCalls)
                    row.put('currentHC',comparisonReport."$key".cHTTPCalls)
                    row.put('devHC',comparisonReport."$key".devHTTPCalls)
                    row.put('previousPayload',comparisonReport."$key".pPayload)
                    row.put('currentPayload',comparisonReport."$key".cPayload)
                    row.put('devPayload',comparisonReport."$key".devPayload)
                    row.put('comparativeAnalysis',comparisonReport."$key".comparativeAnalysis)
                    row.put('crossBrowserAnalysis','')
                    row.put('url',detailBreakUp.url)
                    row.put('ttfbUser',(detailBreakUp?.ttfbUser == null ? 0 : detailBreakUp.ttfbUser))
                    row.put('ttfbBrowser',(detailBreakUp?.ttfbBrowser == null ? 0 : detailBreakUp.ttfbBrowser))
                    row.put('ttfpUser',(detailBreakUp?.ttfpUser == null ? 0 : detailBreakUp.ttfpUser))
                    row.put('ttfpBrowser',(detailBreakUp?.ttfpBrowser == null ? 0 : detailBreakUp.ttfpBrowser))
                    row.put('clientProcessing',(detailBreakUp?.clientProcessing == null ? 0 : detailBreakUp.clientProcessing))
                    row.put('visuallyComplete',(detailBreakUp?.visuallyComplete == null ? 0 : detailBreakUp.visuallyComplete))
                    row.put('domInteractive',(detailBreakUp?.renderingTime == null ? 0 : detailBreakUp.renderingTime))
                    row.put('resourceCount',detailBreakUp.Resources.size)
                    row.put('resourceSize',detailBreakUp?.resourceSize == null ? 0 : detailBreakUp?.resourceSize)
                    if(configReader?.isMarkAPIEnabled != null && configReader?.isMarkAPIEnabled)
                    {
                        def markSample = ElasticSearchUtils.extractMarkDetailsUsingID(configReader,id)
                        if(markSample?.hits?.hits != null && markSample?.hits?.hits.size() > 0)
                        {
                            def markDetails = markSample.hits.hits[0].'_source'
                            def markMap = [:]
                            markDetails.each {k,v ->
                                markMap.put(k,Double.parseDouble(v.toString()).round())
                            }
                            row.put('markEvents',markMap)
                        }
                    }
                }
                else
                {
                    rulesOutput = PaceRuleEngine.applySoftNavigationRules(detailBreakUp, configReader)
                    LOGGER.debug 'OUTPUT applySoftNavigationRules :' + rulesOutput
                    row.put('NavType','Soft')
                    row.put('Platform',detailBreakUp.Platform)
                    row.put('UserAgent',detailBreakUp.UserAgent)
                    row.put('BrowserName',detailBreakUp?.BrowserName)
                    row.put('DeviceType',detailBreakUp?.DeviceType)
                    row.put('Average',value.Average)
                    row.put('Pcnt90',value.Pcnt90)
                    row.put('Pcnt95',value.Pcnt95)
                    row.put('Max',Double.parseDouble(value.Max.toString()).round())
                    row.put('Min',Double.parseDouble(value.Min.toString()).round())
                    row.put('Count',value.Count)
                    row.put('resourceLoadTime',(detailBreakUp?.resourceLoadTime == null ? 0 :  Double.parseDouble(detailBreakUp.resourceLoadTime.toString()).round()))
                    row.put('score',rulesOutput[0])
                    row.put('recommendation',rulesOutput[1])
                    backendTime = PaceRuleEngine.calculateBackendTime(configReader,detailBreakUp.totalPageLoadTime,detailBreakUp.Resources)
                    row.put('backendAnalysis',backendTime[0])
                    //row.put('resourceBlockTime',Double.parseDouble(PaceRuleEngine.calculateBlockingTime(detailBreakUp.Resources).toString()).round())
                    row.put('resourceBlockTime',backendTime[1].round())
                    row.put('previous',comparisonReport."$key".previous)
                    row.put('current',comparisonReport."$key".current)
                    row.put('deviation',comparisonReport."$key".deviation)
                    row.put('previousHC',comparisonReport."$key".pHTTPCalls)
                    row.put('currentHC',comparisonReport."$key".cHTTPCalls)
                    row.put('devHC',comparisonReport."$key".devHTTPCalls)
                    row.put('previousPayload',comparisonReport."$key".pPayload)
                    row.put('currentPayload',comparisonReport."$key".cPayload)
                    row.put('devPayload',comparisonReport."$key".devPayload)
                    row.put('comparativeAnalysis',comparisonReport."$key".comparativeAnalysis)
                    row.put('crossBrowserAnalysis','')
                    row.put('url',detailBreakUp.url)
                    //row.put('resourceBlockTime',Double.parseDouble(PaceRuleEngine.calculateBlockingTime(detailBreakUp.Resources).toString()).round())
                    row.put('visuallyComplete',(detailBreakUp?.visuallyComplete == null ? 0 : detailBreakUp.visuallyComplete))
                    row.put('resourceCount',detailBreakUp.Resources.size)
                    row.put('resourceSize',detailBreakUp?.resourceSize == null ? 0 : detailBreakUp?.resourceSize)
                    row.put('clientProcessing',(value."$comparisonPerc" - (detailBreakUp?.resourceLoadTime == null ? 0 :  Double.parseDouble(detailBreakUp.resourceLoadTime.toString()).round())))
                    if(configReader?.isMarkAPIEnabled != null && configReader?.isMarkAPIEnabled)
                    {
                        def markSample = ElasticSearchUtils.extractMarkDetailsUsingID(configReader,id)
                        if(markSample?.hits?.hits != null && markSample?.hits?.hits.size() > 0)
                        {
                            def markDetails = markSample.hits.hits[0].'_source'
                            def markMap = [:]
                            markDetails.each {k,v ->
                                markMap.put(k,Double.parseDouble(v.toString()).round())
                            }
                            row.put('markEvents',markMap)
                        }
                    }
                }
            }
            txnMetrics.put(key,row.clone())

        }

        LOGGER.debug 'OUTPUT txnMetrics MAP :' + txnMetrics
        LOGGER.debug 'END getDetailedAnalysisReport'
        return txnMetrics
    }


    static def getComparisonReport(def configReader,def analysisFlag = null,def currRunMetrics = null)
    {
        LOGGER.debug 'START getComparisonReport'
        def comparisonList = [:]
        if (configReader!= null)
        {
            if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Time')
            {
                comparisonList = PaceAnalysisEngine.comparisonReport(configReader,currRunMetrics)
            }

            if(configReader.AnalysisType == 'Transaction')
            {
                comparisonList = PaceAnalysisEngine.compareAgainstBaseline(configReader,currRunMetrics)
            }

            LOGGER.debug 'OUTPUT comparisonReport : ' + comparisonList

            if(analysisFlag == null)
            {
                comparisonList =  PaceAnalysisEngine.analyzeTransactionMetrics(configReader,comparisonList)
                LOGGER.debug 'OUTPUT analyzeTransactionMetrics: ' + comparisonList
            }
            else
            {
                comparisonList =  PaceAnalysisEngine.analyzeTransactionMetrics(configReader,comparisonList,analysisFlag)
                LOGGER.debug 'OUTPUT analyzeTransactionMetrics : ' + comparisonList
            }
            LOGGER.debug 'END getComparisonReport'
        }
        return comparisonList

    }


    static def comparisonReport(def configReader,def currRunMetrics)
    {
        LOGGER.debug 'START comparisonReport'
        int previous,current
        double percentage
        def comparisonList = [:]
        def row = [:]
        def prevRunMetrics
        def isCurrentRunBetter = []
        try
        {
            prevRunMetrics = ElasticSearchUtils.getAggregatedResponseTime(configReader,configReader.BaselineRun.toString(),null,'Baseline')
            LOGGER.debug 'OUTPUT getAggregatedResponseTime for PreviousMetrics : ' + prevRunMetrics
            def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)

            currRunMetrics.each {key,value ->
                current = value."$comparisonPerc"
                previous = (prevRunMetrics[key]?."$comparisonPerc" == null ? 0 : prevRunMetrics[key]."$comparisonPerc")
                if(previous == 0)
                {
                    row.put('pRT',0)
                    row.put('cRT',String.valueOf(current))
                    row.put('devRT',0)
                }
                else
                {
                    percentage = Math.round(((current - previous)/(previous == 0 ? 1 : previous)) * 10000.00)/100.00
                    row.put('pRT',String.valueOf(previous))
                    row.put('cRT',String.valueOf(current))
                    row.put('devRT',String.valueOf(percentage))
                }


                current = value.HttpCount
                previous = (prevRunMetrics[key]?.HttpCount == null ? 0 : prevRunMetrics[key].HttpCount)
                percentage = Math.round(((current - previous)/(previous == 0 ? 1 : previous)) * 10000.00)/100.00
                row.put('cHTTPCalls',String.valueOf(current))
                row.put('pHTTPCalls',String.valueOf(previous))
                row.put('devHTTPCalls',String.valueOf(percentage))

                current = value.Payload
                previous = (prevRunMetrics[key]?.Payload == null ? 0 : prevRunMetrics[key].Payload)
                percentage = Math.round(((current - previous)/(previous == 0 ? 1 : previous)) * 10000.00)/100.00

                row.put('cPayload',String.valueOf(current))
                row.put('pPayload',String.valueOf(previous))
                row.put('devPayload',String.valueOf(percentage))

                comparisonList.put(key,row.clone())

                if(configReader.AnalysisType == 'Run')
                {
                    if (percentage < 0 && current > 0)
                    {
                        isCurrentRunBetter.add('Y')
                    }
                    else
                    {
                        isCurrentRunBetter.add('N')
                    }
                }
            }

            if(configReader.AnalysisType == 'Run')
            {
                if ((isCurrentRunBetter.find { it == 'N' }) == 'N' || currRunMetrics.size() <= 0) {
                    //Do Nothing
                } else {
                    //We just need to set the baseline runID in the config file so no update to all documents is required.
                    ElasticSearchUtils.updateBaselineRunID(configReader, configReader.CurrentRun.toString())
                }
            }

        }
        catch (Exception e)
        {
            LOGGER.error 'Exception in comparisonReport' + e.getStackTrace()
        }

        LOGGER.debug 'END comparisonReport'
        return comparisonList
    }

    static def compareAgainstBaseline(def configReader,def currentRunMetrics = null)
    {
        LOGGER.debug 'START compareAgainstBaseline'
        //def currentRunMetrics = [:]
        def baselineRunMetrics  = [:]
        int previous,current
        double percentage
        def comparisonList = [:]

        def row = [:]
        def oldBaseline = [:]
        def txnMap = [:]
        def txnListArray = []
        def txnArray = []
        def baselineUpdate = false
        //StringBuilder jsonString = new StringBuilder()
        try
        {

            LOGGER.debug 'OUTPUT processRunMetricsFromES for CurrentMetrics : ' + currentRunMetrics

            baselineRunMetrics =  PaceAnalysisEngine.baselineRunMetrics(configReader)
            LOGGER.debug 'OUTPUT baselineRunMetrics for baselineRunMetrics : ' + baselineRunMetrics

            oldBaseline = baselineRunMetrics[1]
            LOGGER.debug 'OUTPUT baselineRunMetrics for previous transactions : ' + oldBaseline

            def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)

            if(currentRunMetrics.size() > 0)
            {
                currentRunMetrics.each {key,value ->
                    current = value."$comparisonPerc"
                    previous = (baselineRunMetrics[0].get(key) == null ? 0 : baselineRunMetrics[0].get(key))
                    if(previous == 0)
                    {
                        row.put('previous',0)
                        row.put('current',String.valueOf(current))
                        row.put('deviation',0)
                        //row.append(key).append('##0##').append(String.valueOf(current)).append('##0')
                        //update baseline with current transaction data
                        txnMap.put("Name",key)
                        txnMap.put("DocId",ElasticSearchUtils.getSampleForDetailedAnalysis(configReader,key,current.toString(),configReader.CurrentRun.toString(),'Current').hits.hits[0]."_id")
                        txnMap.put("RunID",configReader.CurrentRun)
                        txnMap.put("Value",current)
                        txnListArray.add(txnMap.clone())
                        txnArray.add(key)
                        baselineUpdate = true
                    }
                    else
                    {
                        percentage = Math.round(((current - previous)/(previous == 0 ? 1 : previous)) * 10000.00)/100.00
                        row.put('previous',String.valueOf(previous))
                        row.put('current',String.valueOf(current))
                        row.put('deviation',String.valueOf(percentage))
                        if (percentage < 0)
                        {
                            //update baseline with current transaction data
                            txnMap.put("Name",key)
                            txnMap.put("DocId",ElasticSearchUtils.getSampleForDetailedAnalysis(configReader,key,current.toString(),configReader.CurrentRun.toString(),'Current').hits.hits[0]."_id")
                            txnMap.put("RunID",configReader.CurrentRun)
                            txnMap.put("Value",current)
                            txnListArray.add(txnMap.clone())
                            txnArray.add(key)
                            baselineUpdate = true
                        }

                        //row.append(key).append('##').append(String.valueOf(previous)).append('##').append(String.valueOf(current)).append('##').append(String.valueOf(percentage))
                    }

                    comparisonList.put(key,row.clone())
                }
            }


            def mapCnt = -1
            txnArray.each {listValue ->
                mapCnt = oldBaseline.get("Transactions").findIndexOf { it -> it.get('Name') == listValue }
                if(mapCnt >= 0)
                {
                    oldBaseline.get("Transactions").remove(mapCnt)
                }
            }
            oldBaseline.put("Transactions",oldBaseline.get("Transactions") + txnListArray)

            if(baselineUpdate)
            {
                ElasticSearchUtils.updateBaseline(configReader,oldBaseline)
            }

        }
        catch (Exception e)
        {
            e.printStackTrace()
        }

        LOGGER.debug 'END compareAgainstBaseline'
        return comparisonList
    }


    static def baselineRunMetrics(def configReader)
    {
        LOGGER.debug 'START baselineRunMetrics'
        //def esIndexStatus = ElasticSearchUtils.isESIndexExists(configReader.esUrl,GlobalConstants.BASELINESEARCH)
        def baselineTxnResponseTime = [:]
        def baselineMap =[:]
        def baselineExists
        def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)

        //if (esIndexStatus == 200)
        //{
        baselineExists = ElasticSearchUtils.baselineReportExists(configReader,comparisonPerc)
        if (baselineExists.hits.hits.size <= 0)
        {
            StringBuilder emptyDoc = new StringBuilder()
            emptyDoc.append('{"ClientName":"').append(configReader.ClientName).append('","ProjectName":"').append(configReader.ProjectName).append('","Scenario":"').append(configReader.Scenario).append('","Percentile":"').append(comparisonPerc.toLowerCase()).append('","Transactions":[]}')
            ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.BASELINETABLE,emptyDoc)
            ElasticSearchUtils.refreshESIndex(configReader.esUrl,GlobalConstants.BASELINEINDEX)
            baselineExists = ElasticSearchUtils.baselineReportExists(configReader,comparisonPerc)
            baselineMap =  baselineExists.hits.hits[0]."_source"
        }
        else
        {
            baselineMap =  baselineExists.hits.hits[0]."_source"
        }

        baselineMap.get("Transactions").each{it -> baselineTxnResponseTime.put(it.get('Name'),it.get("Value"))}
        LOGGER.debug 'baselineTxnResponseTime : ' + baselineTxnResponseTime

        //}

        LOGGER.debug 'END baselineRunMetrics'
        return [baselineTxnResponseTime,baselineMap]
    }

    static def getDegradedTransactions(def tableList,def configReader)
    {
        LOGGER.debug 'START getDegradedTransactions'
        def txnDegraded = false
        float dev = Float.valueOf(tableList.devRT.toString().trim()).floatValue();
        def deviPcnt = (configReader.devThreshold == null ? 5 : Float.parseFloat(configReader.devThreshold.toString()))
        if(dev > deviPcnt)
        {
            txnDegraded = true
        }

        LOGGER.debug 'Degraded Transactions : ' + txnDegraded
        LOGGER.debug 'END getDegradedTransactions'
        return txnDegraded
    }

    static def analyzeTransactionMetrics(def configReader,def txnList,def analysisFlag = null)
    {
        LOGGER.debug 'START analyzeTransactionMetrics'
        def observation = [:]
        def row = [:]
        def baseline = [:]
        def comparisonPerc
        def baselineExists
        if (analysisFlag == null)
        {
            //Sequential version
            txnList.each{it ->
                if(PaceAnalysisEngine.getDegradedTransactions(it.value,configReader))
                {
                    if(configReader.AnalysisType == 'Transaction')
                    {
                        comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)
                        baselineExists = ElasticSearchUtils.baselineReportExists(configReader,comparisonPerc)
                        baseline = baselineExists.hits.hits[0]."_source".Transactions[baselineExists.hits.hits[0]."_source".Transactions.findIndexOf {innit -> innit.get('Name') == it.key }]
                        row.put('comparativeAnalysis',PaceAnalysisEngine.analyzeTransactionMetricsIndividual(configReader,it.key,it.value,baseline))
                    }
                    else
                    {
                        row.put('comparativeAnalysis',PaceAnalysisEngine.analyzeTransactionMetricsIndividual(configReader,it.key,it.value))
                    }
                    row.put('previous',it.value.pRT)
                    row.put('current',it.value.cRT)
                    row.put('deviation',it.value.devRT)
                    row.put('pHTTPCalls',it.value.pHTTPCalls)
                    row.put('cHTTPCalls',it.value.cHTTPCalls)
                    row.put('devHTTPCalls',it.value.devHTTPCalls)
                    row.put('pPayload',it.value.pPayload)
                    row.put('cPayload',it.value.cPayload)
                    row.put('devPayload',it.value.devPayload)

                }
                else
                {
                    row.put('previous',it.value.pRT)
                    row.put('current',it.value.cRT)
                    row.put('deviation',it.value.devRT)
                    row.put('pHTTPCalls',it.value.pHTTPCalls)
                    row.put('cHTTPCalls',it.value.cHTTPCalls)
                    row.put('devHTTPCalls',it.value.devHTTPCalls)
                    row.put('pPayload',it.value.pPayload)
                    row.put('cPayload',it.value.cPayload)
                    row.put('devPayload',it.value.devPayload)
                    row.put('comparativeAnalysis','No Degradation')
                }
                observation.put(it.key,row.clone())

            }
        }
        else
        {
            txnList.each{it ->
                if(PaceAnalysisEngine.getDegradedTransactions(it.value,configReader))
                {
                    row.put('previous',it.value.pRT)
                    row.put('current',it.value.cRT)
                    row.put('deviation',it.value.devRT)
                    row.put('pHTTPCalls',it.value.pHTTPCalls)
                    row.put('cHTTPCalls',it.value.cHTTPCalls)
                    row.put('devHTTPCalls',it.value.devHTTPCalls)
                    row.put('pPayload',it.value.pPayload)
                    row.put('cPayload',it.value.cPayload)
                    row.put('devPayload',it.value.devPayload)
                }
                else
                {
                    row.put('previous',it.value.pRT)
                    row.put('current',it.value.cRT)
                    row.put('deviation',it.value.devRT)
                    row.put('pHTTPCalls',it.value.pHTTPCalls)
                    row.put('cHTTPCalls',it.value.cHTTPCalls)
                    row.put('devHTTPCalls',it.value.devHTTPCalls)
                    row.put('pPayload',it.value.pPayload)
                    row.put('cPayload',it.value.cPayload)
                    row.put('devPayload',it.value.devPayload)
                }
                observation.put(it.key,row.clone())

            }
        }
        LOGGER.debug 'END analyzeTransactionMetrics'
        return observation

    }

    static def analyzeTransactionMetricsIndividual(def configReader,def txnName,def txnData,def baseline = null)
    {
        LOGGER.debug 'START analyzeTransactionMetricsIndividual'
        StringBuilder compAnalysis = new StringBuilder()
        def prevTxnMetrics
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Time')
        {
            prevTxnMetrics = ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader,txnName,configReader.BaselineRun.toString(),'Baseline',txnData.pRT)
        }
        else
        {
            prevTxnMetrics = ElasticSearchUtils.extractSampleForDetailedAnalysisUsingID(configReader,baseline.get("DocId"))
        }

        LOGGER.debug 'OUTPUT extractSampleForDetailedAnalysis for Previous Run : '  + prevTxnMetrics
        def currTxnMetrics = ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader,txnName,configReader.CurrentRun.toString(),'Current',txnData.cRT)
        LOGGER.debug 'OUTPUT extractSampleForDetailedAnalysis for Current Run : '  + currTxnMetrics

        if (currTxnMetrics?.hits?.hits[0]?._source != null && prevTxnMetrics?.hits?.hits[0]?._source != null)
        {
            if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
            {
                compAnalysis.append(PaceRuleEngine.analyzeNativeApp(currTxnMetrics,prevTxnMetrics))
            }
            else
            {
                if(currTxnMetrics.hits.hits[0]._source?.NavType == null || currTxnMetrics.hits.hits[0]._source?.NavType == 'Hard' || currTxnMetrics.hits.hits[0]._source?.NavType == '')
                {
                    compAnalysis.append(PaceRuleEngine.analyzeServerTime(currTxnMetrics,prevTxnMetrics))

                    compAnalysis.append(PaceRuleEngine.analyzeNetworkTime(currTxnMetrics,prevTxnMetrics))

                    compAnalysis.append(PaceRuleEngine.analyzeDOMProcessingTime(currTxnMetrics,prevTxnMetrics))

                    compAnalysis.append(PaceRuleEngine.analyzeRenderingTime(currTxnMetrics,prevTxnMetrics))
                }
                else
                {
                    compAnalysis.append(PaceRuleEngine.analyzeResources(currTxnMetrics,prevTxnMetrics))
                }
            }

        }
        else
        {
            compAnalysis.append('<ul>Data not available for one or both run</ul>')
        }
        LOGGER.debug 'END analyzeTransactionMetricsIndividual'
        return compAnalysis
    }

    //String clientName,String projectName,String scenarioName,String esURL,String txnName,String currSampleValue,String blSampleValue,String currRunID,String blRunID
    static List<Object> compareResourceMetrics(def args)
    {
        LOGGER.debug 'START compareResourceMetrics'
        def prevTxnMetrics = [:]
        def prev = [:]
        def curr = [:]
        def prevbrowser = null
        def currbrowser = null
        def currTxnMetrics = [:]
        def configReader = CommonUtils.getConfiguration(args)
        LOGGER.debug 'CONFIGURATION DETAILS : ' + args.ClientName + ':' + args.ProjectName + ':' + args.ScenarioName
        LOGGER.debug configReader.toString()
        boolean configFlag = false

        if (configReader!= null)
        {
            configFlag = true
            if(args.AnalysisType == 'Transaction')
            {
                def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)
                def baselineExists = ElasticSearchUtils.baselineReportExists(configReader,comparisonPerc)
                def baseline = baselineExists.hits.hits[0]."_source".Transactions[baselineExists.hits.hits[0]."_source".Transactions.findIndexOf { it -> it.get('Name') == args.txnName }]
                prev =  ElasticSearchUtils.extractSampleForDetailedAnalysisUsingID(configReader,baseline.DocId)
                prevTxnMetrics = prev.hits.hits[0]."_source".Resources
                prevbrowser = prev.hits.hits[0]."_source".UserAgent
            }
            else
            {
                prev = ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader, args.txnName, configReader.BaselineRun.toString(), 'Baseline', args.baselineSample)
                prevTxnMetrics = prev?.hits?.hits[0]?."_source"?.Resources
                prevbrowser = prev.hits.hits[0]."_source".UserAgent
            }
            curr = ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader,args.txnName,configReader.CurrentRun.toString(),'Current',args.currentSample)
            currTxnMetrics = curr?.hits?.hits[0]?."_source"?.Resources
            currbrowser = curr.hits.hits[0]."_source".UserAgent
        }

        def compJson = '{"duration":' + PaceAnalysisEngine.compareDuration(prevTxnMetrics,currTxnMetrics,configReader) + ',"size":' + PaceAnalysisEngine.compareSize(prevTxnMetrics,currTxnMetrics,configReader,prevbrowser,currbrowser) + ',"calls":' + PaceAnalysisEngine.compareResourceTypeCalls(prevTxnMetrics,currTxnMetrics,configReader) + ',"sizeByType":' + PaceAnalysisEngine.compareResourceTypeSize(prevTxnMetrics,currTxnMetrics,configReader,prevbrowser,currbrowser) + '}'
        LOGGER.debug 'Comparison Json : ' + compJson
        LOGGER.debug 'END compareResourceMetrics'
        return [configFlag,compJson]
    }

    static def compareResourceTypeCalls(def prevTxnMetrics,def currTxnMetrics,def configReader)
    {
        LOGGER.debug 'START compareResourceTypeCalls'
        def prevList = []
        def currList = []

        if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
        {
            prevTxnMetrics.each{it -> prevList.add(it.ResourceType)}
            currTxnMetrics.each{it -> currList.add(it.ResourceType)}
        }
        else
        {
            prevTxnMetrics.each{it -> prevList.add(it.ResourceType)}
            currTxnMetrics.each{it -> currList.add(it.ResourceType)}
        }

        def prevByType = prevList.countBy {it}
        def currByType = currList.countBy {it}

        def mapJoin = currByType.collect{k, v ->
            [k, v ,prevByType.find {a, b -> a == k}?.value]
        }

        prevByType.collect{k, v ->
            if(!currByType.find {a, b -> a == k})
            {
                mapJoin.add([k,currByType.find {a, b -> a == k}?.value,v])
            }

        }

        def builder = new groovy.json.JsonBuilder()

        builder mapJoin,{def it ->
            name it[0]
            curr (it[1] != null ? it[1] : 0)
            prev (it[2] != null ? it[2] : 0)
        }
        LOGGER.debug 'END compareResourceTypeCalls'
        return builder.toString()
    }

    static def compareResourceTypeSize(def prevTxnMetrics,def currTxnMetrics,def configReader,def prevBrowser,def curBrowser)
    {
        LOGGER.debug 'START compareResourceTypeSize'
        def prevMap = [:]
        def prevList = []
        def currMap = [:]
        def currList = []

        if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
        {
            prevTxnMetrics.each{it -> prevMap.put(it.ResourceType,it.transferSize)}
            currTxnMetrics.each{it -> currMap.put(it.ResourceType,it.transferSize)}
        }
        else
        {
            if(prevBrowser.contains('Trident'))
            {
                prevTxnMetrics.each{it ->
                    prevMap.put('Type',it.ResourceType)
                    prevMap.put('Size',it."Content-Length")
                    prevList.add(prevMap.clone())
                }
            }
            else
            {
                prevTxnMetrics.each{it ->
                    prevMap.put('Type',it.ResourceType)
                    prevMap.put('Size',it.transferSize)
                    prevList.add(prevMap.clone())
                }
            }
            if(curBrowser.contains('Trident'))
            {
                currTxnMetrics.each{it ->
                    currMap.put('Type',it.ResourceType)
                    currMap.put('Size',it."Content-Length")
                    currList.add(currMap.clone())
                }
            }
            else
            {
                currTxnMetrics.each{it ->
                    currMap.put('Type',it.ResourceType)
                    currMap.put('Size',it.transferSize)
                    currList.add(currMap.clone())
                }
            }


        }

        def prevByType = prevList.groupBy{it.Type}.collect{['Type':it.key, 'Size':it.value.sum{it.Size}]}
        def currByType = currList.groupBy{it.Type}.collect{['Type':it.key, 'Size':it.value.sum{it.Size}]}

        prevMap.clear()
        currMap.clear()

        prevByType.each{it-> prevMap.put(it.Type,it.Size)}
        currByType.each{it-> currMap.put(it.Type,it.Size)}

        def mapJoin = currMap.collect{k, v ->
            [k, v ,prevMap.find {a, b -> a == k}?.value]
        }

        prevMap.collect{k, v ->
            if(!currMap.find {a, b -> a == k})
            {
                mapJoin.add([k,currMap.find {a, b -> a == k}?.value,v])
            }

        }

        def builder = new groovy.json.JsonBuilder()

        builder mapJoin,{def it ->
            name it[0]
            curr (it[1] != null ? it[1] : 0)
            prev (it[2] != null ? it[2] : 0)
        }
        LOGGER.debug 'END compareResourceTypeSize'
        return builder.toString()
    }

    static def compareDuration(def prevTxnMetrics,def currTxnMetrics,def configReader)
    {
        LOGGER.debug 'START compareDuration'
        def prevMap = [:]
        def currMap = [:]

        if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
        {
            prevTxnMetrics.each{it -> prevMap.put(it.name,it.totalResourceTime)}
            currTxnMetrics.each{it -> currMap.put(it.name,it.totalResourceTime)}
        }
        else
        {
            prevTxnMetrics.each{it -> prevMap.put(it.name,it.duration)}
            currTxnMetrics.each{it -> currMap.put(it.name,it.duration)}
        }

        def mapJoin = currMap.collect{k, v ->
            [k, v ,prevMap.find {a, b -> a == k}?.value]
        }

        prevMap.collect{k, v ->
            if(!currMap.find {a, b -> a == k})
            {
                mapJoin.add([k,currMap.find {a, b -> a == k}?.value,v])
            }

        }

        def builder = new groovy.json.JsonBuilder()
        float perc = 0

        builder mapJoin,{def it ->
            name it[0]
            curr (it[1] != null ? it[1].toDouble().round() : null)
            prev (it[2] != null ? it[2].toDouble().round() : null)
            if (it[1] != null && it[2] != null)
            {
                perc = (((it[1]-it[2])/(it[2] == 0 ? 1 : it[2]) * 100))
                percnt perc.round(2)
                if(perc < 0)
                {
                    status 'Down'
                }
                else
                {
                    if(perc == 0)
                    {
                        status 'No Change'
                    }
                    else
                    {
                        status 'Up'
                    }
                }
            }
            else
            {
                percnt ''
                if(it[1] != null)
                {
                    status 'New'
                }
                else
                {
                    status 'Removed'
                }
            }

        }
        LOGGER.debug 'END compareDuration'
        return builder.toString()
    }

    static def compareSize(def prevTxnMetrics,def  currTxnMetrics,def configReader,def prevBrowser,def curBrowser)
    {
        LOGGER.debug 'START compareSize'
        def prevMap = [:]
        def currMap = [:]

        if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
        {
            prevTxnMetrics.each { it ->prevMap.put(it.name,(it?."Content-Length" == null ? null : it?."Content-Length"))}
            currTxnMetrics.each{ it ->currMap.put(it.name,(it?."Content-Length" == null ? null : it?."Content-Length"))}
        }
        else
        {
            prevTxnMetrics.each { it ->

                if(prevBrowser.contains('Trident'))
                {
                    if (CommonUtils.stringContainsItemFromList(it.name,configReader.isStaticResources))
                    {
                        prevMap.put(it.name,(it?."Content-Length" == null ? null : Long.parseLong(it?."Content-Length".toString())))
                    }

                }
                else
                {
                    prevMap.put(it.name,(it?."transferSize" == null ? null : Long.parseLong(it?."transferSize".toString())))
                }

            }
            currTxnMetrics.each{ it ->
                if(curBrowser.contains('Trident'))
                {
                    if (CommonUtils.stringContainsItemFromList(it.name, configReader.isStaticResources)) {
                        currMap.put(it.name, (it?."Content-Length" == null ? null : Long.parseLong(it?."Content-Length".toString())))
                    }
                }
                else
                {
                    currMap.put(it.name,(it?."transferSize" == null ? null : Long.parseLong(it?."transferSize".toString())))
                }
            }
        }

        def mapJoin = currMap.collect{k, v ->
            [k, v ,prevMap.find {a, b -> a == k}?.value]
        }

        prevMap.collect{k, v ->
            if(!currMap.find {a, b -> a == k})
            {
                mapJoin.add([k,currMap.find {a, b -> a == k}?.value,v])
            }

        }

        def builder = new groovy.json.JsonBuilder()
        float perc = 0

        builder mapJoin,{def it ->
            name it[0]
            curr (it[1] != null ? it[1] : null)
            prev (it[2] != null ? it[2] : null)

            if (it[1] && it[2])
            {
                perc = (((it[1]-it[2])/(it[2] == 0 ? 1 : it[2])) * 100)
                percnt perc.round(2)
                if(perc < 0)
                {
                    status 'Down'
                }
                else
                {
                    if(perc == 0)
                    {
                        status 'No Change'
                    }
                    else
                    {
                        status 'Up'
                    }
                }
            }
            else
            {
                percnt ''
                if(it[1] != null && it[2] == null)
                {
                    status 'New'
                }
                else if(it[1] == null && it[2] != null)
                {
                    status 'Removed'
                }
                else
                {
                    status 'No Data'
                }
            }

        }
        LOGGER.debug 'END compareSize'
        return builder.toString()
    }

    static List<Object> getHar(def args)
    {
        LOGGER.debug 'START getHAR' + args.txnName

        StringBuilder harData = new StringBuilder()
        double receive,dns,wait,blocked,time,send,ssl,connect,oncontentload,onload,filesize
        def resStartTime
        def analysisSample
        def harSample
        def id
        def status = 'true'
        def sampleValue = (args?.sampleValue == null ? '0' : args.sampleValue)

        def configReader = CommonUtils.getConfiguration(args)
        LOGGER.debug 'CONFIGURATION DETAILS : ' + args.ClientName + ':' + args.ProjectName + ':' + args.ScenarioName
        LOGGER.debug configReader.toString()

        if (configReader!= null)
        {
            if (Integer.parseInt(sampleValue) > 0)
            {
                // analysisSample = ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader,txnName,runID,sampleValue)
                analysisSample = ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader,args.txnName,configReader.CurrentRun.toString(),'Current',sampleValue.toString(),true)
                //ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader,txnName,(configReader?.BaselineRun ? null : configReader.BaselineRun.toString()),'Baseline',txnData.previous)
                LOGGER.debug 'OUTPUT extractSampleForDetailedAnalysis : ' + analysisSample
            }
            else
            {
                def txnCalcMetrics = ElasticSearchUtils.getAggregatedResponseTime(configReader,configReader.CurrentRun.toString(),args.txnName,'Current')
                LOGGER.debug 'OUTPUT getAggregatedResponseTime : ' + txnCalcMetrics
                def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)
                sampleValue = txnCalcMetrics?."$args.txnName"?."$comparisonPerc"
                analysisSample = ElasticSearchUtils.extractSampleForDetailedAnalysis(configReader,args.txnName,configReader.CurrentRun.toString(),'Current',sampleValue.toString(),true)
                LOGGER.debug 'OUTPUT extractSampleForDetailedAnalysis : ' + analysisSample
            }


            if(analysisSample?.hits?.hits[0]?.'_source' != null)
            {

                harSample = analysisSample?.hits?.hits[0]?.'_source'

                if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
                {

                    oncontentload = harSample.userPerceivedTime
                    onload = harSample.totalPageLoadTime
                    filesize = harSample.totalSize
                    time = harSample.userPerceivedTime
                    blocked = 0
                    dns = 0
                    connect = 0
                    ssl = 0
                    wait = 0
                    receive = oncontentload
                    send = 0
                    id = harSample.TransactionName
                }
                else
                {
                    id = harSample.url
                    if(harSample?.NavType == null || harSample?.NavType == 'Hard' || harSample?.NavType == '')
                    {
                        //oncontentload = (harSample.domContentLoadedEventEnd - harSample.navigationStart)
                        oncontentload = (harSample.domInteractive - harSample.navigationStart)

                        //onload = (harSample.loadEventEnd - harSample.navigationStart)
                        onload = (harSample.loadEventStart - harSample.navigationStart)

                        filesize = harSample.dom.toString().size()

                        time = (harSample.responseEnd - harSample.fetchStart)

                        //Blocked
                        blocked = (harSample.domainLookupStart - harSample.navigationStart)
                        blocked = (blocked < 0 ? 0 :  blocked)

                        dns = (harSample.domainLookupEnd - harSample.domainLookupStart)
                        connect = (harSample.connectStart == 0 ? 0 : (harSample.connectEnd - harSample.connectStart))
                        ssl = (harSample.secureConnectionStart == 0 ? 0 : (harSample.connectEnd - harSample.secureConnectionStart))
                        if (connect > 0)
                        {
                            ssl = connect - ssl
                        }
                        wait = (harSample.responseStart - harSample.requestStart)
                        receive = (harSample.responseEnd - harSample.responseStart)
                        send = 0


                    }
                    else
                    {
                        oncontentload = 0
                        onload = harSample.totalPageLoadTime
                        filesize = 0
                        time = onload
                        blocked = 0
                        dns = 0
                        connect = 0
                        ssl = 0
                        wait = 0
                        receive = onload
                        send = 0

                    }
                }

                harData.append('{"log":{"creator":{"name":"cxoptimise","version":"2.0.1"},"pages":[{')
                harData.append('"startedDateTime":"').append(CommonUtils.getISO8601StringForTimeStamp(harSample.navigationStart)).append('",')
                harData.append('"id":"').append(id).append('",')
                harData.append('"title":"').append(harSample.TransactionName).append('",')
                harData.append('"pageTimings":{')
                harData.append('"onContentLoad":').append(oncontentload).append(',')
                harData.append('"onLoad":').append(onload).append('}}],')
                harData.append('"entries":[{')
                harData.append('"startedDateTime":"').append( CommonUtils.getISO8601StringForTimeStamp(harSample.navigationStart)).append('",')
                harData.append('"time":').append(time).append(',')
                harData.append('"request":{"headers":[],"httpVersion":"HTTP/1.x","method":"GET')
                harData.append('","headersSize":-1,"bodySize":-1,"queryString":[],')
                harData.append('"url":"').append(id).append('",')
                harData.append('"cookies":[]},"cache":{},"response":{"_transferSize":0,"headers":[{"name":"Content-Type","value":""}],"redirectURL":"","httpVersion":"HTTP/1.x","statusText":"OK","headersSize":-1,"bodySize":-1,"content":{"size":').append(filesize).append(',"mimeType":"","compression":0},"cookies":[],"status":200},"timings":{')
                harData.append('    "blocked":').append(blocked).append(',')
                harData.append('    "dns":').append(dns).append(',')
                harData.append('    "connect":').append(connect).append(',')
                harData.append('    "send":').append(send).append(',')
                harData.append('    "wait":').append(wait).append(',')
                harData.append('    "receive":').append(receive).append(',')
                harData.append('    "ssl":').append(ssl).append('},')
                harData.append('"pageref":"').append(id).append('"}')
                LOGGER.debug 'OUTPUT Processing Resources'

                if ( harSample.Resources?.size() > 0)
                {
                    if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
                    {
                        for(int i=0;i < harSample.Resources.size();i++ )
                        {
                            LOGGER.debug 'OUTPUT Resource : ' + harSample.Resources[i].name
                            resStartTime =  harSample.Resources[i].requestStart +  harSample.navigationStart
                            LOGGER.debug 'OUTPUT Completed StartTime'

                            time = Math.round(harSample.Resources[i].totalResourceTime)

                            LOGGER.debug 'OUTPUT Completed Duration'

                            dns = -1
                            LOGGER.debug 'OUTPUT Completed DNS'
                            connect = -1
                            LOGGER.debug 'OUTPUT Completed Connect'

                            ssl = -1
                            send = harSample.Resources[i].requestEnd - harSample.Resources[i].requestStart
                            LOGGER.debug 'OUTPUT Completed Send'

                            wait = harSample.Resources[i].responseStart - harSample.Resources[i].requestEnd
                            LOGGER.debug 'OUTPUT Completed Wait'

                            receive = harSample.Resources[i].responseEnd - harSample.Resources[i].responseStart
                            LOGGER.debug 'OUTPUT Completed Receive'
                            blocked = 0

                            harData.append(',{')
                            harData.append('"startedDateTime":"').append( CommonUtils.getISO8601StringForTimeStamp(resStartTime)).append('",')
                            harData.append('"time":').append(time).append(',')
                            harData.append('"request":{"headers":[],"httpVersion":"HTTP/1.x","method":"').append(harSample.Resources[i].Method).append('","headersSize":-1,"bodySize":-1,"queryString":[],')
                            harData.append('"url":"').append(harSample.Resources[i].name).append('",')
                            harData.append('"cookies":[]},"cache":{},"response":{"_transferSize":-1,"headers":[{"name":"Content-Type","value":""}],"redirectURL":"","httpVersion":"HTTP/1.x","statusText":"OK","headersSize":-1,"bodySize":-1,"content":{"size":').append(harSample.Resources[i]."Content-Length").append(',"mimeType":"","compression":0},"cookies":[],"status":').append(harSample.Resources[i].Status).append('},"timings":{')
                            harData.append('"blocked":').append(blocked).append(',')
                            harData.append('"dns":').append(dns).append(',')
                            harData.append('"connect":').append(connect).append(',')
                            harData.append('"send":').append(send).append(',')
                            harData.append('"wait":').append(wait).append(',')
                            harData.append('"receive":').append(receive).append(',')
                            harData.append('"ssl":').append(ssl).append('},')
                            harData.append('"pageref":"').append(id).append('"}')
                            LOGGER.debug 'OUTPUT Resource Completed ' + harSample.Resources[i].name
                        }

                    }
                    else
                    {
                        for(int i=0;i < harSample.Resources.size();i++ )
                        {
                            LOGGER.debug 'OUTPUT Resource : ' + harSample.Resources[i].name
                            resStartTime =  harSample.Resources[i].startTime.longValue() +  harSample.navigationStart
                            LOGGER.debug 'OUTPUT Completed StartTime'

                            time = Math.round(harSample.Resources[i].duration)

                            LOGGER.debug 'OUTPUT Completed Duration'

                            dns = ((CommonUtils.isNullorZero(harSample.Resources[i].domainLookupEnd) || CommonUtils.isNullorZero(harSample.Resources[i].domainLookupEnd)) ?  0 : Math.round((harSample.Resources[i].domainLookupEnd  - harSample.Resources[i].domainLookupStart)))
                            LOGGER.debug 'OUTPUT Completed DNS'
                            connect = ((CommonUtils.isNullorZero(harSample.Resources[i].connectEnd) || CommonUtils.isNullorZero(harSample.Resources[i].connectStart)) ?  0 : Math.round(harSample.Resources[i].connectEnd - harSample.Resources[i].connectStart))
                            LOGGER.debug 'OUTPUT Completed Connect'

                            ssl = 0
                            try
                            {
                                // calc ssl only if req is secure connection
                                if (harSample.Resources[i]?.name != null && harSample.Resources[i].name.toLowerCase().startsWith("https:"))
                                {
                                    ssl = ((CommonUtils.isNullorZero(harSample.Resources[i].secureConnectionStart) || CommonUtils.isNullorZero(harSample.Resources[i].connectEnd)) ?  0 : Math.round(harSample.Resources[i].connectEnd - harSample.Resources[i].secureConnectionStart))
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



                            LOGGER.debug 'OUTPUT Completed SSL'

                            send = 0
                            //send = ((CommonUtils.isNullorZero(harSample.Resources[i].requestStart) || CommonUtils.isNullorZero(harSample.Resources[i].connectEnd)) ?  0 : Math.round(harSample.Resources[i].requestStart - harSample.Resources[i].connectEnd))
                            //log.debug 'OUTPUT Completed Send'

                            wait = ((CommonUtils.isNullorZero(harSample.Resources[i].responseStart) || CommonUtils.isNullorZero(harSample.Resources[i].requestStart)) ?  0 : Math.round(harSample.Resources[i].responseStart - harSample.Resources[i].requestStart))
                            LOGGER.debug 'OUTPUT Completed Wait'

                            receive = ((CommonUtils.isNullorZero(harSample.Resources[i].responseStart) || CommonUtils.isNullorZero(harSample.Resources[i].responseEnd)) ?  0 : Math.round(harSample.Resources[i].responseEnd - harSample.Resources[i].responseEnd))
                            LOGGER.debug 'OUTPUT Completed Receive'
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


                            harData.append(',{')
                            harData.append('"startedDateTime":"').append( CommonUtils.getISO8601StringForTimeStamp(resStartTime)).append('",')
                            harData.append('"time":').append(time).append(',')
                            harData.append('"request":{"headers":[],"httpVersion":"HTTP/1.x","method":"')
                            if(CommonUtils.stringContainsItemFromList(harSample.Resources[i].name,configReader.isStaticResources))
                            {
                                harData.append('GET')
                            }
                            else
                            {
                                harData.append('NA')
                            }
                            harData.append('","headersSize":-1,"bodySize":-1,"queryString":[],')
                            harData.append('"url":"').append(harSample.Resources[i].name.replaceAll('[",\\{\\}\\[\\]]+','')).append('",')
                            harData.append('"cookies":[]},"cache":{},"response":{"_transferSize":')
                            if(harSample.UserAgent.contains("Trident"))
                            {
                                harData.append(harSample.Resources[i]."Content-Length")
                            }
                            else
                            {
                                harData.append(harSample.Resources[i].transferSize)
                            }
                            harData.append(',"headers":[{"name":"Content-Type","value":""}],"redirectURL":"","httpVersion":"HTTP/1.x","statusText":"OK","headersSize":-1,"bodySize":-1,"content":{"size":-1,"mimeType":"","compression":0},"cookies":[],"status":200},"timings":{')
                            harData.append('"blocked":').append(blocked).append(',')
                            harData.append('"dns":').append(dns).append(',')
                            harData.append('"connect":').append(connect).append(',')
                            harData.append('"send":').append(send).append(',')
                            harData.append('"wait":').append(wait).append(',')
                            harData.append('"receive":').append(receive).append(',')
                            harData.append('"ssl":').append(ssl).append('},')
                            harData.append('"pageref":"').append(harSample.url).append('"}')
                            LOGGER.debug 'OUTPUT Resource Completed ' + harSample.Resources[i].name
                        }
                    }
                }
                harData.append('],')
                harData.append('"version":"1.2"')
                harData.append('}')
                harData.append('}')
            }
        }
        else
        {
            status = 'false'
            harData.append('{"ClientName": "').append(args.ClientName).append('","ProjectName": "').append(args.ProjectName).append('","Scenario": "').append(args.ScenarioName).append('","Reason": "No configuration available"}')
        }
        LOGGER.debug('Exiting getHAR')
        return [status,harData]
    }

    //String clientName,String projectName,String scenarioName,String esURL,String runID,String txnName
    static List<Object> getAllSamplesForTransaction(def args)
    {
        LOGGER.debug 'START getAllSamplesForTransaction for ' + args.txnName
        def rulesOutput = null
        def configReader = CommonUtils.getConfiguration(args)
        LOGGER.debug 'CONFIGURATION DETAILS : ' + args.ClientName + ':' + args.ProjectName + ':' + args.ScenarioName
        LOGGER.debug configReader.toString()
        boolean configFlag = false

        if (configReader != null)
        {
            configFlag = true
            def txnData = ElasticSearchUtils.extractAllSamples(configReader,configReader?.CurrentRun.toString(),args.txnName,'Current')
            def analysisList = []
            def row = [:]
            def sampleCounter = (configReader?.samplesCount == null ? 10 : configReader?.samplesCount)
            def cnt = 0
            txnData.hits.hits.each {it ->
                if(cnt < sampleCounter)
                {
                    cnt++
                    if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
                    {
                        rulesOutput = PaceRuleEngine.applyNativeAppRules(it._source,configReader)
                        row.put('NavType','NativeApp')
                        row.put('StartTime',it._source.StartTime)
                        row.put('Platform',it._source.Platform)
                        row.put('UserAgent','MobileNativeApp')
                        row.put('totalPageLoadTime',it._source.totalPageLoadTime)
                        row.put('userPerceivedTime',it._source.userPerceivedTime)
                        row.put('ScreenName',it._source.ScreenName)
                        row.put('totalSize',it._source.totalSize)
                        row.put('totalRequest',it._source.totalRequest)
                        row.put('score',rulesOutput[0])
                        row.put('recommendation',rulesOutput[1])
                        row.put('resourceLoadTime',(it._source?.resourceLoadTime == null ? 0 : (Double.parseDouble(it._source.resourceLoadTime.toString()).round())))
                        row.put('resourceCount',it._source.Resources.size)

                        //row = it._source.StartTime + '##' + it._source.Platform + '##MobileNativeApp##' + it._source.totalPageLoadTime + '##-2##-2##-2##-2##-2##-2##-2##' + it._source.userPerceivedTime  + '##' + it._source.ScreenName  + '##' + it._source.totalSize  + '##' + it._source.totalRequest  + '##' + rulesResult[0] + '##' + rulesResult[1]
                    }
                    else
                    {
                        if(it._source?.NavType == null || it._source?.NavType == 'Hard' || it._source?.NavType == '')
                        {
                            rulesOutput = PaceRuleEngine.applyGenericRules(it._source,configReader)
                            row.put('NavType','Hard')
                            row.put('StartTime',it._source.StartTime)
                            row.put('Platform',it._source.Platform)
                            row.put('UserAgent',it._source.UserAgent)
                            row.put('BrowserName',it._source?.BrowserName)
                            row.put('DeviceType',it._source?.DeviceType)
                            row.put('totalPageLoadTime',it._source.totalPageLoadTime)
                            row.put('fetchStartTime',it._source.fetchStartTime)
                            row.put('redirectTime',it._source.redirectTime)
                            row.put('cacheFetchTime',it._source.cacheFetchTime)
                            row.put('dnsLookupTime',it._source.dnsLookupTime)
                            row.put('tcpConnectTime',it._source.tcpConnectTime)
                            row.put('serverTime',it._source.serverTime_ttfb)
                            row.put('downloadTime',it._source.downloadTime)
                            row.put('domProcessingTime',it._source.domProcessingTime)
                            row.put('onloadTime',it._source.onloadTime)
                            row.put('clientTime',it._source.clientTime)
                            row.put('resourceLoadTime',(it._source?.resourceLoadTime == null ? 0 : (Double.parseDouble(it._source.resourceLoadTime.toString()).round())))
                            row.put('score',rulesOutput[0])
                            row.put('recommendation',rulesOutput[1])
                            row.put('speedIndex',(it._source?.speedIndex == null ? 0 : (Double.parseDouble(it._source.speedIndex.toString()).round())))
                            row.put('domCount',it._source.domElementCount)
                            row.put('resourceCount',it._source.Resources.size)
                            row.put('resourceSize',it._source.resourceSize)
                            row.put('ttfbUser',(it._source?.ttfbUser == null ? 0 : it._source?.ttfbUser))
                            row.put('ttfbBrowser',(it._source?.ttfbBrowser == null ? 0 : it._source?.ttfbBrowser))
                            row.put('ttfpUser',(it._source?.ttfbUser == null ? 0 : it._source?.ttfbUser))
                            row.put('ttfpBrowser',(it._source?.ttfpBrowser == null ? 0 : it._source?.ttfpBrowser))
                            row.put('clientProcessing',(it._source?.clientProcessing == null ? 0 : it._source?.clientProcessing))
                            row.put('visuallyComplete',(it._source?.visuallyComplete == null ? 0 : it._source?.visuallyComplete))
                            row.put('domInteractive',(it._source?.renderingTime == null ? 0 : it._source?.renderingTime))
                            //row.put('resourceBlockTime',PaceRuleEngine.calculateBlockingTime(it._source?.Resources))
                            //backendTime = PaceRuleEngine.calculateBackendTime(configReader,it._source?.totalPageLoadTime,it._source?.Resources,false)
                            //row.put('backendAnalysis',backendTime[0])
                            //row.put('resourceBlockTime',Double.parseDouble(PaceRuleEngine.calculateBlockingTime(detailBreakUp.Resources).toString()).round())
                            row.put('resourceBlockTime',PaceRuleEngine.calculateBackendTime(configReader,it._source?.totalPageLoadTime,it._source?.Resources,false)[1].round())
                            //row = it._source.StartTime + '##' + it._source.Platform + '##' + it._source.UserAgent + '##' + it._source.totalPageLoadTime + '##' + it._source.fetchStartTime + '##' + it._source.redirectTime + '##' + it._source.cacheFetchTime + '##' + it._source.dnsLookupTime + '##' + it._source.tcpConnectTime + '##' + it._source.serverTime_ttfb + '##' + it._source.downloadTime + '##' + it._source.domProcessingTime + '##' + it._source.onloadTime + '##' + it._source.domElementCount + '##' + it._source.Resources.size  + '##' + rulesResult[0] + '##' + rulesResult[1] + '##' + it._source.speedIndex
                        }
                        else
                        {
                            rulesOutput = PaceRuleEngine.applySoftNavigationRules(it._source,configReader)
                            row.put('NavType','Soft')
                            row.put('StartTime',it._source.StartTime)
                            row.put('Platform',it._source.Platform)
                            row.put('UserAgent',it._source.UserAgent)
                            row.put('BrowserName',it._source?.BrowserName)
                            row.put('DeviceType',it._source?.DeviceType)
                            row.put('totalPageLoadTime',it._source.totalPageLoadTime)
                            row.put('resourceLoadTime',(it._source?.resourceLoadTime == null ? 0 : (Double.parseDouble(it._source.resourceLoadTime.toString()).round())))
                            row.put('score',rulesOutput[0])
                            row.put('recommendation',rulesOutput[1])
                            row.put('resourceCount',it._source.Resources.size)
                            row.put('resourceSize',it._source.resourceSize)
                            //backendTime = PaceRuleEngine.calculateBackendTime(configReader,it._source?.totalPageLoadTime,it._source?.Resources,false)
                            //row.put('backendAnalysis',backendTime[0])
                            //row.put('resourceBlockTime',Double.parseDouble(PaceRuleEngine.calculateBlockingTime(detailBreakUp.Resources).toString()).round())
                            row.put('resourceBlockTime',PaceRuleEngine.calculateBackendTime(configReader,it._source?.totalPageLoadTime,it._source?.Resources,false)[1].round())
                            row.put('clientProcessing',(it._source.totalPageLoadTime - (it._source?.resourceLoadTime == null ? 0 : (Double.parseDouble(it._source.resourceLoadTime.toString()).round()))))
                            row.put('visuallyComplete',(it._source?.visuallyComplete == null ? 0 : it._source?.visuallyComplete))
                            //row = it._source.StartTime + '##' + it._source.Platform + '##' + it._source.UserAgent + '##' + it._source.totalPageLoadTime + '##-1##-1##-1##-1##-1##-1##-1##-1##-1##-1##'  + it._source.Resources.size  + '##' + rulesResult[0] + '##' + rulesResult[1] + '##' + it._source.resourceLoadTime
                        }
                    }

                }

                analysisList.add(row.clone())
            }
            LOGGER.debug 'END getAllSamplesForTransaction for ' + args.txnName
            if(args?.scale == null || args.scale == 'milliseconds')
            {
                return [configFlag,PaceReportEngine.getJsonStringForAllSamples(configReader,args.txnName,(args?.CurrentRun == null ? configReader.RunID : args.CurrentRun),analysisList,args?.timezone)]
            }
            else
            {
                return [configFlag,PaceReportEngine.getJsonStringForAllSamplesInSeconds(configReader,args.txnName,(args?.CurrentRun == null ? configReader.RunID : args.CurrentRun),analysisList,args?.timezone)]
            }
        }
        else
        {
            return [configFlag,null]
        }
    }

    static List<Object> getStatusForCICD(def args,def esUrl)
    {
        LOGGER.debug 'START getStatusForCICD'
        LOGGER.debug 'Input Request :' + args
        //StringBuilder report = new StringBuilder()
        def errorMsg = []
        def reqMap = JsonUtils.jsonToMap(args)
        reqMap.put('contentType','json')
        reqMap.put('esUrl',esUrl)
        reqMap.put('cicd',true)
        def statusList = []
        def compliance = [:]

        compliance.put('totalScoreComplianceWeight', reqMap?.totalScoreComplianceWeight == null ? 25 : reqMap?.totalScoreComplianceWeight)
        compliance.put('compareComplianceWeight', reqMap?.compareComplianceWeight == null ? 25 : reqMap?.compareComplianceWeight)
        compliance.put('indvScoreComplianceWeight', reqMap?.indvScoreComplianceWeight == null ? 25 : reqMap?.indvScoreComplianceWeight)
        compliance.put('slaComplianceWeight', reqMap?.slaComplianceWeight == null ? 25 : reqMap?.slaComplianceWeight)

        def summaryReport = PaceAnalysisEngine.generateSummaryReport(reqMap)
        if((summaryReport[3]).toString().equals("true") && summaryReport[0] != null)
        {
            def summaryJson = new JSONObject((summaryReport[0]).toString())
            def summaryMap = JsonUtils.jsonToMap(summaryJson)
            def ignoreTransactionList = []

            if(reqMap?.excludeTransactionsList != null && reqMap?.excludeTransactionsList != '')
            {
                reqMap?.excludeTransactionsList.toString().split(',').each { ignoreTransactionList.add(it) }
            }


            if (summaryMap.execSummary.totalScore > (reqMap?.totalScoreThreshold == null || reqMap?.totalScoreThreshold == '' ? 80 : reqMap?.totalScoreThreshold))
            {
                compliance.put('totalScoreCompliance', true)
            } else {
                compliance.put('totalScoreCompliance', false)
                compliance.put('totalScoreComplianceReason', 'Total Score :' + summaryMap.execSummary.totalScore + ' less than configured threshold ' + (reqMap?.totalScoreThreshold == null ? 80 : reqMap?.totalScoreThreshold))
            }

            summaryMap.Transactions.each { it ->
                if (!ignoreTransactionList.count(it.name)) {
                    if (Double.parseDouble(it.deviation.toString()) > (reqMap?.compareDeviationThreshold == null || reqMap?.compareDeviationThreshold =='' ? 5 : reqMap?.compareDeviationThreshold)) {
                        statusList.add(false)
                        errorMsg.add(it.name + ' comparison percentage deviation of ' + it.deviation + '% is greater than allowed threshold of ' + (reqMap?.compareDeviationThreshold == null ? 5 : reqMap?.compareDeviationThreshold) + '%.')
                    } else {
                        statusList.add(true)

                    }
                }

            }

            def compareCompliance = (statusList.count(true) / statusList.size()) * 100
            if (compareCompliance > (reqMap?.compareComplianceThreshold == null || reqMap?.compareComplianceThreshold == '' ? 80 : reqMap?.compareComplianceThreshold)) {
                compliance.put('compareComplianceStatus', true)
                compliance.put('compareCompliancePercentage', compareCompliance)
            } else {
                compliance.put('compareComplianceStatus', false)
                compliance.put('compareCompliancePercentage', compareCompliance)
                compliance.put('compareComplianceReason', errorMsg.clone())
            }

            errorMsg.removeAll { it || !it }
            statusList.removeAll { it || !it }

            summaryMap.Transactions.each { it ->
                if (!ignoreTransactionList.count(it.name)) {
                    if (Double.parseDouble(it.score.toString()) < (reqMap?.individualScoreThreshold == null || reqMap?.individualScoreThreshold == '' ? 80 : reqMap?.individualScoreThreshold)) {
                        statusList.add(false)
                        errorMsg.add(it.name + ' score of ' + it.score + ' is lesser than allowed threshold of ' + (reqMap?.individualScoreThreshold == null || reqMap?.individualScoreThreshold == '' ? 80 : reqMap?.individualScoreThreshold))
                    } else {
                        statusList.add(true)
                    }
                }
            }


            def indvScoreCompliance = (statusList.count(true) / statusList.size()) * 100
            if (indvScoreCompliance > (reqMap?.indvScoreComplianceThreshold == null || reqMap?.indvScoreComplianceThreshold == '' ? 80 : reqMap?.indvScoreComplianceThreshold)) {
                compliance.put('indvScoreComplianceStatus', true)
                compliance.put('indvScoreCompliancePercentage', indvScoreCompliance)
            } else {
                compliance.put('indvScoreComplianceStatus', false)
                compliance.put('indvScoreCompliancePercentage', indvScoreCompliance)
                compliance.put('indvScoreComplianceReason', errorMsg.clone())
            }

            errorMsg.removeAll { it || !it }
            statusList.removeAll { it || !it }

            summaryMap.Transactions.each { it ->
                if (!ignoreTransactionList.count(it.name)) {
                    if (Double.parseDouble(it.restime.toString()) > Double.parseDouble(it.sla.toString())) {
                        statusList.add(false)
                        errorMsg.add(it.name + ' with response time of ' + it.restime + ' ms exceeds configured SLA of ' + it.sla + ' ms')
                    } else {
                        statusList.add(true)
                    }
                }
            }

            def slaCompliance = (statusList.count(true) / statusList.size()) * 100
            if (slaCompliance > (reqMap?.slaComplianceThreshold == null || reqMap?.slaComplianceThreshold == '' ? 80 : reqMap?.slaComplianceThreshold)) {
                compliance.put('slaComplianceStatus', true)
                compliance.put('slaCompliancePercentage', slaCompliance)
            } else {
                compliance.put('slaComplianceStatus', false)
                compliance.put('slaCompliancePercentage', slaCompliance)
                compliance.put('slaComplianceReason', errorMsg.clone())
            }

            summaryMap.put('compliance', compliance)

            LOGGER.debug 'END getStatusForCICD'
            return [new JSONObject(summaryMap).toString(), summaryReport[1], summaryReport[2], summaryReport[3]]
        }
        else
        {
            LOGGER.debug 'END getStatusForCICD'
            return [null, null, null, false]
        }
    }

    static def getClientList(String esUrl)
    {
        def response = null
        response = ElasticSearchUtils.getAvailableClientName(esUrl)
        return response
    }

    static def getClientDetails(String username,String esUrl,int noOfDays,String timezone='UTC')
    {
        LOGGER.debug 'START getAccessInfo'
        StringBuilder details = new StringBuilder()
        def userDetails = null
        def clientList = []
        def configDetails = null

        userDetails = ElasticSearchUtils.IsUserExists(esUrl,username)
        if(userDetails.hits?.hits.size > 0)
        {
            if (username == 'cxopuser' || username == 'cxopadmin') {
                clientList = ElasticSearchUtils.getUniqueClients(esUrl)
            } else {
                clientList = userDetails.hits.hits[0]._source.Clients.tokenize(',')
            }

            LOGGER.debug 'All Clients :' + clientList + 'for User ' + username
            if (clientList.size > 0) {
                def response_body = ElasticSearchUtils.getClientDetails(esUrl, noOfDays, clientList)
                details.append('{"UserName" : "').append(username).append('","ClientList":[')
                int clientSize = response_body.aggregations.ClientName.buckets.size
                int projectSize = 0
                int scenarioSize = 0
                int runSize = 0
                int displaySize = 0
                if (clientSize > 0) {
                    for (int i = 0; i < clientSize; i++) {
                        if (i != clientSize - 1) {
                            details.append('{"CName" : "').append(response_body.aggregations.ClientName.buckets[i].key).append('",')
                            details.append('"ProjectList":[')
                            projectSize = response_body.aggregations.ClientName.buckets[i].ProjectName.buckets.size
                            if (projectSize > 0) {
                                for (int j = 0; j < projectSize; j++) {
                                    if (j != projectSize - 1) {
                                        details.append('{"PName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].key).append('",')
                                        details.append('"ScenarioList":[')
                                        scenarioSize = response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets.size
                                        if (scenarioSize > 0) {
                                            for (int k; k < scenarioSize; k++) {
                                                if (k != scenarioSize - 1) {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"},')
                                                } else {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"}')
                                                }
                                            }
                                        }
                                        details.append(']},')
                                    } else {
                                        details.append('{"PName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].key).append('",')
                                        details.append('"ScenarioList":[')
                                        scenarioSize = response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets.size
                                        if (scenarioSize > 0) {
                                            for (int k; k < scenarioSize; k++) {
                                                if (k != scenarioSize - 1) {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"},')
                                                } else {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"}')
                                                }
                                            }
                                        }
                                        details.append(']}')
                                    }
                                }
                            }
                            details.append(']},')
                        } else {
                            details.append('{"CName" : "').append(response_body.aggregations.ClientName.buckets[i].key).append('",')
                            details.append('"ProjectList":[')
                            projectSize = response_body.aggregations.ClientName.buckets[i].ProjectName.buckets.size
                            if (projectSize > 0) {
                                for (int j = 0; j < projectSize; j++) {
                                    if (j != projectSize - 1) {
                                        details.append('{"PName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].key).append('",')
                                        details.append('"ScenarioList":[')
                                        scenarioSize = response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets.size
                                        if (scenarioSize > 0) {
                                            for (int k; k < scenarioSize; k++) {
                                                if (k != scenarioSize - 1) {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"},')
                                                } else {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"}')
                                                }
                                            }
                                        }
                                        details.append(']},')
                                    } else {
                                        details.append('{"PName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].key).append('",')
                                        details.append('"ScenarioList":[')
                                        scenarioSize = response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets.size
                                        if (scenarioSize > 0) {
                                            for (int k; k < scenarioSize; k++) {
                                                if (k != scenarioSize - 1) {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"},')
                                                } else {
                                                    details.append('{"SName" : "').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].key).append('"}')
                                                }
                                            }
                                        }
                                        details.append(']}')
                                    }
                                }
                            }
                            details.append(']}')

                        }
                    }
                    details.append(']}')
                }
                //details.append('{"UserName: "').append(username).append('","ClientList":[]}')
            }
            else
            {
                details.append('{"UserName: "').append(username).append('","ClientList":[]}')
            }
        }
        else
        {
            details.append('{"UserName: "').append(username).append('","ClientList":[]}')
        }

        //println details

        LOGGER.debug 'Project AND Run Details : ' + details
        LOGGER.debug 'END getAccessInfo'
        return details
    }


    static JSONObject getRunDetails(def input,int noOfDays)
    {
        LOGGER.debug 'START getRunDetails'
        StringBuilder details = new StringBuilder()
        def userDetails = null
        def clientList = []
        def configDetails = null
        configDetails = ElasticSearchUtils.extractConfig(input.ClientName,input.ProjectName,input.Scenario,GlobalConstants.ESUrl,null)
        details.append('{')
        if(configDetails != null)
        {
            def response_body = ElasticSearchUtils.extractRunDetails(GlobalConstants.ESUrl,noOfDays,input.ClientName,input.ProjectName,input.Scenario)
            int runSize = 0
            int displaySize = 0
            details.append('"status" : true,')
            details.append('"BaselineRunID":').append(configDetails.BaselineRunID).append(',')
            details.append('"RunID":').append(configDetails.RunID).append(',')
            details.append('"RunIDs":[')

            runSize = response_body.aggregations.RunIDS.buckets.size
            displaySize = runSize - GlobalConstants.getRunIDCount()
            response_body.aggregations.RunIDS.buckets.sort{it.key}
            if(runSize > 0)
            {
                for(int l; l < runSize; l++)
                {
                    if(l > displaySize)
                    {
                        if(l != runSize - 1)
                        {
                            //details.append('{"RunID" :').append(response_body.aggregations.ClientName.buckets[i].ProjectName.buckets[j].Scenario.buckets[k].RunID.buckets[l].key).append('},')
                            details.append('{"RunID" :').append(response_body.aggregations.RunIDS.buckets[l].key).append(',')
                            def result = ElasticSearchUtils.executionTimeOfRunID(input.ClientName,input.ProjectName,input.Scenario,response_body.aggregations.RunIDS.buckets[l].key,input.TimeZone)
                            details.append('"Release" : "').append(result[1]).append('",')
                            details.append('"Build" : "').append(result[2]).append('",')
                            details.append('"Time" : "').append(result[0]).append('"},')
                        }
                        else
                        {
                            details.append('{"RunID" :').append(response_body.aggregations.RunIDS.buckets[l].key).append(',')
                            def result = ElasticSearchUtils.executionTimeOfRunID(input.ClientName,input.ProjectName,input.Scenario,response_body.aggregations.RunIDS.buckets[l].key,input.TimeZone)
                            details.append('"Release" : "').append(result[1]).append('",')
                            details.append('"Build" : "').append(result[2]).append('",')
                            details.append('"Time" : "').append(result[0]).append('"}')
                        }

                    }

                }

            }
            details.append(']')
        }
        else
        {
            details.append('"status" : false')
        }
        details.append('}')


        LOGGER.debug 'Project AND Run Details : ' + details
        LOGGER.debug 'END getClientRunDetails'
        return new JSONObject(details.toString())
    }


    //String clientName,String projectName,String scenarioName,String baseUrl,String runID = null,String samplePercentile = null
    static List<Object> getCBTDetails(def args)
    {
        LOGGER.debug 'START getCBTDetails'
        def finalMap = [:]
        def configReader = CommonUtils.getConfiguration(args)
        boolean configFlag = false
        LOGGER.debug 'CONFIGURATION DETAILS : ' + args.ClientName + ':' + args.ProjectName + ':' + args.ScenarioName
        LOGGER.debug configReader.toString()
        if (configReader != null)
        {
            configFlag = true
            def metricsMap = [:]
            def browserMap = [:]
            def browserList = []
            def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)
            def txnSample
            if(args?.txnName == null)
            {
                txnSample= ElasticSearchUtils.getAggregatedResponseTimeForCBT(configReader,configReader.CurrentRun.toString(),null,'Current')
            }
            else
            {
                txnSample= ElasticSearchUtils.getAggregatedResponseTimeForCBT(configReader,configReader.CurrentRun.toString(),args.txnName,'Current')
            }

            LOGGER.debug 'Transaction List for CBT : ' + txnSample
            def detailedSample
            def pcntMap = [:]
            int resSize = 0
            def uniqueUA = []
            def navType

            txnSample.each { it ->
                it.value.each { item ->
                    detailedSample = ElasticSearchUtils.getSampleForDetailedAnalysisForCBT(configReader, it.key, item.UA, item."$comparisonPerc".toString(),configReader.CurrentRun.toString(),'Current')
                    metricsMap.put("userAgent", detailedSample.ResolvedUA)
                    metricsMap.put("totalPageLoadTime",detailedSample.totalPageLoadTime)
                    metricsMap.put("resourceLoadTime", (detailedSample?.resourceLoadTime == null ? 0 : Double.parseDouble(detailedSample.resourceLoadTime.toString()).round()))
                    if (detailedSample?.NavType == 'Hard')
                    {
                        navType = 'Hard'
                        metricsMap.put("clientTime", detailedSample.clientTime)
                        metricsMap.put("pageRenderingTime", detailedSample.pageRenderingTime)
                        metricsMap.put("pagenetworkTime", detailedSample.pagenetworkTime)
                        metricsMap.put("pageDomProcessingTime", detailedSample.pageDomProcessingTime)
                        metricsMap.put("speedIndex", (detailedSample?.speedIndex == null ? 0 : Double.parseDouble(detailedSample.speedIndex.toString()).round()))
                    }
                    else
                    {
                        navType = 'Soft'
                        metricsMap.put("clientTime",(detailedSample.totalPageLoadTime - (detailedSample?.resourceLoadTime == null ? 0 : Double.parseDouble(detailedSample.resourceLoadTime.toString()).round())))
                    }
                    detailedSample.Resources.sort { x, y ->
                        if (x.responseEnd == y.responseEnd) {
                            x.fetchStart <=> y.fetchStartyes
                        } else {
                            x.responseEnd <=> y.responseEnd
                        }
                    }
                    resSize = detailedSample.Resources.size()
                    metricsMap.put("firstResStartTime", Double.parseDouble(detailedSample.Resources[0].fetchStart.toString()).round())
                    metricsMap.put("lastResStartTime", Double.parseDouble(detailedSample.Resources[resSize - 1].fetchStart.toString()).round())
                    metricsMap.put("aggResdnsLookupTime", detailedSample.Resources.sum { it.dnsLookupTime })
                    metricsMap.put("aggRestcpConnectTime", detailedSample.Resources.sum { it.tcpConnectTime })
                    pcntMap = PaceRuleEngine.getResourcesPercentageForCBT(detailedSample.Resources, configReader)
                    metricsMap.put("pcntResourcesCached", pcntMap.pcntCached)
                    metricsMap.put("pcntResourcesCacheValidated", pcntMap.pcntCacheValidator)
                    metricsMap.put("pcntResourcesCompressed", pcntMap.pcntCompressed)
                    metricsMap.put("resrcCount", resSize)
                    uniqueUA.add(item.UA.toString())
                    browserList.add(metricsMap.clone())
                }
                browserMap.put(it.key, browserList.clone())
                browserList.clear()
            }

            def arrList = []
            def resultMap = [:]


            browserMap.each { it ->
                resultMap.put('Name', it.key)
                resultMap.put('Metrics', it.value)
                resultMap.put('Verdict', PaceRuleEngine.getCBTVerdict(it.value,navType))
                arrList.add(resultMap.clone())
            }
            finalMap.put('UA', uniqueUA.unique())
            finalMap.put('Transaction', arrList)
        }
        LOGGER.debug 'END getCBTDetails'
        if(configFlag)
        {
            return [configFlag,JsonOutput.toJson(finalMap)]
        }
        else
        {
            return [configFlag,null]
        }

    }


    static int getNumberOfNodes(String esUrl)
    {
        int nodes = 1
        def response = ElasticSearchUtils.elasticSearchGET(esUrl,'_nodes')
        if(response?.nodes != null && response?.nodes?.size() > 1)
        {
            nodes = response?."_nodes"?.nodes.size()
        }
        return nodes

    }

    static boolean isIndexExists(String esUrl,String path)
    {

    }

    static boolean checkOrCreateIndex(String esUrl,String path,String mapping)
    {
        boolean chkFlag = false
        def response = ElasticSearchUtils.elasticSearchGET(esUrl,path + '/_mapping')
        LOGGER.debug('Index {} GET Response {}',path,response)
        if(response != null)
        {
            chkFlag = true
        }

        if(!chkFlag)
        {
            response = ElasticSearchUtils.elasticSearchPUT(esUrl,path,mapping)
            LOGGER.debug('Index {} PUT Response {}',path,response)
            if(response != null)
            {
                chkFlag = true
            }
            else
            {
                chkFlag = false
            }

        }
        return chkFlag

    }

    static boolean setupRules(String esUrl,String rules,String esVersion = '5')
    {
        boolean chkFlag = false
        def response = null
        if(esVersion == '5')
        {
            response = ElasticSearchUtils.elasticSearchGET(esUrl,GlobalConstants.RULESDOC)
        }
        else
        {
            response = ElasticSearchUtils.elasticSearchGET(esUrl,GlobalConstants.ES6RULESDOC)
        }

        if(response != null)
        {
            chkFlag = true
        }

        if(!chkFlag)
        {
            if(esVersion == '5')
            {
                response = ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.RULESDOC,rules)
            }
            else
            {
                response = ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.ES6RULESDOC,rules)
            }

            if(response != null)
            {
                chkFlag = true
            }
            else
            {
                chkFlag = false
            }

        }
        return chkFlag

    }

    static boolean setupKibanaDashboard(String esUrl,String path,String visDetails)
    {
        def response = ElasticSearchUtils.elasticSearchGET(esUrl,path)
        def chkFlag = false
        if(response != null)
        {
            chkFlag = true
        }

        if(!chkFlag)
        {
            response = ElasticSearchUtils.elasticSearchPOST(esUrl,path,visDetails)
            if(response != null)
            {
                chkFlag = true
            }
            else
            {
                chkFlag = false
            }

        }
        return chkFlag

    }

    static List<Object> persistMetrics(String esUrl,String path,String body)
    {
        def id = null
        def status = false
        def response = ElasticSearchUtils.elasticSearchPOST(esUrl,path,body)
        if(response != null && response?.created)
        {
            status = true
            id =  response.'_id'
        }

        return [status,id]

    }

    static Map<String,Object> getConfiguration(def args)
    {
        def config = null
        def response = null
        def id

        response = ElasticSearchUtils.isConfigExists(args.ClientName,args.ProjectName,args.Scenario,args.esUrl)

        if(response != null && response?.hits?.hits?.size() > 0)
        {
            id = response.hits.hits[0]."_id"
            response = response.hits.hits[0]."_source"
            response.remove('dataStoreUrl')
            response.remove('beaconUrl')

            if(args.updateRunID.equals('time') && (((System.currentTimeMillis() - response.runTimestamp)/60000) > response.runIntervalInMinutes))
            {
                synchronized(this) {
                    response.put('RunID', (response.RunID + 1))
                    response.put('runTimestamp', System.currentTimeMillis())
                    def update = ElasticSearchUtils.elasticSearchPOSTWithMap(args.esUrl, GlobalConstants.CONFIGTABLE_WS + id, response)
                    if (update != null && update?.result == 'updated') {
                        ElasticSearchUtils.elasticSearchGET(GlobalConstants.ESUrl, GlobalConstants.CONFIGREFRESH)
                        config = ElasticSearchUtils.isConfigExists(args.ClientName, args.ProjectName, args.Scenario, args.esUrl)?.hits?.hits[0]?."_source"
                    }
                }
            } else if(args.updateRunID.equals('true'))
            {
                synchronized(this) {
                    response.put('RunID', (response.RunID + 1))
                    response.put('runTimestamp', System.currentTimeMillis())
                    def update = ElasticSearchUtils.elasticSearchPOSTWithMap(args.esUrl, GlobalConstants.CONFIGTABLE_WS + id, response)
                    if (update != null && update?.result == 'updated') {
                        ElasticSearchUtils.elasticSearchGET(GlobalConstants.ESUrl, GlobalConstants.CONFIGREFRESH)
                        config = ElasticSearchUtils.isConfigExists(args.ClientName, args.ProjectName, args.Scenario, args.esUrl)?.hits?.hits[0]?."_source"
                    }
                }

            } else
            {
                config = response
            }
        }
        return config

    }

    static Map<String,Object> createConfiguration(def args)
    {
        def config = [:]
        def response = null
        def id

        response = ElasticSearchUtils.isConfigExists(args.ClientName,args.ProjectName,args.Scenario,args.esUrl)


        if(response != null && response?.hits?.hits?.size() > 0)
        {
            config.put('Status',1)
        }
        else
        {
            def jsonObj = new JSONObject(args.defaultConfig.toString())
            def defaultConfig = JsonUtils.jsonToMap(jsonObj)

            defaultConfig.each{k,v ->
                config.put(k,v)
            }
            config.put('ClientName', args.ClientName)
            config.put('ProjectName', args.ProjectName)
            config.put('Scenario', args.Scenario)
            config.put('RunID', 1)
            config.put('ProjectNameOrg', args.ProjectNameOrg)
            config.put('ScenarioOrg', args.ScenarioOrg)
            config.put('ClientNameOrg', args.ClientNameOrg)
            config.put('creationTimestamp',System.currentTimeMillis())
            config.put('runTimestamp',System.currentTimeMillis())
            config.put('comments', 'Config file was created by admin')
            config.put('configUpdated', false)
            config.put('SLACheck', 'onload')

            if(args.isMarkEnabled == true)
            {
                config.put('isMarkAPIEnabled', true)
            }

            if(args.isLoadTest == true)
            {
                config.put('isLoadTest', true)
                config.put('isResourceCrawlingEnabled',false)
                config.put('isMemoryAPIEnabled',false)
            }
            if(args.isNativeApp == true)
            {
                config.put('isNativeApp',true)
                config.put('isCPUEnabled',false)
                config.put('isMemoryEnabled', false)
                config.put('httpCallsThreshold',10)
                config.put('excludedNativeAppRules','')
                config.put('resourceRTThrehold',500)
                config.put('totalPayloadThreshold',102400)
                config.put('resourcePayloadThreshold',10240)
                config.put('userPerceivedSLA',5000)
                config.put('defaultTransactionThreshold',8000)
            }

            config.put('Rules',ElasticSearchUtils.getRules(args.esUrl))
            config.put('kibanaURL',args.kibanaUrl)


            response = ElasticSearchUtils.elasticSearchPOSTWithMap(args.esUrl,GlobalConstants.CONFIGTABLE,config)
            if(response == null)
            {
                config = null
            }
            else
            {
                config = ElasticSearchUtils.elasticSearchGET(args.esUrl,GlobalConstants.CONFIGTABLE_WS + response?."_id")."_source"
            }

        }
        return config

    }

    static Map<String,String> updateConfiguration(def request,def esUrl)
    {
        def updateStatus = [:]
        def id
        def response = null

        def updatedConfig = JsonUtils.jsonToMap(request)
        response = ElasticSearchUtils.isConfigExists(updatedConfig.ClientName,updatedConfig.ProjectName,updatedConfig.Scenario,esUrl)
        if(response != null && response?.hits?.hits?.size() > 0)
        {
            id = response?.hits?.hits[0]?."_id"
            def config = response?.hits?.hits[0]?."_source"

            if(updatedConfig?.configUpdated != null)
            {
                config.put('configUpdated',updatedConfig?.configUpdated)
            }

            if(updatedConfig?.SLACheck != null)
            {
                config.put('SLACheck',updatedConfig?.SLACheck)
            }

            if(updatedConfig?.devThreshold != null)
            {
                config.put('devThreshold',updatedConfig?.devThreshold)
            }

            if(updatedConfig?.compareComplianceThreshold != null)
            {
                config.put('compareComplianceThreshold',updatedConfig?.compareComplianceThreshold)
            }

            if(updatedConfig?.totalScoreThreshold != null)
            {
                config.put('totalScoreThreshold',updatedConfig?.totalScoreThreshold)
            }

            if(updatedConfig?.slaComplianceThreshold != null)
            {
                config.put('slaComplianceThreshold',updatedConfig?.slaComplianceThreshold)
            }

            if(updatedConfig?.individualScoreThreshold != null)
            {
                config.put('individualScoreThreshold',updatedConfig?.individualScoreThreshold)
            }

            if(updatedConfig?.indvScoreComplianceThreshold != null)
            {
                config.put('indvScoreComplianceThreshold',updatedConfig?.indvScoreComplianceThreshold)
            }

            if(updatedConfig?.totalScoreComplianceWeight != null && updatedConfig?.compareComplianceWeight != null && updatedConfig?.indvScoreComplianceWeight != null && updatedConfig?.slaComplianceWeight != null)
            {
                if((updatedConfig.totalScoreComplianceWeight + updatedConfig.compareComplianceWeight + updatedConfig.indvScoreComplianceWeight + updatedConfig.slaComplianceWeight) == 100)
                {
                    config.put('totalScoreComplianceWeight',updatedConfig?.totalScoreComplianceWeight)
                    config.put('compareComplianceWeight',updatedConfig?.compareComplianceWeight)
                    config.put('indvScoreComplianceWeight',updatedConfig?.indvScoreComplianceWeight)
                    config.put('slaComplianceWeight',updatedConfig?.slaComplianceWeight)
                }

            }

            if(updatedConfig?.samplePercentile != null)
            {
                config.put('samplePercentile',updatedConfig?.samplePercentile)
            }

            if(updatedConfig?.httpCallsThreshold != null)
            {
                config.put('httpCallsThreshold',updatedConfig?.httpCallsThreshold)
            }

            if(updatedConfig?.domCountThreshold != null)
            {
                config.put('domCountThreshold',updatedConfig?.domCountThreshold)
            }

            if(updatedConfig?.BaselineRunID != null)
            {
                config.put('BaselineRunID',updatedConfig?.BaselineRunID)
            }

            if(updatedConfig?.Release != null)
            {
                config.put('Release',updatedConfig?.Release)
            }

            if(updatedConfig?.BuildNumber != null)
            {
                config.put('BuildNumber',updatedConfig?.BuildNumber)
            }

            if(updatedConfig?.isMemoryAPIEnabled != null)
            {
                config.put('isMemoryAPIEnabled',updatedConfig?.isMemoryAPIEnabled)
            }
            if(updatedConfig?.isResourceAPIEnbaled != null)
            {
                config.put('isResourceAPIEnbaled',updatedConfig?.isResourceAPIEnbaled)
            }

            if(updatedConfig?.isNavigationAPIEnabled != null)
            {
                config.put('isNavigationAPIEnabled',updatedConfig?.isNavigationAPIEnabled)
            }
            if(updatedConfig?.isDOMNeeded != null)
            {
                config.put('isDOMNeeded',updatedConfig?.isDOMNeeded)
            }

            if(updatedConfig?.isMarkAPIEnabled != null)
            {
                config.put('isMarkAPIEnabled',updatedConfig?.isMarkAPIEnabled)
            }

            if(updatedConfig?.isResourceCrawlingEnabled != null)
            {
                config.put('isResourceCrawlingEnabled',updatedConfig?.isResourceCrawlingEnabled)
            }

            if(updatedConfig?.imageResourceExtension != null)
            {
                config.put('imageResourceExtension',updatedConfig?.imageResourceExtension)
            }
            if(updatedConfig?.staticResourceExtension != null)
            {
                config.put('staticResourceExtension',updatedConfig?.staticResourceExtension)
            }
            if(updatedConfig?.runIntervalInMinutes != null)
            {
                config.put('runIntervalInMinutes',updatedConfig?.runIntervalInMinutes)
            }

            if(updatedConfig?.defaultTransactionThreshold != null)
            {
                config.put('defaultTransactionThreshold',updatedConfig?.defaultTransactionThreshold)
            }

            if(updatedConfig?.resourceDurationThreshold != null)
            {
                config.put('resourceDurationThreshold',updatedConfig?.resourceDurationThreshold)
            }

            if(updatedConfig?.excludedGeneriRules != null)
            {
                config.put('excludedGeneriRules',updatedConfig?.excludedGeneriRules)
            }
            if(updatedConfig?.excludedSoftNavigationRules != null)
            {
                config.put('excludedSoftNavigationRules',updatedConfig?.excludedSoftNavigationRules)
            }
            if(updatedConfig?.excludedCBTRules != null)
            {
                config.put('excludedCBTRules',updatedConfig?.excludedCBTRules)
            }
            if(updatedConfig?.samplesCount != null)
            {
                config.put('samplesCount',updatedConfig?.samplesCount)
            }

            if(updatedConfig?.excludedMobileRules != null)
            {
                config.put('excludedMobileRules',updatedConfig?.excludedMobileRules)
            }


            if(updatedConfig?.isCPUEnabled != null)
            {
                config.put('isCPUEnabled',updatedConfig?.isCPUEnabled)
            }

            if(updatedConfig?.isMemoryEnabled != null)
            {
                config.put('isMemoryEnabled',updatedConfig?.isMemoryEnabled)
            }

            if(updatedConfig?.excludedNativeAppRules != null)
            {
                config.put('excludedNativeAppRules',updatedConfig?.excludedNativeAppRules)
            }

            if(updatedConfig?.resourceRTThreshold != null)
            {
                config.put('resourceRTThreshold',updatedConfig?.resourceRTThreshold)
            }

            if(updatedConfig?.totalPayloadThreshold != null)
            {
                config.put('totalPayloadThreshold',updatedConfig?.totalPayloadThreshold)
            }

            if(updatedConfig?.resourcePayloadThreshold != null)
            {
                config.put('resourcePayloadThreshold',updatedConfig?.resourcePayloadThreshold)
            }

            if(updatedConfig?.userPerceivedSLA != null)
            {
                config.put('userPerceivedSLA',updatedConfig?.userPerceivedSLA)
            }

            if(updatedConfig?.transactionSLA != null)
            {
                def txnMap = [:]
                updatedConfig?.transactionSLA.each{k,v ->
                    txnMap.put(k,v)
                }
                config.put('transactionSLA',txnMap)
            }

            if(updatedConfig?.Rules != null)
            {
                boolean priorityFlag = true
                if(updatedConfig?.Rules?.genericRules != null)
                {

                    if(updatedConfig?.Rules?.genericRules.sum{it.weight} == 100)
                    {
                        config.Rules.genericRules.removeAll{it}
                        updatedConfig?.Rules?.genericRules.each{it->
                            if(it.priority != 'Low' && it.priority != 'Medium' && it.priority != 'High')
                            {
                                priorityFlag = false
                            }
                            config.Rules.genericRules.add(it)
                        }
                    }
                    else
                    {
                        updateStatus.put('Comment','Ignored Generic Rules Update since sum of weight is not equal to 100')
                    }

                    if(!priorityFlag)
                    {
                        updateStatus.put('Comment','Ignored Generic Rules Update since priority allowed for rules are only Low or Medium or High')
                    }

                }

                priorityFlag = true
                if(updatedConfig?.Rules?.softNavigationRules != null)
                {
                    if(updatedConfig?.Rules?.softNavigationRules.sum{it.weight} == 100)
                    {
                        config.Rules.softNavigationRules.removeAll{it}
                        updatedConfig?.Rules?.softNavigationRules.each{it->
                            if(it.priority != 'Low' && it.priority != 'Medium' && it.priority != 'High')
                            {
                                priorityFlag = false
                            }
                            config.Rules.softNavigationRules.add(it)
                        }
                    }
                    else
                    {
                        if (updateStatus.get('Comment') != null)
                        {
                            updateStatus.put('Comment', updateStatus.get('Comment') + ' and Ignored Soft Navigation Rules Update since sum of weight is not equal to 100')
                        }
                        else
                        {
                            updateStatus.put('Comment', 'Ignored Soft Navigation Rules Update since sum of weight is not equal to 100')
                        }
                    }

                    if(!priorityFlag)
                    {
                        if (updateStatus.get('Comment') != null) {
                            updateStatus.put('Comment', updateStatus.get('Comment') + ' and priority allowed for Soft rules are only Low or Medium or High')
                        }
                        else
                        {
                            updateStatus.put('Comment', 'Ignored Soft Navigation Rules Update since priority allowed for rules are only Low or Medium or High')
                        }
                    }
                }

            }

            def update = ElasticSearchUtils.elasticSearchPOSTWithMap(esUrl,GlobalConstants.CONFIGTABLE_WS + id,config)
            if(update != null&& update?.result == 'updated')
            {
                updateStatus.put('Status','0')
            }
            else
            {
                updateStatus.put('Status','1')
                updateStatus.put('Reason','Failed to update config in data source')
            }

        }
        else
        {
            updateStatus = null
        }
        return updateStatus

    }

    static String checkHealth(def url,def path)
    {
        def response = null
        response = ElasticSearchUtils.elasticSearchGET(url,path)
        if(response != null)
        {
            return 'UP'
        }
        else
        {
            return 'DOWN'
        }

    }

    static boolean createStandardUsers(String esUrl)
    {
        boolean chkFlag = true
        JSONObject json = new JSONObject('{"UserName":"cxopuser","Password":"zxcvbnm","Role":"USER"}')
        def status = LoginUtils.CreateUser(esUrl,json).toString()
        if(status.contains("false"))
        {
            chkFlag = false
        }

        json = new JSONObject('{"UserName":"cxopadmin","Password":"qwertyuiop","Role":"ADMIN"}')
        status = LoginUtils.CreateUser(esUrl,json).toString()
        if(status.contains("false"))
        {
            chkFlag = false
        }

        return chkFlag


    }

    static Map<String,Object> isConfigExists(String clientName,String projectName,String scenario,String esUrl,boolean runDetails = false)
    {
        def retVal = [:]
        def configExists = ElasticSearchUtils.isConfigExists(clientName,projectName,scenario,esUrl)
        if(configExists != null && configExists?.hits?.hits?.size() > 0)
        {
            retVal.put('Status','true')
            retVal.put('staticResourceExtension',configExists?.hits?.hits[0]?.'_source'?.staticResourceExtension)
            retVal.put('imageResourceExtension',configExists?.hits?.hits[0]?.'_source'?.imageResourceExtension)
            retVal.put('resourceDurationThreshold',configExists?.hits?.hits[0]?.'_source'?.resourceDurationThreshold == null ? '10' : configExists?.hits?.hits[0]?.'_source'?.resourceDurationThreshold)
            if(runDetails)
            {
                retVal.put('RunID',configExists?.hits?.hits[0]?.'_source'?.RunID == null ? '0' : configExists?.hits?.hits[0]?.'_source'?.RunID)
                retVal.put('RunTime',configExists?.hits?.hits[0]?.'_source'?.runTimestamp == null ? System.currentTimeMillis() : configExists?.hits?.hits[0]?.'_source'?.runTimestamp)
                retVal.put('Release',configExists?.hits?.hits[0]?.'_source'?.Release)
                retVal.put('BuildNumber',configExists?.hits?.hits[0]?.'_source'?.BuildNumber)

            }

        }
        else
        {
            retVal.put('Status','false')
        }
        return retVal

    }

    static List<Object> getTransactionDetails(args)
    {
        LOGGER.debug('Entering getTransactionDetails')
        def configFlag = 'false'
        def transactionMap = [:]
        args.put('AnalysisType','Run')
        def configReader = CommonUtils.getConfiguration(args)
        LOGGER.debug('CONFIGURATION DETAILS for {} {} {} : {}',args.ClientName,args.ProjectName,args.Scenario,configReader)
        if (configReader != null)
        {
            configFlag = 'true'

            configReader.transactionSLA.each{it ->
                transactionMap.put(it.key,it.value)
            }

            ElasticSearchUtils.uniqueTransactionList(configReader)?.aggregations?.langs?.buckets?.each{it->
                if(!transactionMap.containsKey(it.key))
                {
                    transactionMap.put(it.key,configReader.defaultTransactionThreshold)
                }

            }

        }
        else
        {
            transactionMap.put('ClientName',args.ClientName)
            transactionMap.put('ProjectName',args.ProjectName)
            transactionMap.put('ScenarioName',args.Scenario)
            transactionMap.put('Reason','No Configuration Available')
        }
        LOGGER.debug('Exiting getTransactionDetails')

        return [transactionMap,configFlag]
    }

    static List<Object> deleteStats(args)
    {
        LOGGER.debug('Entering deleteStats')
        def configFlag = 'false'
        def status = [:]
        args.put('AnalysisType','Run')
        status.put('ClientName',args.ClientName)
        status.put('ProjectName',args.ProjectName)
        status.put('ScenarioName',args.Scenario)
        def configReader = CommonUtils.getConfiguration(args)
        LOGGER.debug('CONFIGURATION DETAILS for {} {} {} : {}',args.ClientName,args.ProjectName,args.Scenario,configReader)
        if (configReader != null)
        {
            configFlag = 'true'
            def res = ElasticSearchUtils.deleteStats(GlobalConstants.ESUrl,args.ClientName,args.ProjectName,args.Scenario,args?.RunID)
            if(res?.deleted > 0)
            {
                status.put('Reason','Deleted Successfully')
                status.put('Status','success')
            }
            else
            {
                status.put('Reason','Deleted Failed or No records available for the filter condition provided')
            }

        }
        else
        {

            status.put('Reason','No Configuration Available')
        }
        LOGGER.debug('Exiting deleteStats')

        return [status,configFlag]
    }



}