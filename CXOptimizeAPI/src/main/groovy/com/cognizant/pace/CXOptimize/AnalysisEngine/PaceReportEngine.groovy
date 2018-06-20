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

class PaceReportEngine
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PaceReportEngine.class)
    static def getHTMLContent(def configReader,def jsonString)
    {
        LOGGER.debug 'getHTMLContent JSON String : ' +  jsonString

        def jsonObj = new JSONObject(jsonString)
        def analysisMap = JsonUtils.jsonToMap(jsonObj)

        StringBuilder htmlTable = new StringBuilder()
        float devPctComparison = 0.00f
        float devPctSLA = 0.00f
        def recommendationList = []
        def threshold = (configReader.ragThresold ? configReader.ragThresold : '20,10,-5')
        List<String> thresholdValues = Arrays.asList(threshold.split("\\s*,\\s*"));
        //05/13 added to avoid NULL pointer exception when analysis doesnt have any transaction data
        def id = null
        def rules = PaceRuleEngine.getGeneralRules(null)
        def softRules = PaceRuleEngine.getSoftNavigationRules(null)
        def strPriority = null
        def ruleCnt = 0
        if (analysisMap.TransactionMetrics.size > 0)
        {
            htmlTable.append('<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js"></script><script>function drawShape(){for(var l=document.getElementsByTagName("Canvas"),t=l.length,e=0;t>e;e++){var i=document.getElementById(l[e].id),f=i.getContext("2d");l[e].id.includes("medium")&&(f.fillStyle="orange",f.fillRect(0,0,40,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left",f.fillText("Medium",0,10)),l[e].id.includes("high")&&(f.fillStyle="red",f.fillRect(0,0,25,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left",f.fillText("High",0,10)),l[e].id.includes("low")&&(f.fillStyle="green",f.fillRect(0,0,22,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left",f.fillText("Low",0,10)),"A"==l[e].id.charAt(0)&&(f.fillStyle="#009900",f.fillRect(0,0,25,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left","1"==l[e].id.charAt(1)?f.fillText(l[e].id.substring(1,4).concat("%"),0,10):f.fillText(l[e].id.substring(1,3).concat("%"),0,10)),"B"==l[e].id.charAt(0)&&(f.fillStyle="#99ff33",f.fillRect(0,0,25,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left",f.fillText(l[e].id.substring(1,3).concat("%"),0,10)),"C"==l[e].id.charAt(0)&&(f.fillStyle="#ffcc00",f.fillRect(0,0,25,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left",f.fillText(l[e].id.substring(1,3).concat("%"),0,10)),"D"==l[e].id.charAt(0)&&(f.fillStyle="#ff751a",f.fillRect(0,0,25,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left",f.fillText(l[e].id.substring(1,3).concat("%"),0,10)),"E"==l[e].id.charAt(0)&&(f.fillStyle="#ff3333",f.fillRect(0,0,25,12),f.fillStyle="white",f.font="8pt sans-serif",f.textAlign="left",f.fillText(l[e].id.substring(1,3).concat("%"),0,10)),"F" == l[e].id.charAt(0) && (f.fillStyle = "#cc0000", f.fillRect(0, 0, 25, 12), f.fillStyle = "white", f.font = "8pt sans-serif", f.textAlign = "left", "0"==l[e].id.charAt(1)?f.fillText(l[e].id.substring(2,3).concat("%"),0,10):f.fillText(l[e].id.substring(1,3).concat("%"),0,10))}}</script><script type="text/javascript">function showhide(id) {var e = document.getElementById(id);if ($(e).hasClass("expander")) {$(e).removeClass("expander");}else {$(e).addClass("expander");}}</script><style> h1.heading1 {color: #000;font-size: 2.25em;font-weight: bold;font-family: Helvetica;text-align: center;margin:0;text-shadow: 0 1px 0 #ccc, 0 1px 0 #c9c9c9, 0 2px 0 #bbb, 0 2px 0 #b9b9b9, 0 2px 0 #aaa, 0 3px 1px rgba(0,0,0,.1), 0 0 5px rgba(0,0,0,.1), 0 0.5px 1.5px rgba(0,0,0,.3), 0 1.5px 2.5px rgba(0,0,0,.2), 0 2.5px 5px rgba(0,0,0,.25), 0 5px 5px rgba(0,0,0,.2), 0 10px 10px rgba(0,0,0,.15);}.heading2 {  color: #000;font-size: 1em;font-family: Helvetica;height: 12px;text-shadow: 0 1px 0 #ccc, 0 1px 0 #c9c9c9, 0 1px 0 #bbb, 0 2px 0 #b9b9b9, 0 1px 0 #aaa, 0 1px 1px rgba(0,0,0,.1), 0 0 3px rgba(0,0,0,.1), 0 0.5px 1px rgba(0,0,0,.3), 0 1.5px 2.5px rgba(0,0,0,.2), 0 1px 2px rgba(0,0,0,.25), 0 2px 1px rgba(0,0,0,.2), 0 5px 5px rgba(0,0,0,.15);}.container {display: flex;flex-flow: row nowrap; justify-content: space-between;  }div.expander {height: 60px;overflow: hidden;cursor: pointer;}div.scrollable {width: 100%;height: 100%;margin: 0;padding: 0;overflow: auto;}body {background: #fafafa;color: #444;font: 100%/30px \'Helvetica Neue\', helvetica, arial, sans-serif;    }table {border-collapse: collapse;font-size: 12px;table-layout:fixed;word-wrap:break-word;text-align: center;width: 100%;} th {background: linear-gradient(#777, #444);border-left: 1px solid #555;border-right: 1px solid #777;border-top: 1px solid #555;border-bottom: 1px solid #333;box-shadow: inset 0 1px 0 #999;color: #fff;padding: 15px 0px;text-align: center;font-weight: normal;position: relative;}th:first-child {border-left: 1px solid #777;    box-shadow: inset 1px 1px 0 #999;}th:last-child {box-shadow: inset -1px 1px 0 #999;}td {border-right: 1px solid #fff; border-left: 1px solid #e8e8e8;border-top: 1px solid #fff;border-bottom: 1px solid #e8e8e8;padding: 10px 2px;position: relative;transition: all 300ms;}tr:nth-child(odd) td { background: #e9e9e9;  }tr:nth-child(even) td {background: #f1f1f1;  }tbody:hover tr:hover td {color: #000;background: #dddddd}a {text-decoration:none;}</style></head><body onload="drawShape();"><h1 class="heading1">PerfInsight Report</h1>')
            htmlTable.append('<div style="display:inline" class="heading2">Platform : </div>"').append(getPlatform(analysisMap.TransactionMetrics[0].Platform)).append('"<div></div><div style="display:inline;" class="heading2">User Agent : </div> "').append(analysisMap.TransactionMetrics[0].UserAgent).append('"<div></div><div style="display:inline;" class="heading2">Kibana Dashboard :  </div><a href="').append(configReader.kibanaURL).append('" target="_blank">Click Here</a><div class="container"><div class="left"><p style="vertical-align: top;display:inline" class="heading2">Client Name : </p><p style="display:inline">').append(analysisMap.ClientNameOrg).append('</p></div><div class="center"><p style="display:inline" class="heading2">Project Name : </p><p style="display:inline">').append(analysisMap.ProjectNameOrg).append('</p></div><div class="right"><p style="display:inline" class="heading2">Scenario Name : </p><p style="display:inline">').append(analysisMap.ScenarioOrg).append('</p></div></div><table align="right" style="width:50%"><tbody><tr><td style="text-align: center; ; background-color: cc0000; font-weight: bold; color:black">0 <= S < 50</td><td style="text-align: center; ; background-color: ff3333; font-weight: bold; color:black">50 <= S < 60</td><td style="text-align: center; ; background-color: ff751a; font-weight: bold; color:black">60 <= S < 70</td><td style="text-align: center; ; background-color: ffcc00; font-weight: bold; color:black">70 <= S < 80</td><td style="text-align: center; ; background-color: 99ff33; font-weight: bold; color:black">80 <= S < 90</td><td style="text-align: center; ; background-color: 009900; font-weight: bold; color:black">90 <= S < 100</td></tr></tbody></table><table><tbody><tr><th style="width:100px">Transaction</th><th>SLA</th><th>Average</th><th>90 Percentile</th><th>95 Percentile</th><th>Fetch Start Time</th><th>Redirect Time</th><th>Cache Fetch Time</th><th>DNS Lookup Time</th><th>TCP Connect Time</th><th>Server Time</th><th>Download Time</th><th>DOM Processing Time</th><th>OnLoad Time</th><th>Client Time</th><th>Score</br>(S)</th><th style="width:200px">Recommendation</th><th>Baseline RT</th><th>Current RT</th><th>%Deviation</th><th style="width:200px">Analysis</th></tr>' )

            for(int i=0; i < analysisMap.TransactionMetrics.size ;i++)
            {
                htmlTable.append('<tr><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].Name)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].SLA)
                htmlTable.append(PaceReportEngine.getDeviationHTML(thresholdValues,analysisMap.TransactionMetrics[i].totalPageLoadTime.Average.toString(),analysisMap.TransactionMetrics[i].SLA.toString()))
                htmlTable.append(Float.parseFloat(analysisMap.TransactionMetrics[i].totalPageLoadTime.Average.toString()).round())
                htmlTable.append(PaceReportEngine.getDeviationHTML(thresholdValues,analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile".toString(),analysisMap.TransactionMetrics[i].SLA.toString()))
                htmlTable.append(Float.parseFloat(analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile".toString()).round())
                htmlTable.append(PaceReportEngine.getDeviationHTML(thresholdValues,analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString(),analysisMap.TransactionMetrics[i].SLA.toString()))
                htmlTable.append(Float.parseFloat(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).round())
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].fetchStartTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].redirectTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].cacheFetchTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].dnsLookupTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].tcpConnectTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].serverTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].downloadTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].domProcessingTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].onloadTime)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].clientTime)
                htmlTable.append(PaceReportEngine.getScoreColor(analysisMap.TransactionMetrics[i].Score))
                htmlTable.append(analysisMap.TransactionMetrics[i].Score)
                htmlTable.append('/100</td><td style="text-align: right" title="Click to view or hide" onclick="javascript:showhide(&#39;16').append(i.toString()).append('&#39;)"><div id="16').append(i.toString()).append('" class="expander" style="text-align: left">')
                htmlTable.append('<ul>')
                recommendationList.removeAll{it}
                ruleCnt = analysisMap.TransactionMetrics[i].genericRulesRecommendations.size

                if(ruleCnt > 0)
                {
                    for (int j = 0; j < ruleCnt; j++) {
                        id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                        if(analysisMap.TransactionMetrics[i]?.NavType == null || analysisMap.TransactionMetrics[i]?.NavType == 'Hard')
                        {
                            strPriority = rules[id].priority + '##<canvas id="' + rules[id].priority.toLowerCase() + i + id + '" width="' + rules[id].width + '" height="' + rules[id].height + '"></canvas>' + getScoreCanvas(id, i, analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score) + ' ' + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment
                        }
                        else
                        {
                            strPriority = softRules[id].priority + '##<canvas id="' + softRules[id].priority.toLowerCase() + i + id + '" width="' + softRules[id].width + '" height="' + softRules[id].height + '"></canvas>' + getScoreCanvas(id, i, analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score) + ' ' + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment
                        }
                        recommendationList.add(strPriority)
                    }
                    recommendationList.sort();
                    for (int j = 0; j < recommendationList.size(); j++) {
                        htmlTable.append('<li>')
                        htmlTable.append(recommendationList[j].split("\\s*##\\s*")[1])
                        htmlTable.append('</li>')
                    }
                }
                htmlTable.append('</ul>')
                htmlTable.append('</div></td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselineRT)
                htmlTable.append('</td><td>')
                htmlTable.append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentRT)
                htmlTable.append(PaceReportEngine.getDeviationHTML(thresholdValues,analysisMap.TransactionMetrics[i].comparativeAnalysis.deviation.toString()))
                htmlTable.append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviation)
                htmlTable.append('%</td><td style="text-align: right" title="Click to view or hide" onclick="javascript:showhide(&#39;20').append(i.toString()).append('&#39;)"><div id="20').append(i.toString()).append('" class="expander" style="text-align: left">')
                htmlTable.append(analysisMap.TransactionMetrics[i].comparativeAnalysis.analysis)
                htmlTable.append('</div></td></tr>')
            }
            htmlTable.append('</tbody></table></body>')
        }
        else
        {
            htmlTable.append('<html><body><h1>No Transaction Data available for current.Please re-run analysis</h1></body></html>')
        }
        return htmlTable

    }

    static def getPlatform(def platform)
    {
        StringBuilder device = new StringBuilder();
        if(platform == 'Android' || platform == 'Linux armv7l' || platform == 'null')
        {
            device.append('Android(').append( platform).append(')')
        }
        else if (platform.contains('iPhone') || platform.contains('iPod') || platform.contains('iPad') || platform.contains('Mac') || platform.contains('Pike'))
        {
            device.append('Apple(').append(platform).append(')')
        }
        else if (platform.contains('BlackBerry'))
        {
            device.append('BlackBerry(').append(platform).append(')')
        }
        else if (platform.contains('FreeBSD'))
        {
            device.append('FreeBSD(').append(platform).append(')')
        }
        else if (platform.contains('Linux'))
        {
            device.append('Linux(').append(platform).append(')')
        }
        else if (platform.contains('Win') || platform.contains('Pocket') || platform.contains('OS/2'))
        {
            device.append('Windows(').append(platform).append(')')
        }
        else if (platform.contains('Nintendo'))
        {
            device.append('Nintendo(').append(platform).append(')')
        }
        else if (platform.contains('OpenBSD'))
        {
            device.append('OpenBSD(').append(platform).append(')')
        }
        else if (platform.contains('Symbian') || platform.contains('S60') || platform.contains('Nokia'))
        {
            device.append('Symbian(').append(platform).append(')')
        }
        else if (platform.contains('PalmOS') || platform.contains('webOS'))
        {
            device.append('Palm(').append(platform).append(')')
        }
        else if (platform.contains('SunOS'))
        {
            device.append('Solaris(').append(platform).append(')')
        }
        else if (platform.toLowerCase().contains('playstation'))
        {
            device.append('Sony(').append(platform).append(')')
        }
        else
        {
            device.append(platform)
        }
        return device

    }

    static def getBrowserName(def userAgent)
    {
        def name = null
        if(userAgent.contains('MSIE'))
        {
            name = 'Internet Explorer ' + (userAgent.split('MSIE ')[1]).toString().substring(3)
        }
        else if((userAgent.contains('rv') || userAgent.contains('IE 11.0')) && !userAgent.contains('Firefox'))
        {
            name = 'Internet Explorer 11'
        }
        else if(userAgent.contains('Edge'))
        {
            name = 'Edge ' + (userAgent.split('Edge/')[1]).toString().substring(0,2)
        }
        else if(userAgent.contains('Chrome'))
        {
            name = 'Chrome ' + (userAgent.split('Chrome/')[1]).toString().substring(0,12)
        }
        else if(userAgent.contains('Firefox'))
        {
            name = 'Firefox ' + (userAgent.split('Firefox/')[1]).toString().substring(0,3)
        }
        else if(userAgent.contains('Android'))
        {
            name = 'Android Mobile Browser '
        }
        else
        {
            name = userAgent
        }
        return name
    }


    static def getScoreCanvas(def ruleID,def transactionID,def score,def summaryReport = null)
    {
        def priority = 'SS'
        if(summaryReport)
        {
            switch(score)
            {
                case 90..100:
                    priority = 'A' + score + transactionID + ruleID + ',25,12'
                    break
                case 80..89:
                    priority = 'B' + score + transactionID + ruleID + ',25,12'
                    break
                case 70..79:
                    priority = 'C' + score + transactionID + ruleID + ',25,12'
                    break
                case 60..69:
                    priority = 'D' + score + transactionID + ruleID + ',25,12'
                    break
                case 50..59:
                    priority = 'E' + score + transactionID + ruleID + ',25,12'
                    break
                case 0..49:
                    priority = 'F' + (score < 10 ? ('0' + score) : score) + transactionID + ruleID + ',25,12'
                    break
                default:
                    break
            }
        }
        else
        {
            switch(score)
            {
                case 90..100:
                    priority = '<canvas id="A' + score + transactionID + ruleID + '" width="25" height="12"></canvas>'
                    break
                case 80..89:
                    priority = '<canvas id="B' + score + transactionID + ruleID + '" width="25" height="12"></canvas>'
                    break
                case 70..79:
                    priority = '<canvas id="C' + score + transactionID + ruleID + '" width="25" height="12"></canvas>'
                    break
                case 60..69:
                    priority = '<canvas id="D' + score + transactionID + ruleID + '" width="25" height="12"></canvas>'
                    break
                case 50..59:
                    priority = '<canvas id="E' + score + transactionID + ruleID + '" width="25" height="12"></canvas>'
                    break
                case 0..49:
                    priority = '<canvas id="F' + (score < 10 ? ('0' + score) : score) + transactionID + ruleID + '" width="25" height="12"></canvas>'
                    break
                default:
                    break
            }
        }

        return priority
    }

    static def getScoreColor(def score)
    {
        def rowColor
        switch(score)
        {
            case 90..100:
                rowColor = '</td><td style="text-align: center; ; background-color: 009900; font-weight: bold; color:black">'
                break
            case 80..89:
                rowColor = '</td><td style="text-align: center; ; background-color: 99ff33; font-weight: bold; color:black">'
                break
            case 70..79:
                rowColor = '</td><td style="text-align: center; ; background-color: ffcc00; font-weight: bold; color:black">'
                break
            case 60..69:
                rowColor = '</td><td style="text-align: center; ; background-color: ff751a; font-weight: bold; color:black">'
                break
            case 50..59:
                rowColor = '</td><td style="text-align: center; ; background-color: ff3333; font-weight: bold; color:black">'
                break
            case 1..49:
                rowColor = '</td><td style="text-align: center; ; background-color: cc0000; font-weight: bold; color:black">'
                break
            default:
                rowColor = '</td><td>'
                break

        }
        return rowColor
    }


    static def getDeviationHTML(def thresholdValues,String value,String SLA=null)
    {
        String rowColor = '</td><td style="text-align: center; font-weight: bold; color:black">'
        float devPctComparison = 0.00f

        if(SLA == null)
        {
            devPctComparison =  Float.parseFloat(value)
            //White
            if(devPctComparison >= Float.parseFloat(thresholdValues[2]) && devPctComparison < Float.parseFloat(thresholdValues[1]))
            {
            }
            //Amber
            else if(devPctComparison >= Float.parseFloat(thresholdValues[1]) && devPctComparison < Float.parseFloat(thresholdValues[0]))
            {
                rowColor = '</td><td style="text-align: center; ; background-color: FFFF80; font-weight: bold; color:black">'
            }
            //Red
            else if(devPctComparison >= Float.parseFloat(thresholdValues[0]))
            {
                rowColor = '</td><td style="text-align: center; ; background-color: C94C4C; font-weight: bold; color:black">'
            }
            //Green
            else if(devPctComparison < Float.parseFloat(thresholdValues[2]))
            {
                rowColor = '</td><td style="text-align: center; ; background-color: E1FFB7; font-weight: bold; color:black">'
            }
        }
        else
        {
            devPctComparison = Math.round((Float.parseFloat(value) - Float.parseFloat(SLA))/ Float.parseFloat((SLA)) * 10000.00)/100.00
            //White
            if(devPctComparison >= Float.parseFloat(thresholdValues[2]) && devPctComparison < Float.parseFloat(thresholdValues[1]))
            {
            }
            //Amber
            else if(devPctComparison >= Float.parseFloat(thresholdValues[1]) && devPctComparison < Float.parseFloat(thresholdValues[0]))
            {
                rowColor = '</td><td style="text-align: center; ; background-color: FFFF80; font-weight: bold; color:black">'
            }
            //Red
            else if(devPctComparison >= Float.parseFloat(thresholdValues[0]))
            {
                rowColor = '</td><td style="text-align: center; ; background-color: C94C4C; font-weight: bold; color:black">'
            }
            //Green
            else if(devPctComparison < Float.parseFloat(thresholdValues[2]))
            {
                rowColor = '</td><td style="text-align: center; ; background-color: E1FFB7; font-weight: bold; color:black">'
            }
        }
        return rowColor

    }

    static def getJsonString(def configReader,def analysisReport)
    {

        LOGGER.debug 'getJsonString Map : ' +  analysisReport

        def indvTxnSLA = configReader.transactionSLA
        def indvTxnUPSLA = configReader.transactionUPSLA


        StringBuilder jsonStr = new StringBuilder();
        jsonStr.append('{"ClientName":"').append(configReader.ClientName.toString()).append('",')
        jsonStr.append('"ClientNameOrg":"').append(configReader.ClientNameOrg.toString()).append('",')
        jsonStr.append('"ProjectName":"').append(configReader.ProjectName.toString()).append('",')
        jsonStr.append('"ProjectNameOrg":"').append(configReader.ProjectNameOrg.toString()).append('",')
        jsonStr.append('"Scenario":"').append(configReader.Scenario.toString()).append('",')
        jsonStr.append('"ScenarioOrg":"').append(configReader.ScenarioOrg.toString()).append('",')
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            jsonStr.append('"CurrentRunID":"').append(configReader.CurrentRun.toString()).append('",')
            jsonStr.append('"BaselineRunID":"').append(configReader.BaselineRun.toString()).append('",')
        }

        if(configReader.AnalysisType == 'Time')
        {
            jsonStr.append('"CurrentTime":"').append(configReader.CurrentStart).append(' - ').append(configReader.CurrentEnd).append('",')
            jsonStr.append('"BaselineTime":"').append(configReader.BaselineStart).append(' - ').append(configReader.BaselineEnd).append('",')
        }

        int i = 0
        jsonStr.append('"TransactionMetrics":[')
        analysisReport.each{key,value ->
            if (i == 0)
            {
                i++
            }
            else
            {
                jsonStr.append(',')
            }

            jsonStr.append('{"Name":"').append(key).append('",')
            jsonStr.append('"NavType":"').append(value.NavType).append('",')
            jsonStr.append('"Platform":"').append(value.Platform).append('",')
            jsonStr.append('"UserAgent":"').append(value.UserAgent).append('",')
            jsonStr.append('"SLA":"').append((indvTxnSLA?."$key" ? indvTxnSLA."$key" : configReader.defaultTransactionThreshold)).append('",')
            jsonStr.append('"totalPageLoadTime":{"Average":').append(value.Average).append(',')
            jsonStr.append('"Maximum":').append(value?.Max == null ? 0 : value?.Max).append(',')
            jsonStr.append('"Minimum":').append(value?.Min == null ? 0 : value?.Min).append(',')
            jsonStr.append('"90 Percentile":').append(value.Pcnt90).append(',')
            jsonStr.append('"95 Percentile":').append(value.Pcnt95).append(',')
            jsonStr.append('"count":').append(value.Count).append('},')
            jsonStr.append('"resourceLoadTime":').append((value?.resourceLoadTime == null ? 0 : value.resourceLoadTime)).append(',')
            if(value.NavType == 'NativeApp')
            {
                jsonStr.append('"ScreenName":"').append(value.ScreenName).append('",')
                jsonStr.append('"userPerceivedTime":').append(value.userPerceivedTime).append(',')
                jsonStr.append('"totalRequest":').append(value.totalRequest).append(',')
                jsonStr.append('"totalSize":').append(value.totalSize)append(',')
            }
            else if(value.NavType == 'Hard') {
                jsonStr.append('"BrowserName":"').append(value.BrowserName).append('",')
                jsonStr.append('"DeviceType":"').append(value.DeviceType).append('",')
                jsonStr.append('"url":"').append(value.url).append('",')
                jsonStr.append('"fetchStartTime":').append(value.fetchStartTime).append(',')
                jsonStr.append('"redirectTime":').append(value.redirectTime).append(',')
                jsonStr.append('"cacheFetchTime":').append(value.cacheFetchTime).append(',')
                jsonStr.append('"dnsLookupTime":').append(value.dnsLookupTime).append(',')
                jsonStr.append('"tcpConnectTime":').append(value.tcpConnectTime).append(',')
                jsonStr.append('"downloadTime":').append(value.downloadTime).append(',')
                jsonStr.append('"domProcessingTime":').append(value.domProcessingTime).append(',')
                jsonStr.append('"onloadTime":').append(value.onloadTime).append(',')
                jsonStr.append('"serverTime":').append(value.serverTime).append(',')
                jsonStr.append('"clientTime":').append(value.clientTime).append(',')
                jsonStr.append('"speedIndex":').append((value?.speedIndex == null ? 0 : value.speedIndex)).append(',')
                jsonStr.append('"ttfbUser":').append((value?.ttfbUser == null ? 0 : value.ttfbUser)).append(',')
                jsonStr.append('"ttfbBrowser":').append((value?.ttfbBrowser == null ? 0 : value.ttfbBrowser)).append(',')
                jsonStr.append('"ttfpUser":').append((value?.ttfpUser == null ? 0 : value.ttfpUser)).append(',')
                jsonStr.append('"ttfpBrowser":').append((value?.ttfpBrowser == null ? 0 : value.ttfpBrowser)).append(',')
                jsonStr.append('"clientProcessing":').append((value?.clientProcessing == null ? 0 : value.clientProcessing)).append(',')
                jsonStr.append('"resourceBlockTime":').append((value?.resourceBlockTime == null ? 0 : value.resourceBlockTime)).append(',')
                jsonStr.append('"resourceCount":').append((value?.resourceCount == null ? 0 : value.resourceCount)).append(',')
                jsonStr.append('"resourceSize":').append((value?.resourceSize == null ? 0 : value.resourceSize)).append(',')
                jsonStr.append('"domInteractive":').append((value?.domInteractive == null ? 0 : value.domInteractive)).append(',')
            }
            else {
                jsonStr.append('"BrowserName":').append(value.BrowserName).append(',')
                jsonStr.append('"DeviceType":').append(value.DeviceType).append(',')
                jsonStr.append('"url":"').append(value.url).append('",')
                jsonStr.append('"clientProcessing":').append((value?.clientProcessing == null ? 0 : value.clientProcessing)).append(',')
                jsonStr.append('"resourceBlockTime":').append((value?.resourceBlockTime == null ? 0 : value.resourceBlockTime)).append(',')
                jsonStr.append('"resourceCount":').append((value?.resourceCount == null ? 0 : value.resourceCount)).append(',')
                jsonStr.append('"resourceSize":').append((value?.resourceSize == null ? 0 : value.resourceSize)).append(',')
            }
            jsonStr.append('"visuallyComplete":').append((value?.visuallyComplete == null ? 0 : value.visuallyComplete)).append(',')
            if(configReader?.isMarkAPIEnabled != null && configReader?.isMarkAPIEnabled && value?.markEvents != null)
            {
                jsonStr.append('"markTimings":[')
                int mapSize = value.markEvents.size()
                int cnt = 0
                value.markEvents.each{k,v ->
                    if(cnt == (mapSize - 1))
                    {
                        jsonStr.append('{"').append(k).append('":').append(v).append('}')
                    }
                    else
                    {
                        jsonStr.append('{"').append(k).append('":').append(v).append('},')
                    }
                    cnt = cnt + 1

                }
                jsonStr.append('],')
            }
            jsonStr.append('"backendAnalysis":[').append(value.backendAnalysis).append('],')
            jsonStr.append('"score":').append(value.score).append(',')
            jsonStr.append('"genericRulesRecommendations":[').append(value.recommendation).append(']').append(',')
            jsonStr.append('"comparativeAnalysis":{"baselineRT":').append(value.previous).append(',')
            jsonStr.append('"currentRT":').append(value.current).append(',')
            jsonStr.append('"deviation":').append(value.deviation).append(',')
            jsonStr.append('"currentHC":').append(value.currentHC).append(',')
            jsonStr.append('"baselineHC":').append(value.previousHC).append(',')
            jsonStr.append('"deviationHC":').append(value.devHC).append(',')
            jsonStr.append('"currentPld":').append(value.currentPayload).append(',')
            jsonStr.append('"baselinePld":').append(value.previousPayload).append(',')
            jsonStr.append('"deviationPld":').append(value.devPayload).append(',')
            jsonStr.append('"analysis":"').append(value.comparativeAnalysis).append('"}}')

        }

        jsonStr.append(']}')
        return jsonStr

    }

    static def getJsonStringForAllSamples(def configReader,def txnName,def runID,def arrList)
    {
        LOGGER.debug 'getJsonString ARR LIST : ' +  arrList
        List<String> items = null

        def indvTxnSLA = configReader.transactionSLA
        int txnSLA = 0

        txnSLA = (indvTxnSLA?."$txnName" ? indvTxnSLA."$txnName" : configReader.defaultTransactionThreshold)

        StringBuilder jsonStr = new StringBuilder();
        jsonStr.append('{')
        jsonStr.append('"ClientName":"')
        jsonStr.append(configReader.ClientName.toString())
        jsonStr.append('",')
        jsonStr.append('"ClientNameOrg":"')
        jsonStr.append(configReader.ClientNameOrg.toString())
        jsonStr.append('",')
        jsonStr.append('"ProjectName":"')
        jsonStr.append(configReader.ProjectName.toString())
        jsonStr.append('",')
        jsonStr.append('"ProjectNameOrg":"')
        jsonStr.append(configReader.ProjectNameOrg.toString())
        jsonStr.append('",')
        jsonStr.append('"Scenario":"')
        jsonStr.append(configReader.Scenario.toString())
        jsonStr.append('",')
        jsonStr.append('"ScenarioOrg":"')
        jsonStr.append(configReader.ScenarioOrg.toString())
        jsonStr.append('",')

        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            jsonStr.append('"CurrentRunID":"')
            jsonStr.append(runID.toString())
            jsonStr.append('",')
            jsonStr.append('"Duration":"')
            jsonStr.append(ElasticSearchUtils.execTimeOfRunID(configReader,runID.toString()))
            jsonStr.append('",')
        }

        if(configReader.AnalysisType == 'Time')
        {
            jsonStr.append('"CurrentRunID":"')
            jsonStr.append(configReader.BaselineTime).append(' - ').append(configReader.CurrentTime)
            jsonStr.append('",')
            jsonStr.append('"Duration":"')
            jsonStr.append(configReader.BaselineTime).append(' - ').append(configReader.CurrentTime)
            jsonStr.append('",')
        }

        jsonStr.append('"Transaction":"')
        jsonStr.append(txnName)
        jsonStr.append('",')
        jsonStr.append('"SLA":"')
        jsonStr.append(txnSLA)
        jsonStr.append('",')
        jsonStr.append('"TransactionMetrics":[')
        for(int i=0; i < arrList.size ;i++)
        {
            if (i == 0)
            {
            }
            else
            {
                jsonStr.append(',')
            }
            //items =  Arrays.asList(arrList[i].toString().split("\\s*##\\s*"));
            jsonStr.append('{"Time":"')
            jsonStr.append(Date.parse("yyyyMMdd'T'hhmmss.SSS", arrList[i].StartTime.toString()).format("HH:mm:ss").toString()).append('",')
            jsonStr.append('"Platform":"').append(arrList[i].Platform).append('",')
            jsonStr.append('"UserAgent":"').append(arrList[i].UserAgent).append('",')
            jsonStr.append('"BrowserName":"').append(arrList[i]?.BrowserName).append('",')
            jsonStr.append('"DeviceType":"').append(arrList[i]?.DeviceType).append('",')
            jsonStr.append('"totalPageLoadTime":').append(arrList[i].totalPageLoadTime).append(',')
            jsonStr.append('"resourceLoadTime":').append(arrList[i].resourceLoadTime).append(',')
            jsonStr.append('"resourceBlockTime":').append(arrList[i].resourceBlockTime).append(',')
            if(arrList[i].NavType == 'NativeApp')
            {
                jsonStr.append('"userPerceivedTime":').append(arrList[i].userPerceivedTime).append(',')
                jsonStr.append('"ScreenName":').append(arrList[i].ScreenName).append(',')
                jsonStr.append('"totalSize":').append(arrList[i].totalSize).append(',')
            }
            if(arrList[i].NavType == 'Hard')
            {
                jsonStr.append('"fetchStartTime":').append(arrList[i].fetchStartTime).append(',')
                jsonStr.append('"redirectTime":').append(arrList[i].redirectTime).append(',')
                jsonStr.append('"cacheFetchTime":').append(arrList[i].cacheFetchTime).append(',')
                jsonStr.append('"dnsLookupTime":').append(arrList[i].dnsLookupTime).append(',')
                jsonStr.append('"tcpConnectTime":').append(arrList[i].tcpConnectTime).append(',')
                jsonStr.append('"serverTime":').append(arrList[i].serverTime).append(',')
                jsonStr.append('"downloadTime":').append(arrList[i].downloadTime).append(',')
                jsonStr.append('"domProcessingTime":').append(arrList[i].domProcessingTime).append(',')
                jsonStr.append('"onloadTime":').append(arrList[i].onloadTime).append(',')
                jsonStr.append('"domElementCount":').append(arrList[i].domCount).append(',')
                jsonStr.append('"speedIndex":').append(arrList[i].speedIndex).append(',')
                jsonStr.append('"ttfbUser":').append(arrList[i].ttfbUser).append(',')
                jsonStr.append('"ttfbBrowser":').append(arrList[i].ttfbBrowser).append(',')
                jsonStr.append('"ttfpUser":').append(arrList[i].ttfpUser).append(',')
                jsonStr.append('"ttfpBrowser":').append(arrList[i].ttfpBrowser).append(',')
                jsonStr.append('"domInteractive":').append(arrList[i].domInteractive).append(',')

            }
            jsonStr.append('"visuallyComplete":').append(arrList[i].visuallyComplete).append(',')
            jsonStr.append('"clientProcessing":').append(arrList[i].clientProcessing).append(',')
            jsonStr.append('"resrCount":').append(arrList[i].resourceCount).append(',')
            jsonStr.append('"resrSize":').append(arrList[i].resourceSize).append(',')
            jsonStr.append('"score":').append(arrList[i].score).append(',')
            jsonStr.append('"genericRulesRecommendations":[').append(arrList[i].recommendation).append(']}')
        }
        jsonStr.append(']}')

        def jsonObj = new JSONObject(jsonStr.toString())
        def allSamples = JsonUtils.jsonToMap(jsonObj)

        def id
        def ruleIDs = []
        def nativeRules = null
        def rules = PaceRuleEngine.getGeneralRules(null,configReader)
        def softRules = PaceRuleEngine.getSoftNavigationRules(null,configReader)
        if(configReader?.isNativeApp != null && configReader?.isNativeApp == true) {
            nativeRules = PaceRuleEngine.getNativeAppRules(null, configReader)
        }
        def ruleCnt = 0

        for(int i=0; i < allSamples.TransactionMetrics.size ;i++)
        {
            ruleCnt = allSamples.TransactionMetrics[i].genericRulesRecommendations.size
            if(ruleCnt > 0)
            {
                for(int j=0; j < ruleCnt ;j++)
                {
                    id = allSamples.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                    ruleIDs.add(id)
                    if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
                    {
                        allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvPriority", nativeRules[id].priority.toLowerCase() + i + id + ',' + nativeRules[id].width + ',' + nativeRules[id].height)
                        allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvScore", getScoreCanvas(id, i, allSamples.TransactionMetrics[i].genericRulesRecommendations[j].score, true))
                    }
                    else
                    {
                        if (allSamples.TransactionMetrics[i].fetchStartTime == '-1')
                        {
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvPriority", softRules[id].priority.toLowerCase() + i + id + ',' + softRules[id].width + ',' + softRules[id].height)
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvScore", getScoreCanvas(id, i, allSamples.TransactionMetrics[i].genericRulesRecommendations[j].score, true))

                        }
                        else
                        {
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvPriority", rules[id].priority.toLowerCase() + i + id + ',' + rules[id].width + ',' + rules[id].height)
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvScore", getScoreCanvas(id, i, allSamples.TransactionMetrics[i].genericRulesRecommendations[j].score, true))
                        }

                    }
                }
            }

        }

        return JsonOutput.toJson(allSamples)

    }

    static def getJsonStringForAllSamplesInSeconds(def configReader,def txnName,def runID,def arrList)
    {
        LOGGER.debug 'getJsonString ARR LIST : ' +  arrList
        List<String> items = null

        def indvTxnSLA = configReader.transactionSLA
        int txnSLA = 0

        txnSLA = (indvTxnSLA?."$txnName" ? indvTxnSLA."$txnName" : configReader.defaultTransactionThreshold)

        StringBuilder jsonStr = new StringBuilder();
        jsonStr.append('{')
        jsonStr.append('"ClientName":"')
        jsonStr.append(configReader.ClientName.toString())
        jsonStr.append('",')
        jsonStr.append('"ClientNameOrg":"')
        jsonStr.append(configReader.ClientNameOrg.toString())
        jsonStr.append('",')
        jsonStr.append('"ProjectName":"')
        jsonStr.append(configReader.ProjectName.toString())
        jsonStr.append('",')
        jsonStr.append('"ProjectNameOrg":"')
        jsonStr.append(configReader.ProjectNameOrg.toString())
        jsonStr.append('",')
        jsonStr.append('"Scenario":"')
        jsonStr.append(configReader.Scenario.toString())
        jsonStr.append('",')
        jsonStr.append('"ScenarioOrg":"')
        jsonStr.append(configReader.ScenarioOrg.toString())
        jsonStr.append('",')

        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            jsonStr.append('"CurrentRunID":"')
            jsonStr.append(runID.toString())
            jsonStr.append('",')
            jsonStr.append('"Duration":"')
            jsonStr.append(ElasticSearchUtils.execTimeOfRunID(configReader,runID.toString()))
            jsonStr.append('",')
        }

        if(configReader.AnalysisType == 'Time')
        {
            jsonStr.append('"CurrentRunID":"')
            jsonStr.append(configReader.BaselineTime).append(' - ').append(configReader.CurrentTime)
            jsonStr.append('",')
            jsonStr.append('"Duration":"')
            jsonStr.append(configReader.BaselineTime).append(' - ').append(configReader.CurrentTime)
            jsonStr.append('",')
        }

        jsonStr.append('"Transaction":"')
        jsonStr.append(txnName)
        jsonStr.append('",')
        jsonStr.append('"SLA":"')
        jsonStr.append(txnSLA)
        jsonStr.append('",')
        jsonStr.append('"TransactionMetrics":[')
        for(int i=0; i < arrList.size ;i++)
        {
            if (i == 0)
            {
            }
            else
            {
                jsonStr.append(',')
            }
            //items =  Arrays.asList(arrList[i].toString().split("\\s*##\\s*"));
            jsonStr.append('{"Time":"')
            jsonStr.append(Date.parse("yyyyMMdd'T'hhmmss.SSS", arrList[i].StartTime.toString()).format("HH:mm:ss").toString()).append('",')
            jsonStr.append('"Platform":"').append(arrList[i].Platform).append('",')
            jsonStr.append('"UserAgent":"').append(arrList[i].UserAgent).append('",')
            jsonStr.append('"BrowserName":"').append(arrList[i]?.BrowserName).append('",')
            jsonStr.append('"DeviceType":"').append(arrList[i]?.DeviceType).append('",')
            jsonStr.append('"totalPageLoadTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].totalPageLoadTime)).append(',')
            jsonStr.append('"resourceLoadTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].resourceLoadTime)).append(',')
            jsonStr.append('"resourceBlockTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].resourceBlockTime)).append(',')
            if(arrList[i].NavType == 'NativeApp')
            {
                jsonStr.append('"userPerceivedTime":').append(arrList[i].userPerceivedTime).append(',')
                jsonStr.append('"ScreenName":').append(arrList[i].ScreenName).append(',')
                jsonStr.append('"totalSize":').append(arrList[i].totalSize).append(',')
            }
            if(arrList[i].NavType == 'Hard')
            {
                jsonStr.append('"fetchStartTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].fetchStartTime)).append(',')
                jsonStr.append('"redirectTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].redirectTime)).append(',')
                jsonStr.append('"cacheFetchTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].cacheFetchTime)).append(',')
                jsonStr.append('"dnsLookupTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].dnsLookupTime)).append(',')
                jsonStr.append('"tcpConnectTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].tcpConnectTime)).append(',')
                jsonStr.append('"serverTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].serverTime)).append(',')
                jsonStr.append('"downloadTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].downloadTime)).append(',')
                jsonStr.append('"domProcessingTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].domProcessingTime)).append(',')
                jsonStr.append('"onloadTime":').append(CommonUtils.convertMilliToSeconds(arrList[i].onloadTime)).append(',')
                jsonStr.append('"domElementCount":').append(CommonUtils.convertMilliToSeconds(arrList[i].domCount)).append(',')
                jsonStr.append('"speedIndex":').append(CommonUtils.convertMilliToSeconds(arrList[i].speedIndex)).append(',')
                jsonStr.append('"ttfbUser":').append(CommonUtils.convertMilliToSeconds(arrList[i].ttfbUser)).append(',')
                jsonStr.append('"ttfbBrowser":').append(CommonUtils.convertMilliToSeconds(arrList[i].ttfbBrowser)).append(',')
                jsonStr.append('"ttfpUser":').append(CommonUtils.convertMilliToSeconds(arrList[i].ttfpUser)).append(',')
                jsonStr.append('"ttfpBrowser":').append(CommonUtils.convertMilliToSeconds(arrList[i].ttfpBrowser)).append(',')
                jsonStr.append('"domInteractive":').append(CommonUtils.convertMilliToSeconds(arrList[i].domInteractive)).append(',')

            }
            jsonStr.append('"visuallyComplete":').append(CommonUtils.convertMilliToSeconds(arrList[i].visuallyComplete)).append(',')
            jsonStr.append('"clientProcessing":').append(CommonUtils.convertMilliToSeconds(arrList[i].clientProcessing)).append(',')
            jsonStr.append('"resrCount":').append(arrList[i].resourceCount).append(',')
            jsonStr.append('"resrSize":').append(arrList[i].resourceSize).append(',')
            jsonStr.append('"score":').append(arrList[i].score).append(',')
            jsonStr.append('"genericRulesRecommendations":[').append(arrList[i].recommendation).append(']}')
        }
        jsonStr.append(']}')

        def jsonObj = new JSONObject(jsonStr.toString())
        def allSamples = JsonUtils.jsonToMap(jsonObj)

        def id
        def ruleIDs = []
        def rules = PaceRuleEngine.getGeneralRules(null,configReader)
        def softRules = PaceRuleEngine.getSoftNavigationRules(null,configReader)
        def nativeRules = PaceRuleEngine.getNativeAppRules(null,configReader)
        def ruleCnt = 0

        for(int i=0; i < allSamples.TransactionMetrics.size ;i++)
        {
            ruleCnt = allSamples.TransactionMetrics[i].genericRulesRecommendations.size
            if(ruleCnt > 0)
            {
                for(int j=0; j < ruleCnt ;j++)
                {
                    id = allSamples.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                    ruleIDs.add(id)
                    if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
                    {
                        allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvPriority", nativeRules[id].priority.toLowerCase() + i + id + ',' + nativeRules[id].width + ',' + nativeRules[id].height)
                        allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvScore", getScoreCanvas(id, i, allSamples.TransactionMetrics[i].genericRulesRecommendations[j].score, true))
                    }
                    else
                    {
                        if (allSamples.TransactionMetrics[i].fetchStartTime == '-1')
                        {
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvPriority", softRules[id].priority.toLowerCase() + i + id + ',' + softRules[id].width + ',' + softRules[id].height)
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvScore", getScoreCanvas(id, i, allSamples.TransactionMetrics[i].genericRulesRecommendations[j].score, true))

                        }
                        else
                        {
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvPriority", rules[id].priority.toLowerCase() + i + id + ',' + rules[id].width + ',' + rules[id].height)
                            allSamples.TransactionMetrics[i].genericRulesRecommendations[j].put("canvScore", getScoreCanvas(id, i, allSamples.TransactionMetrics[i].genericRulesRecommendations[j].score, true))
                        }

                    }
                }
            }

        }

        return JsonOutput.toJson(allSamples)

    }

    static def getRuleSummaryJSON(def ruleIDs,def ruleTxnMapping,def ruleType,def configReader)
    {
        StringBuilder rulesRow = new StringBuilder()
        def rules = null
        if(ruleType == 'Hard')
        {
            rules = PaceRuleEngine.getGeneralRules(null,configReader)
        }
        else if (ruleType == 'HardOld')
        {
            rules = PaceRuleEngine.getGeneralRulesOld(null)
        }
        else if (ruleType == 'Soft')
        {
            rules = PaceRuleEngine.getSoftNavigationRules(null,configReader)
        }
        else
        {
            rules = PaceRuleEngine.getNativeAppRules(null,configReader)
        }

        rulesRow.append('"rulesSummary" : [')
        def uniqRuleIDs = (ruleIDs.countBy {it})
        def counter = 0
        if(uniqRuleIDs.size() > 0)
        {
            uniqRuleIDs.each
                    {ruleID, count ->
                        rulesRow.append('{"priority" : "').append(rules[ruleID.toString()].priority).append('","Rule" : "').append(rules[ruleID.toString()].desc).append('","count" : ').append(count).append(',"txnList" : "').append(getTransactionListByRule(ruleTxnMapping,ruleID + '#')).append('"}')
                        counter = counter + 1
                        if(counter == uniqRuleIDs.size())
                        {
                            rulesRow.append(']')
                        }
                        else
                        {
                            rulesRow.append(',')
                        }
                    }

        }
        else
        {
            rulesRow.append(']')
        }

        return rulesRow
    }

    static def getTransactionListByRule(def ruleTxnMapping,def match)
    {
        List<String> items = null
        StringBuilder txnList = new StringBuilder()

        ruleTxnMapping.findAll {it.startsWith(match)}.each
                {
                    items = Arrays.asList(it.split("\\s*#\\s*"))
                    txnList.append('<a>')
                    txnList.append(items[1])
                    txnList.append('</a></br>')
                }

        return txnList
    }


    static def summaryJSON(def configReader,def jsonString)
    {
        LOGGER.debug 'summaryJSON JSON String : ' +  jsonString
        def jsonObj = new JSONObject(jsonString)
        def analysisMap = JsonUtils.jsonToMap(jsonObj)
        StringBuilder summary = new StringBuilder()
        StringBuilder summaryTable = new StringBuilder()
        StringBuilder slaTable = new StringBuilder()
        StringBuilder headerTable = new StringBuilder()
        StringBuilder exectiveSummary = new StringBuilder()


        def id = null
        def ruleIDs = []
        def ruleToTxnMapping = []
        float devPctComparison = 0.00f
        float devPctSLA = 0.00f
        float totalScore = 0.00f
        def httpCalls = 0
        def cmtSize = 0
        def cssBody = 0
        def headJS = 0
        def htmlSize = 0
        def bwSaving = 0
        def ruleCnt = 0
        def threshold = (configReader.ragThresold ? configReader.ragThresold : '20,10,-5')
        List<String> thresholdValues = Arrays.asList(threshold.split("\\s*,\\s*"));
        def rules = PaceRuleEngine.getGeneralRules(null,configReader)
        //def rulesOld = PaceRuleEngine.getGeneralRulesOld()
        def softRules = PaceRuleEngine.getSoftNavigationRules(null,configReader)
        def nativeRules = PaceRuleEngine.getNativeAppRules(null,configReader)
        def runDetails

        def txnCount = (analysisMap.TransactionMetrics?.size == null ? 0 : analysisMap.TransactionMetrics?.size)
        headerTable.append('"header" : {')
        headerTable.append('"ClientName": "').append(analysisMap.ClientNameOrg).append('",')
        headerTable.append('"ProjectName" : "').append(analysisMap.ProjectNameOrg).append('",')
        headerTable.append('"ScenarioName" : "').append(analysisMap.ScenarioOrg).append('"')
        if(configReader?.BaselineStart == null)
        {
            headerTable.append(',"CurrentRunID" : "').append(analysisMap.CurrentRunID).append('",')
            headerTable.append('"BaselineRunID" : "').append(analysisMap.BaselineRunID).append('"')
        }

        if(txnCount > 0)
        {
            if(configReader?.BaselineStart == null)
            {
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap.CurrentRunID)
                headerTable.append(',"CurrentRunDuration" : "').append(runDetails[0]).append('",')
                headerTable.append('"CurrentRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"CurrentBuildNo" : "').append(runDetails[2]).append('",')
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap.BaselineRunID)
                headerTable.append('"BaselineRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"BaselineBuildNo" : "').append(runDetails[2]).append('",')
                headerTable.append('"BaselineRunDuration" : "').append(runDetails[0]).append('"')
            }
            else
            {
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap?.CurrentRunID,'Y','Current')
                headerTable.append(',"CurrentRunDuration" : "').append(runDetails[0]).append('",')
                headerTable.append('"CurrentRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"CurrentBuildNo" : "').append(runDetails[2]).append('",')
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap?.CurrentRunID,'Y','Baseline')
                headerTable.append('"BaselineRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"BaselineBuildNo" : "').append(runDetails[2]).append('",')
                headerTable.append('"BaselineRunDuration" : "').append(runDetails[0]).append('"')
            }

        }
        headerTable.append('}')

        slaTable.append('"Transactions" : [')

        def hardTxnCnt = 0
        /*
        if(analysisMap.TransactionMetrics[0]?.NavType == null)
        {
            for(int i=0; i < txnCount;i++)
            {
                slaTable.append('{"name" : "').append(analysisMap.TransactionMetrics[i].Name).append('",')
                slaTable.append('"platform" : "').append(getPlatform(analysisMap.TransactionMetrics[i].Platform)).append('",')
                slaTable.append('"useragent" : "').append(analysisMap.TransactionMetrics[i].UserAgent).append('",')
                slaTable.append('"browser" : "').append(getBrowserName(analysisMap.TransactionMetrics[i].UserAgent)).append('",')
                slaTable.append('"device" : "').append(getPlatform(analysisMap.TransactionMetrics[i].Platform)).append('",')
                slaTable.append('"sla" : "').append(analysisMap.TransactionMetrics[i].SLA).append('",')
                slaTable.append('"restime" : "').append(Float.parseFloat(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).round()).append('",')
                if(analysisMap.TransactionMetrics[i]?.score == null)
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].Score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].Score.toString())
                }
                else
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].score.toString())
                }

                slaTable.append('"clientTime" : "').append(analysisMap.TransactionMetrics[i].clientTime).append('",')
                slaTable.append('"avg" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.Average.toString()).append('",')
                slaTable.append('"ninetyper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile".toString()).append('",')
                slaTable.append('"ninetyfiveper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).append('",')
                slaTable.append('"count" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.count.toString()).append('",')
                slaTable.append('"fetchStartTime" : "').append(analysisMap.TransactionMetrics[i].fetchStartTime).append('",')
                slaTable.append('"redirectTime" : "').append(analysisMap.TransactionMetrics[i].redirectTime).append('",')
                slaTable.append('"cacheFetchTime" : "').append(analysisMap.TransactionMetrics[i].cacheFetchTime).append('",')
                slaTable.append('"dnsLookupTime" : "').append((analysisMap.TransactionMetrics[i]?.dnsLookupTime == null ? analysisMap.TransactionMetrics[i].dnsLookUpTime : analysisMap.TransactionMetrics[i].dnsLookupTime)).append('",')
                slaTable.append('"tcpConnectTime" : "').append(analysisMap.TransactionMetrics[i].tcpConnectTime).append('",')
                slaTable.append('"serverTime" : "').append(analysisMap.TransactionMetrics[i].serverTime).append('",')
                slaTable.append('"downloadTime" : "').append(analysisMap.TransactionMetrics[i].downloadTime).append('",')
                slaTable.append('"domProcessingTime" : "').append(analysisMap.TransactionMetrics[i].domProcessingTime).append('",')
                slaTable.append('"onloadTime" : "').append((analysisMap.TransactionMetrics[i]?.onloadTime == null ? analysisMap.TransactionMetrics[i].onLoadTime : analysisMap.TransactionMetrics[i].onloadTime)).append('",')
                slaTable.append('"recommendations" :[')
                for(int j=0; j < analysisMap.TransactionMetrics[i].genericRulesRecommendations.size ;j++)
                {
                    id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                    ruleIDs.add(id)
                    ruleToTxnMapping.add(id + '#' +  analysisMap.TransactionMetrics[i].Name)
                    slaTable.append('{').append('"canvPriority" : "').append(rulesOld[id].priority.toLowerCase()).append(i).append(id).append(',').append(rulesOld[id].width).append(',').append(rulesOld[id].height).append('",')
                    slaTable.append('"canvScore" : "').append(getScoreCanvas(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id,i,analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score,true)).append('",')
                    slaTable.append('"desc" : "').append(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment).append('"}')
                    if(j == (analysisMap.TransactionMetrics[i].genericRulesRecommendations.size -1))
                    {
                        slaTable.append('],')
                    }
                    else
                    {
                        slaTable.append(',')
                    }
                    if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 6)
                    {
                        cmtSize = cmtSize + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                    }
                    if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 9)
                    {
                        cssBody = cssBody + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                    }
                    if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 11)
                    {
                        headJS = headJS + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                    }
                    if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 15)
                    {
                        httpCalls = httpCalls + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                    }
                    if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 23)
                    {
                        htmlSize = htmlSize + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                        bwSaving = bwSaving + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score
                    }

                }
                slaTable.append('"baselineRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselineRT).append('",')
                slaTable.append('"currentRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentRT).append('",')
                slaTable.append('"deviation" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviation).append('",')
                slaTable.append('"comparativeAnalysis" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.analysis).append('"}')

                if( i == (txnCount -1))
                {
                    slaTable.append('],')
                }
                else
                {
                    slaTable.append(',')
                }
            }
        }
        else
        {*/
        if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
        {
            for(int i=0; i < txnCount;i++)
            {
                slaTable.append('{"name" : "').append(analysisMap.TransactionMetrics[i].Name).append('",')
                slaTable.append('"platform" : "').append(analysisMap.TransactionMetrics[i].Platform).append('",')
                slaTable.append('"useragent" : "').append(analysisMap.TransactionMetrics[i].UserAgent).append('",')
                slaTable.append('"browser" : "').append(analysisMap.TransactionMetrics[i].UserAgent).append('",')
                slaTable.append('"device" : "').append(analysisMap.TransactionMetrics[i].DeviceType).append('",')
                slaTable.append('"sla" : "').append(analysisMap.TransactionMetrics[i].SLA).append('",')
                slaTable.append('"restime" : "').append(Float.parseFloat(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).round()).append('",')
                if(analysisMap.TransactionMetrics[i]?.score == null)
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].Score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].Score.toString())
                }
                else
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].score.toString())
                }

                slaTable.append('"serverTime" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).append('",')
                slaTable.append('"clientTime" : "').append(analysisMap.TransactionMetrics[i].userPerceivedTime.toString()).append('",')


                slaTable.append('"avg" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.Average.toString()).append('",')
                slaTable.append('"ninetyper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile".toString()).append('",')
                slaTable.append('"ninetyfiveper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).append('",')
                slaTable.append('"count" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.count.toString()).append('",')
                slaTable.append('"ScreenName" : "').append(analysisMap.TransactionMetrics[i].ScreenName).append('",')
                slaTable.append('"userPerceivedTime" : "').append(analysisMap.TransactionMetrics[i].userPerceivedTime).append('",')
                slaTable.append('"totalRequest" : "').append(analysisMap.TransactionMetrics[i].totalRequest).append('",')
                slaTable.append('"resourceLoadTime" : "').append(analysisMap.TransactionMetrics[i].resourceLoadTime).append('",')
                slaTable.append('"backendAnalysis" : ').append(JsonOutput.toJson(analysisMap.TransactionMetrics[i].backendAnalysis)).append(',')
                slaTable.append('"totalSize" : "').append(analysisMap.TransactionMetrics[i].totalSize).append('",')

                slaTable.append('"recommendations" :[')
                ruleCnt = analysisMap.TransactionMetrics[i].genericRulesRecommendations.size
                if(ruleCnt > 0)
                {
                    for(int j=0; j < analysisMap.TransactionMetrics[i].genericRulesRecommendations.size ;j++)
                    {
                        id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                        ruleIDs.add(id)
                        ruleToTxnMapping.add(id + '#' +  analysisMap.TransactionMetrics[i].Name)
                        //recommendationTable.append('{').append('"canvPriority" : "').append(getPriority(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id,i,analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score,true)).append('",')
                        slaTable.append('{').append('"canvPriority" : "').append(nativeRules[id].priority.toLowerCase()).append(i).append(id).append(',').append(nativeRules[id].width).append(',').append(nativeRules[id].height).append('",')
                        slaTable.append('"canvScore" : "').append(getScoreCanvas(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id,i,analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score,true)).append('",')
                        slaTable.append('"desc" : "').append(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment).append('"}')
                        if(j == (analysisMap.TransactionMetrics[i].genericRulesRecommendations.size -1))
                        {
                            slaTable.append('],')
                        }
                        else
                        {
                            slaTable.append(',')
                        }

                        if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 1)
                        {
                            httpCalls = httpCalls + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                        }

                    }

                }
                else
                {
                    slaTable.append('],')
                }



                slaTable.append('"comparativeAnalysis" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.analysis).append('"}')

                if( i == (txnCount - 1))
                {
                    slaTable.append('],')
                }
                else
                {
                    slaTable.append(',')
                }
            }

        }
        else
        {
            for(int i=0; i < txnCount;i++)
            {
                slaTable.append('{"name" : "').append(analysisMap.TransactionMetrics[i].Name).append('",')
                slaTable.append('"url" : "').append(analysisMap.TransactionMetrics[i]?.url == null ? 'Reanalyze' : analysisMap.TransactionMetrics[i]?.url).append('",')
                slaTable.append('"platform" : "').append(analysisMap.TransactionMetrics[i].Platform).append('",')
                slaTable.append('"useragent" : "').append(analysisMap.TransactionMetrics[i].UserAgent).append('",')
                slaTable.append('"browser" : "').append(analysisMap.TransactionMetrics[i].BrowserName).append('",')
                slaTable.append('"device" : "').append(analysisMap.TransactionMetrics[i].DeviceType).append('",')
                slaTable.append('"sla" : "').append(analysisMap.TransactionMetrics[i].SLA).append('",')
                slaTable.append('"restime" : "').append(Float.parseFloat(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).round()).append('",')

                if(analysisMap.TransactionMetrics[i]?.score == null)
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].Score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].Score.toString())
                }
                else
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].score.toString())
                }

                if(analysisMap.TransactionMetrics[i].NavType == 'Hard')
                {
                    slaTable.append('"clientTime" : "').append(analysisMap.TransactionMetrics[i].clientTime).append('",')
                }
                else
                {
                    slaTable.append('"clientTime" : "').append((analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile" - analysisMap.TransactionMetrics[i].resourceLoadTime)).append('",')
                }

                slaTable.append('"avg" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.Average.toString()).append('",')
                slaTable.append('"max" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime?.Maximum == null ? 0 : analysisMap.TransactionMetrics[i].totalPageLoadTime?.Maximum.toString()).append('",')
                slaTable.append('"min" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime?.Minimum == null ? 0 : analysisMap.TransactionMetrics[i].totalPageLoadTime?.Minimum.toString()).append('",')
                slaTable.append('"ninetyper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile".toString()).append('",')
                slaTable.append('"ninetyfiveper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).append('",')
                slaTable.append('"count" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.count.toString()).append('",')
                slaTable.append('"resourceCount" : "').append((analysisMap.TransactionMetrics[i]?.resourceCount == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceCount)).append('",')
                slaTable.append('"resourceSize" : "').append((analysisMap.TransactionMetrics[i]?.resourceSize == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceSize)).append('",')
                slaTable.append('"visuallyComplete" : "').append((analysisMap.TransactionMetrics[i]?.visuallyComplete == null ? 0 : analysisMap.TransactionMetrics[i]?.visuallyComplete)).append('",')
                if(analysisMap.TransactionMetrics[i].NavType == 'Hard')
                {
                    slaTable.append('"fetchStartTime" : "').append(analysisMap.TransactionMetrics[i].fetchStartTime).append('",')
                    slaTable.append('"redirectTime" : "').append(analysisMap.TransactionMetrics[i].redirectTime).append('",')
                    slaTable.append('"cacheFetchTime" : "').append(analysisMap.TransactionMetrics[i].cacheFetchTime).append('",')
                    slaTable.append('"dnsLookupTime" : "').append((analysisMap.TransactionMetrics[i]?.dnsLookupTime == null ? analysisMap.TransactionMetrics[i].dnsLookUpTime : analysisMap.TransactionMetrics[i].dnsLookupTime)).append('",')
                    slaTable.append('"tcpConnectTime" : "').append(analysisMap.TransactionMetrics[i].tcpConnectTime).append('",')
                    slaTable.append('"serverTime" : "').append(analysisMap.TransactionMetrics[i].serverTime).append('",')
                    slaTable.append('"downloadTime" : "').append(analysisMap.TransactionMetrics[i].downloadTime).append('",')
                    slaTable.append('"domProcessingTime" : "').append(analysisMap.TransactionMetrics[i].domProcessingTime).append('",')
                    slaTable.append('"onloadTime" : "').append((analysisMap.TransactionMetrics[i]?.onloadTime == null ? analysisMap.TransactionMetrics[i].onLoadTime : analysisMap.TransactionMetrics[i].onloadTime)).append('",')
                    slaTable.append('"speedIndex" : "').append(analysisMap.TransactionMetrics[i].speedIndex).append('",')
                    slaTable.append('"resourceLoadTime" : "').append(analysisMap.TransactionMetrics[i].resourceLoadTime).append('",')
                    slaTable.append('"ttfbUser" : "').append((analysisMap.TransactionMetrics[i]?.ttfbUser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfbUser)).append('",')
                    slaTable.append('"ttfbBrowser" : "').append((analysisMap.TransactionMetrics[i]?.ttfbBrowser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfbBrowser)).append('",')
                    slaTable.append('"ttfpUser" : "').append((analysisMap.TransactionMetrics[i]?.ttfpUser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfpUser)).append('",')
                    slaTable.append('"ttfpBrowser" : "').append((analysisMap.TransactionMetrics[i]?.ttfpBrowser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfpBrowser)).append('",')
                    slaTable.append('"clientProcessing" : "').append((analysisMap.TransactionMetrics[i]?.clientProcessing == null ? 0 : analysisMap.TransactionMetrics[i]?.clientProcessing)).append('",')
                    slaTable.append('"resourceBlockTime" : "').append((analysisMap.TransactionMetrics[i]?.resourceBlockTime == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceBlockTime)).append('",')
                    slaTable.append('"domInteractive" : "').append((analysisMap.TransactionMetrics[i]?.domInteractive == null ? 0 : analysisMap.TransactionMetrics[i]?.domInteractive)).append('",')
                }
                else
                {
                    slaTable.append('"resourceLoadTime" : "').append(analysisMap.TransactionMetrics[i].resourceLoadTime).append('",')
                    slaTable.append('"serverTime" : "').append(analysisMap.TransactionMetrics[i].resourceLoadTime).append('",')
                    slaTable.append('"clientProcessing" : "').append((analysisMap.TransactionMetrics[i]?.clientProcessing == null ? 0 : analysisMap.TransactionMetrics[i]?.clientProcessing)).append('",')
                    slaTable.append('"resourceBlockTime" : "').append((analysisMap.TransactionMetrics[i]?.resourceBlockTime == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceBlockTime)).append('",')
                }

                if(configReader?.isMarkAPIEnabled != null && configReader?.isMarkAPIEnabled)
                {
                    slaTable.append('"markTimings" : ').append(analysisMap.TransactionMetrics[i]?.markTimings == null ? '[]' : JsonOutput.toJson(analysisMap.TransactionMetrics[i].markTimings)).append(',')
                }

                slaTable.append('"backendAnalysis" : ').append(JsonOutput.toJson(analysisMap.TransactionMetrics[i].backendAnalysis)).append(',')


                ruleCnt = analysisMap.TransactionMetrics[i].genericRulesRecommendations.size
                slaTable.append('"recommendations" :[')

                if (ruleCnt > 0)
                {
                    def bwFlag = (analysisMap.TransactionMetrics[i].genericRulesRecommendations.id.contains('2') && analysisMap.TransactionMetrics[i].genericRulesRecommendations.id.contains('30'))
                    if(analysisMap.TransactionMetrics[i]?.NavType == 'Hard') {
                        for (int j = 0; j < ruleCnt; j++) {
                            id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                            ruleIDs.add(id)
                            ruleToTxnMapping.add(id + '#' + analysisMap.TransactionMetrics[i].Name)
                            //recommendationTable.append('{').append('"canvPriority" : "').append(getPriority(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id,i,analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score,true)).append('",')
                            slaTable.append('{').append('"canvPriority" : "').append(rules[id].priority.toLowerCase()).append(i).append(id).append(',').append(rules[id].width).append(',').append(rules[id].height).append('",')
                            slaTable.append('"canvScore" : "').append(getScoreCanvas(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id, i, analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score, true)).append('",')
                            slaTable.append('"desc" : "').append(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment).append('"}')
                            if (j == (ruleCnt - 1)) {
                                slaTable.append('],')
                            } else {
                                slaTable.append(',')
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 7) {
                                cmtSize = cmtSize + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 10) {
                                cssBody = cssBody + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 12) {
                                headJS = headJS + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 16) {
                                httpCalls = httpCalls + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 2) {

                                htmlSize = htmlSize + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                                bwSaving = bwSaving + (100 - analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score)
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 30) {
                                bwSaving = bwSaving + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if(bwFlag)
                            {
                                bwSaving = (bwSaving/2)
                            }
                        }
                    }
                    else
                    {
                        for (int j = 0; j < ruleCnt; j++)
                        {
                            id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                            ruleIDs.add(id)
                            ruleToTxnMapping.add(id + '#' + analysisMap.TransactionMetrics[i].Name)

                            slaTable.append('{').append('"canvPriority" : "').append(softRules[id].priority.toLowerCase()).append(i).append(id).append(',').append(softRules[id].width).append(',').append(softRules[id].height).append('",')
                            slaTable.append('"canvScore" : "').append(getScoreCanvas(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id, i, analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score, true)).append('",')
                            slaTable.append('"desc" : "').append(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment).append('"}')
                            if (j == (ruleCnt - 1))
                            {
                                slaTable.append('],')
                            }
                            else
                            {
                                slaTable.append(',')
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 16) {
                                httpCalls = httpCalls + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }

                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 30) {
                                bwSaving = bwSaving + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                        }


                    }

                }
                else
                {
                    slaTable.append('],')
                }


                slaTable.append('"baselineRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselineRT).append('",')
                slaTable.append('"currentRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentRT).append('",')
                slaTable.append('"deviation" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviation).append('",')
                slaTable.append('"baselineHC" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselineHC).append('",')
                slaTable.append('"currentHC" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentHC).append('",')
                slaTable.append('"deviationHC" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviationHC).append('",')
                slaTable.append('"baselinePld" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselinePld).append('",')
                slaTable.append('"currentPld" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentPld).append('",')
                slaTable.append('"deviationPld" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviationPld).append('",')

                slaTable.append('"comparativeAnalysis" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.analysis).append('"}')


                if( i == (txnCount -1))
                {
                    slaTable.append('],')
                }
                else
                {
                    slaTable.append(',')

                }

            }
        }

        // }

        double scorePer = 0
        double bwSavingPcnt = 0
        /* if(analysisMap.TransactionMetrics[0]?.NavType == null)
         {
             if(txnCount > 0) {
                 summaryTable.append(PaceReportEngine.getRuleSummaryJSON(ruleIDs, ruleToTxnMapping,'HardOld',configReader))
                 scorePer = (totalScore / txnCount).round()
                 bwSavingPcnt = (1 - (bwSaving / (txnCount * 100))) * 100
             }
         }
         else
         {*/
        if(txnCount > 0)
        {
            if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
            {
                summaryTable.append(PaceReportEngine.getRuleSummaryJSON(ruleIDs, ruleToTxnMapping,'Native',configReader))
                scorePer = (totalScore / txnCount).round()
                bwSavingPcnt = 0
            }
            else
            {
                summaryTable.append(PaceReportEngine.getRuleSummaryJSON(ruleIDs, ruleToTxnMapping,'Hard',configReader))
                scorePer = (totalScore / txnCount).round()
                bwSavingPcnt = (bwSaving / (txnCount * 100)) * 100
            }
        }
        //}
        exectiveSummary.append('"execSummary" : {')
        exectiveSummary.append('"totalScore" :').append(scorePer).append(',')
        exectiveSummary.append('"callsReduction" :').append(httpCalls).append(',')
        exectiveSummary.append('"cssCnt" :').append(cssBody).append(',')
        exectiveSummary.append('"jsCnt" :').append(headJS).append(',')
        exectiveSummary.append('"commentSize" :').append(cmtSize).append(',')
        exectiveSummary.append('"htmlSize" :').append(htmlSize).append(',')
        exectiveSummary.append('"bandwidthSaving" :').append(bwSavingPcnt).append('}')

        summary.append ('{')
        summary.append (headerTable).append(',')
        summary.append (exectiveSummary).append(',')
        summary.append (summaryTable).append(',')
        summary.append (slaTable)
        summary.append ('"kibanaURL" : "').append(configReader.kibanaURL).append('"}')
        return summary
    }

    static def summaryJSONInSeconds(def configReader,def jsonString)
    {
        LOGGER.debug 'summaryJSON JSON String : ' +  jsonString
        def jsonObj = new JSONObject(jsonString)
        def analysisMap = JsonUtils.jsonToMap(jsonObj)

        StringBuilder summary = new StringBuilder()
        StringBuilder summaryTable = new StringBuilder()
        StringBuilder slaTable = new StringBuilder()
        StringBuilder headerTable = new StringBuilder()
        StringBuilder exectiveSummary = new StringBuilder()


        def id = null
        def ruleIDs = []
        def ruleToTxnMapping = []
        float devPctComparison = 0.00f
        float devPctSLA = 0.00f
        float totalScore = 0.00f
        def httpCalls = 0
        def cmtSize = 0
        def cssBody = 0
        def headJS = 0
        def htmlSize = 0
        def bwSaving = 0
        def ruleCnt = 0
        def threshold = (configReader.ragThresold ? configReader.ragThresold : '20,10,-5')
        def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)
        List<String> thresholdValues = Arrays.asList(threshold.split("\\s*,\\s*"));
        def rules = PaceRuleEngine.getGeneralRules(null,configReader)
        def softRules = PaceRuleEngine.getSoftNavigationRules(null,configReader)
        def nativeRules = PaceRuleEngine.getNativeAppRules(null,configReader)
        def runDetails

        def txnCount = (analysisMap.TransactionMetrics?.size == null ? 0 : analysisMap.TransactionMetrics?.size)
        headerTable.append('"header" : {')
        headerTable.append('"ClientName": "').append(analysisMap.ClientNameOrg).append('",')
        headerTable.append('"ProjectName" : "').append(analysisMap.ProjectNameOrg).append('",')
        headerTable.append('"ScenarioName" : "').append(analysisMap.ScenarioOrg).append('"')
        if(configReader?.BaselineStart == null)
        {
            headerTable.append(',"CurrentRunID" : "').append(analysisMap.CurrentRunID).append('",')
            headerTable.append('"BaselineRunID" : "').append(analysisMap.BaselineRunID).append('"')
        }

        if(txnCount > 0)
        {
            if(configReader?.BaselineStart == null)
            {
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap.CurrentRunID)
                headerTable.append(',"CurrentRunDuration" : "').append(runDetails[0]).append('",')
                headerTable.append('"CurrentRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"CurrentBuildNo" : "').append(runDetails[2]).append('",')
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap.BaselineRunID)
                headerTable.append('"BaselineRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"BaselineBuildNo" : "').append(runDetails[2]).append('",')
                headerTable.append('"BaselineRunDuration" : "').append(runDetails[0]).append('"')
            }
            else
            {
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap?.CurrentRunID,'Y','Current')
                headerTable.append(',"CurrentRunDuration" : "').append(runDetails[0]).append('",')
                headerTable.append('"CurrentRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"CurrentBuildNo" : "').append(runDetails[2]).append('",')
                runDetails = ElasticSearchUtils.execTimeOfRunID(configReader,analysisMap?.CurrentRunID,'Y','Baseline')
                headerTable.append('"BaselineRelease" : "').append(runDetails[1]).append('",')
                headerTable.append('"BaselineBuildNo" : "').append(runDetails[2]).append('",')
                headerTable.append('"BaselineRunDuration" : "').append(runDetails[0]).append('"')
            }

        }
        headerTable.append('}')

        slaTable.append('"Transactions" : [')

        def hardTxnCnt = 0

        if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
        {
            for(int i=0; i < txnCount;i++)
            {
                slaTable.append('{"name" : "').append(analysisMap.TransactionMetrics[i].Name).append('",')
                slaTable.append('"platform" : "').append(analysisMap.TransactionMetrics[i].Platform).append('",')
                slaTable.append('"useragent" : "').append(analysisMap.TransactionMetrics[i].UserAgent).append('",')
                slaTable.append('"browser" : "').append(analysisMap.TransactionMetrics[i].UserAgent).append('",')
                slaTable.append('"device" : "').append(analysisMap.TransactionMetrics[i].DeviceType).append('",')
                slaTable.append('"sla" : "').append(analysisMap.TransactionMetrics[i].SLA).append('",')
                slaTable.append('"restime" : "').append(Float.parseFloat(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).round()).append('",')
                if(analysisMap.TransactionMetrics[i]?.score == null)
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].Score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].Score.toString())
                }
                else
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].score.toString())
                }

                slaTable.append('"serverTime" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).append('",')
                slaTable.append('"clientTime" : "').append(analysisMap.TransactionMetrics[i].userPerceivedTime.toString()).append('",')


                slaTable.append('"avg" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.Average.toString()).append('",')
                slaTable.append('"ninetyper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile".toString()).append('",')
                slaTable.append('"ninetyfiveper" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile".toString()).append('",')
                slaTable.append('"count" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.count.toString()).append('",')
                slaTable.append('"ScreenName" : "').append(analysisMap.TransactionMetrics[i].ScreenName).append('",')
                slaTable.append('"userPerceivedTime" : "').append(analysisMap.TransactionMetrics[i].userPerceivedTime).append('",')
                slaTable.append('"totalRequest" : "').append(analysisMap.TransactionMetrics[i].totalRequest).append('",')
                slaTable.append('"resourceLoadTime" : "').append(analysisMap.TransactionMetrics[i].resourceLoadTime).append('",')
                slaTable.append('"backendAnalysis" : ').append(JsonOutput.toJson(analysisMap.TransactionMetrics[i].backendAnalysis)).append(',')
                slaTable.append('"totalSize" : "').append(analysisMap.TransactionMetrics[i].totalSize).append('",')

                slaTable.append('"recommendations" :[')
                ruleCnt = analysisMap.TransactionMetrics[i].genericRulesRecommendations.size
                if(ruleCnt > 0)
                {
                    for(int j=0; j < analysisMap.TransactionMetrics[i].genericRulesRecommendations.size ;j++)
                    {
                        id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                        ruleIDs.add(id)
                        ruleToTxnMapping.add(id + '#' +  analysisMap.TransactionMetrics[i].Name)
                        //recommendationTable.append('{').append('"canvPriority" : "').append(getPriority(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id,i,analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score,true)).append('",')
                        slaTable.append('{').append('"canvPriority" : "').append(nativeRules[id].priority.toLowerCase()).append(i).append(id).append(',').append(nativeRules[id].width).append(',').append(nativeRules[id].height).append('",')
                        slaTable.append('"canvScore" : "').append(getScoreCanvas(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id,i,analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score,true)).append('",')
                        slaTable.append('"desc" : "').append(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment).append('"}')
                        if(j == (analysisMap.TransactionMetrics[i].genericRulesRecommendations.size -1))
                        {
                            slaTable.append('],')
                        }
                        else
                        {
                            slaTable.append(',')
                        }

                        if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 1)
                        {
                            httpCalls = httpCalls + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                        }

                    }

                }
                else
                {
                    slaTable.append('],')
                }


                slaTable.append('"baselineRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselineRT).append('",')
                slaTable.append('"currentRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentRT).append('",')
                slaTable.append('"deviation" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviation).append('",')
                slaTable.append('"comparativeAnalysis" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.analysis).append('"}')

                if( i == (txnCount - 1))
                {
                    slaTable.append('],')
                }
                else
                {
                    slaTable.append(',')
                }
            }

        }
        else
        {
            for(int i=0; i < txnCount;i++)
            {
                slaTable.append('{"name" : "').append(analysisMap.TransactionMetrics[i].Name).append('",')
                slaTable.append('"url" : "').append(analysisMap.TransactionMetrics[i]?.url == null ? 'Url not available' : analysisMap.TransactionMetrics[i]?.url).append('",')
                slaTable.append('"platform" : "').append(analysisMap.TransactionMetrics[i].Platform).append('",')
                slaTable.append('"useragent" : "').append(analysisMap.TransactionMetrics[i].UserAgent).append('",')
                slaTable.append('"browser" : "').append(analysisMap.TransactionMetrics[i].BrowserName).append('",')
                slaTable.append('"device" : "').append(analysisMap.TransactionMetrics[i].DeviceType).append('",')
                slaTable.append('"sla" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].SLA)).append('",')
                if (comparisonPerc == 'Pcnt95')
                {
                    slaTable.append('"restime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile")).append('",')
                }
                if (comparisonPerc == 'Pcnt90')
                {
                    slaTable.append('"restime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile")).append('",')
                }
                if (comparisonPerc == 'Average')
                {
                    slaTable.append('"restime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime.Average)).append('",')
                }


                if(analysisMap.TransactionMetrics[i]?.score == null)
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].Score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].Score.toString())
                }
                else
                {
                    slaTable.append('"score" : "').append(analysisMap.TransactionMetrics[i].score).append('",')
                    totalScore = totalScore + Float.parseFloat(analysisMap.TransactionMetrics[i].score.toString())
                }

                if(analysisMap.TransactionMetrics[i].NavType == 'Hard')
                {
                    slaTable.append('"clientTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].clientTime)).append('",')
                }
                else
                {
                    slaTable.append('"clientTime" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile" - analysisMap.TransactionMetrics[i].resourceLoadTime))).append('",')
                }

                slaTable.append('"avg" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime.Average)).append('",')
                slaTable.append('"max" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime?.Maximum == null ? 0 : analysisMap.TransactionMetrics[i].totalPageLoadTime?.Maximum)).append('",')
                slaTable.append('"min" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime?.Minimum == null ? 0 : analysisMap.TransactionMetrics[i].totalPageLoadTime?.Minimum)).append('",')
                slaTable.append('"ninetyper" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime."90 Percentile")).append('",')
                slaTable.append('"ninetyfiveper" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].totalPageLoadTime."95 Percentile")).append('",')
                slaTable.append('"count" : "').append(analysisMap.TransactionMetrics[i].totalPageLoadTime.count.toString()).append('",')
                slaTable.append('"resourceCount" : "').append((analysisMap.TransactionMetrics[i]?.resourceCount == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceCount)).append('",')
                slaTable.append('"resourceSize" : "').append((analysisMap.TransactionMetrics[i]?.resourceSize == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceSize)).append('",')
                slaTable.append('"visuallyComplete" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i]?.visuallyComplete == null ? 0 : analysisMap.TransactionMetrics[i]?.visuallyComplete)).append('",')
                if(analysisMap.TransactionMetrics[i].NavType == 'Hard')
                {
                    slaTable.append('"fetchStartTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].fetchStartTime)).append('",')
                    slaTable.append('"redirectTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].redirectTime)).append('",')
                    slaTable.append('"cacheFetchTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].cacheFetchTime)).append('",')
                    slaTable.append('"dnsLookupTime" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.dnsLookupTime == null ? analysisMap.TransactionMetrics[i].dnsLookUpTime : analysisMap.TransactionMetrics[i].dnsLookupTime))).append('",')
                    slaTable.append('"tcpConnectTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].tcpConnectTime)).append('",')
                    slaTable.append('"serverTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].serverTime)).append('",')
                    slaTable.append('"downloadTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].downloadTime)).append('",')
                    slaTable.append('"domProcessingTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].domProcessingTime)).append('",')
                    slaTable.append('"onloadTime" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.onloadTime == null ? analysisMap.TransactionMetrics[i].onLoadTime : analysisMap.TransactionMetrics[i].onloadTime))).append('",')
                    slaTable.append('"speedIndex" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].speedIndex)).append('",')
                    slaTable.append('"resourceLoadTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].resourceLoadTime)).append('",')
                    slaTable.append('"ttfbUser" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.ttfbUser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfbUser))).append('",')
                    slaTable.append('"ttfbBrowser" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.ttfbBrowser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfbBrowser))).append('",')
                    slaTable.append('"ttfpUser" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.ttfpUser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfpUser))).append('",')
                    slaTable.append('"ttfpBrowser" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.ttfpBrowser == null ? 0 : analysisMap.TransactionMetrics[i]?.ttfpBrowser))).append('",')
                    slaTable.append('"clientProcessing" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.clientProcessing == null ? 0 : analysisMap.TransactionMetrics[i]?.clientProcessing))).append('",')
                    slaTable.append('"resourceBlockTime" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.resourceBlockTime == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceBlockTime))).append('",')
                    slaTable.append('"domInteractive" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i]?.domInteractive == null ? 0 : analysisMap.TransactionMetrics[i]?.domInteractive)).append('",')
                }
                else
                {
                    slaTable.append('"resourceLoadTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].resourceLoadTime)).append('",')
                    slaTable.append('"serverTime" : "').append(CommonUtils.convertMilliToSeconds(analysisMap.TransactionMetrics[i].resourceLoadTime)).append('",')
                    slaTable.append('"clientProcessing" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.clientProcessing == null ? 0 : analysisMap.TransactionMetrics[i]?.clientProcessing))).append('",')
                    slaTable.append('"resourceBlockTime" : "').append(CommonUtils.convertMilliToSeconds((analysisMap.TransactionMetrics[i]?.resourceBlockTime == null ? 0 : analysisMap.TransactionMetrics[i]?.resourceBlockTime))).append('",')
                }

                if(configReader?.isMarkAPIEnabled != null && configReader?.isMarkAPIEnabled)
                {
                    slaTable.append('"markTimings" : ').append(analysisMap.TransactionMetrics[i]?.markTimings == null ? '[]' : JsonOutput.toJson(analysisMap.TransactionMetrics[i].markTimings)).append(',')
                }

                slaTable.append('"backendAnalysis" : ').append(JsonOutput.toJson(analysisMap.TransactionMetrics[i].backendAnalysis)).append(',')


                ruleCnt = analysisMap.TransactionMetrics[i].genericRulesRecommendations.size
                slaTable.append('"recommendations" :[')

                if (ruleCnt > 0)
                {
                    def bwFlag = (analysisMap.TransactionMetrics[i].genericRulesRecommendations.id.contains('2') && analysisMap.TransactionMetrics[i].genericRulesRecommendations.id.contains('30'))
                    if(analysisMap.TransactionMetrics[i]?.NavType == 'Hard') {
                        for (int j = 0; j < ruleCnt; j++) {
                            id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                            ruleIDs.add(id)
                            ruleToTxnMapping.add(id + '#' + analysisMap.TransactionMetrics[i].Name)
                            //recommendationTable.append('{').append('"canvPriority" : "').append(getPriority(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id,i,analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score,true)).append('",')
                            slaTable.append('{').append('"canvPriority" : "').append(rules[id].priority.toLowerCase()).append(i).append(id).append(',').append(rules[id].width).append(',').append(rules[id].height).append('",')
                            slaTable.append('"canvScore" : "').append(getScoreCanvas(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id, i, analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score, true)).append('",')
                            slaTable.append('"desc" : "').append(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment).append('"}')
                            if (j == (ruleCnt - 1)) {
                                slaTable.append('],')
                            } else {
                                slaTable.append(',')
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 7) {
                                cmtSize = cmtSize + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 10) {
                                cssBody = cssBody + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 12) {
                                headJS = headJS + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 16) {
                                httpCalls = httpCalls + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 2) {

                                htmlSize = htmlSize + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                                bwSaving = bwSaving + (100 - analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score)
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 30) {
                                bwSaving = bwSaving + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                            if(bwFlag)
                            {
                                bwSaving = (bwSaving/2)
                            }
                        }
                    }
                    else
                    {
                        for (int j = 0; j < ruleCnt; j++)
                        {
                            id = analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id.toString()
                            ruleIDs.add(id)
                            ruleToTxnMapping.add(id + '#' + analysisMap.TransactionMetrics[i].Name)

                            slaTable.append('{').append('"canvPriority" : "').append(softRules[id].priority.toLowerCase()).append(i).append(id).append(',').append(softRules[id].width).append(',').append(softRules[id].height).append('",')
                            slaTable.append('"canvScore" : "').append(getScoreCanvas(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id, i, analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].score, true)).append('",')
                            slaTable.append('"desc" : "').append(analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].comment).append('"}')
                            if (j == (ruleCnt - 1))
                            {
                                slaTable.append('],')
                            }
                            else
                            {
                                slaTable.append(',')
                            }
                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 16) {
                                httpCalls = httpCalls + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }

                            if (analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].id == 30) {
                                bwSaving = bwSaving + analysisMap.TransactionMetrics[i].genericRulesRecommendations[j].value
                            }
                        }


                    }

                }
                else
                {
                    slaTable.append('],')
                }


                slaTable.append('"baselineRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselineRT).append('",')
                slaTable.append('"currentRT" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentRT).append('",')
                slaTable.append('"deviation" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviation).append('",')
                slaTable.append('"baselineHC" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselineHC).append('",')
                slaTable.append('"currentHC" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentHC).append('",')
                slaTable.append('"deviationHC" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviationHC).append('",')
                slaTable.append('"baselinePld" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.baselinePld).append('",')
                slaTable.append('"currentPld" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.currentPld).append('",')
                slaTable.append('"deviationPld" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.deviationPld).append('",')
                slaTable.append('"comparativeAnalysis" : "').append(analysisMap.TransactionMetrics[i].comparativeAnalysis.analysis).append('"}')


                if( i == (txnCount -1))
                {
                    slaTable.append('],')
                }
                else
                {
                    slaTable.append(',')

                }

            }
        }

        double scorePer = 0
        double bwSavingPcnt = 0

        if(txnCount > 0)
        {
            if(configReader?.isNativeApp != null && configReader?.isNativeApp == true)
            {
                summaryTable.append(PaceReportEngine.getRuleSummaryJSON(ruleIDs, ruleToTxnMapping,'Native',configReader))
                scorePer = (totalScore / txnCount).round()
                bwSavingPcnt = 0
            }
            else
            {
                summaryTable.append(PaceReportEngine.getRuleSummaryJSON(ruleIDs, ruleToTxnMapping,'Hard',configReader))
                scorePer = (totalScore / txnCount).round()
                bwSavingPcnt = (bwSaving / (txnCount * 100)) * 100
            }
        }

        exectiveSummary.append('"execSummary" : {')
        exectiveSummary.append('"totalScore" :').append(scorePer).append(',')
        exectiveSummary.append('"callsReduction" :').append(httpCalls).append(',')
        exectiveSummary.append('"cssCnt" :').append(cssBody).append(',')
        exectiveSummary.append('"jsCnt" :').append(headJS).append(',')
        exectiveSummary.append('"commentSize" :').append(cmtSize).append(',')
        exectiveSummary.append('"htmlSize" :').append(htmlSize).append(',')
        exectiveSummary.append('"bandwidthSaving" :').append(bwSavingPcnt).append('}')

        summary.append ('{')
        summary.append (headerTable).append(',')
        summary.append (exectiveSummary).append(',')
        summary.append (summaryTable).append(',')
        summary.append (slaTable)
        summary.append ('"kibanaURL" : "').append(configReader.kibanaURL).append('"}')
        return summary
    }


}