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

using com.cognizant.pace.CXOptimize.Collector.config;
using com.cognizant.pace.CXOptimize.Collector.constant;
using com.cognizant.pace.CXOptimize.Collector.service;
//using OpenQA.Selenium;
using Microsoft.VisualStudio.TestTools.UITesting;
using Microsoft.VisualStudio.TestTools.UITesting.HtmlControls;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading;
using Yahoo.Yui.Compressor;

namespace com.cognizant.pace.CXOptimize.Collector.utils
{
    public class CollectorUtils
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        //Getter & Setter for Previous Transaction Time
        private static long PrevTxnStartTime = 0;
        //Getter & Setter for Previous Transaction Time
        private static long PrevTxnHeapSize = 0;

        public static long getCurrentMilliSeconds()
        {
            return Convert.ToInt64((DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalMilliseconds);
        }

        public static long getPrevTxnStartTime()
        {
            return PrevTxnStartTime;
        }

        public static void setPrevTxnStartTime(long prevTxnStartTime)

        {
            PrevTxnStartTime = prevTxnStartTime;
        }

        public static long getPrevTxnHeapSize()
        {
            return PrevTxnHeapSize;
        }

        public static void setPrevTxnHeapSize(long prevTxnHeapSize)

        {
            PrevTxnHeapSize = prevTxnHeapSize;
        }

        public static Boolean stringContainsItemFromList(String inputString, String match)
        {
            String[] items = match.Split(new string[] { "\\s*,\\s*" }, StringSplitOptions.None);
            inputString = inputString.ToLower().Split(new string[] { "\\?" }, StringSplitOptions.None)[0];
            foreach (String item in items)
            {
                if (inputString.Contains(item))
                {
                    return true;
                }
            }
            return false;
        }

        public static long compress(String input, String filename)
        {
            long compressed = 0;
            String output = string.Empty;
            try
            {
                StreamReader inputReader = new StreamReader(input);
                StringWriter outputWriter = new StringWriter();

                if (stringContainsItemFromList(filename, ".js"))
                {
                    JavaScriptCompressor compressor = new JavaScriptCompressor();
                    output = compressor.Compress(input);
                    compressed = output.Length;
                }
                else
                {
                    CssCompressor compressor = new CssCompressor();
                    output = compressor.Compress(input);
                    compressed = output.Length;
                }
            }
            catch (Exception e)
            {
                log.Debug("Error Compressing CSS or Javascript {}", e);
                log.Debug("Continuing with simple minifier");
                compressed = input.Replace("\\n", "").Replace("\\t", "").Replace("\\r", "").Length;
                //compressed = input.Replace("\\n| {2}|\\t|\\r", "").Length;
            }
            return compressed;
        }

        public static Dictionary<String, Object> extractData(String txnName, String url, BrowserWindow browser, int txnStatus)
        {
            log.Debug("Started extracting data for Transaction : " + txnName + " with status " + txnStatus);
            Dictionary<String, Object> rtnValue = new Dictionary<string, object>();
            try
            {
                //IJavaScriptExecutor jsExe = (IJavaScriptExecutor)browser;

                log.Debug("Check Browser Type");
                String userAgent = (String)browser.ExecuteScript("var nAgt = navigator.userAgent|| {}; return nAgt;");
                log.Debug("User Agent : " + userAgent);

                if (userAgent != null)
                {
                    CollectorConstants.setUserAgent(userAgent);
                    rtnValue = collateData(txnName, url, browser, txnStatus);
                }
                else {
                    /*Adding conditions to check if Client Config is null or User Agent is null*/
                    log.Debug("No performance data will be collected for " + txnName + " since UserAgent: " + userAgent + " is null");
                    rtnValue = null;
                }
            }
            catch (Exception e)
            {
                log.Error("Exception in extractData for " + txnName + " at " + e);
            }
            log.Debug("Completed extracting data for Transaction : " + txnName + " with status " + txnStatus);
            return rtnValue;
        }

        public static Dictionary<String, Object> collateData(String txnName, String url, BrowserWindow jsExe, int txnStatus)
        {
            log.Debug("Started collate data for Transaction : " + txnName + " with status " + txnStatus);
            Dictionary<String, Object> rtnValue = new Dictionary<string, object>();
            long currNavTime;
            long totalTime = 0;
            long serverTime = 0;
            Boolean navType = false;
            double speedIndex = 0;

            Dictionary<String, Object> collectedData = new Dictionary<string, object>();

            try
            {

                Dictionary<string, object> navigationDetails = new Dictionary<string, object>();
                Dictionary<string, object> memoryDetails = new Dictionary<string, object>();
                Dictionary<string, object> browserDetails = new Dictionary<string, object>();
                Dictionary<String, Object> heapUsage = new Dictionary<string, object>();
                List<Dictionary<String, Object>> resourceDetails = null;
                List<Dictionary<String, Object>> markDetails = null;

                Boolean isNavigationAPIEnabled, isResourceAPIEnabled, isMemoryAPIEnabled, isMarkAPIEnabled;
                StringBuilder document = new StringBuilder();
                long loadEventEnd, currentHeapUsage, msFirstPaint;
                long startTime, endTime, duration;

                //Load Configuration from remote service

                ConfigurationLoader config = ConfigurationLoader.getInstance();
                if (config.clientConfig.Count > 0)
                {
                    log.Debug("Configuration Loading..");
                    collectedData = (from x in config.clientConfig select x).ToDictionary(x => x.Key, x => x.Value);
                    collectedData.Add("TxnName", txnName);
                    collectedData.Add("txnStatus", txnStatus);
                    collectedData.Add("Url", url);

                    isNavigationAPIEnabled = ((config.clientConfig.ContainsKey("isNavigationAPIEnabled")) ? Convert.ToBoolean(config.clientConfig["isNavigationAPIEnabled"]) : false);
                    isResourceAPIEnabled = ((config.clientConfig.ContainsKey("isResourceAPIEnabled")) ? Convert.ToBoolean(config.clientConfig["isResourceAPIEnabled"]) : false);
                    isMemoryAPIEnabled = ((config.clientConfig.ContainsKey("isMemoryAPIEnabled")) ? Convert.ToBoolean(config.clientConfig["isMemoryAPIEnabled"]) : false);
                    isMarkAPIEnabled = ((config.clientConfig.ContainsKey("isMarkAPIEnabled")) ? Convert.ToBoolean(config.clientConfig["isMarkAPIEnabled"]) : false);

                    log.Debug("Configuration Loading completed");
                    log.Debug("Navigation API data collection started for " + txnName);

                    if (isNavigationAPIEnabled)
                    {
                        log.Debug("User Agent : " + CollectorConstants.getUserAgent());
                        if (CollectorConstants.getUserAgent().Contains("Trident"))
                        {
                            log.Debug("Processing IE logic");
                            startTime = getCurrentMilliSeconds();
                            do
                            {
                                loadEventEnd = Convert.ToInt64(jsExe.ExecuteScript(CollectorConstants.getLoadEventEndIE()).ToString());
                                endTime = getCurrentMilliSeconds();
                                duration = (endTime - startTime) / 1000;
                                if (duration > 180)
                                {
                                    break;
                                }
                            }
                            while (loadEventEnd <= 0);

                            object strNavigationDetails = jsExe.ExecuteScript(CollectorConstants.getNavigationTimeIE());
                            navigationDetails = JSONUtils.JsonStringToMap(strNavigationDetails.ToString());
                        }
                        else {
                            log.Debug("Processing other browser logic");
                            //Loop until loadEventEnd is non zero or 3 minutes to avoid infinite loop
                            startTime = getCurrentMilliSeconds();
                            do
                            {
                                //navigationDetails = (Map<String, Object>)jsExe.executeScript("var performance = window.performance || window.webkitPerformance || window.mozPerformance || window.msPerformance || {};var timings = performance.timing || {}; return timings.loadEventEnd;");
                                loadEventEnd = Convert.ToInt64(jsExe.ExecuteScript(CollectorConstants.getLoadEventEnd()).ToString());
                                endTime = getCurrentMilliSeconds();
                                duration = (endTime - startTime) / 1000;
                                if (duration > 180)
                                {
                                    break;
                                }
                            }
                            while (loadEventEnd <= 0); //Adding the loop to avoid loadEventEnd = 0

                            navigationDetails = (Dictionary<String, Object>)jsExe.ExecuteScript(CollectorConstants.getNavigationTime());
                            if (CollectorConstants.getUserAgent().Contains("Chrome") && !CollectorConstants.getUserAgent().Contains("Edge"))
                            {
                                msFirstPaint = (long)jsExe.ExecuteScript(CollectorConstants.getFirstPaint());
                                if (msFirstPaint != -9999)
                                    collectedData.Add("msFirstPaint", msFirstPaint);
                                else
                                    collectedData.Add("msFirstPaint", navigationDetails["loadEventEnd"]);

                            }
                            else
                            {
                                //No first paint event available for firefox
                                if (!CollectorConstants.getUserAgent().Contains("Edge"))
                                {
                                    collectedData.Add("msFirstPaint", navigationDetails["loadEventEnd"]);
                                }
                            }

                        }

                        log.Debug("Collected Navigation API data " + navigationDetails.ToString() + " for Transaction : " + txnName);
                        //validate if this is a new transaction. If true persist data else break immediately

                        currNavTime = (long)navigationDetails["navigationStart"];

                        if (currNavTime > getPrevTxnStartTime())
                        {
                            setPrevTxnStartTime(currNavTime);
                            navType = true;
                            collectedData.Add("StartTime", currNavTime);
                            totalTime = ((long)navigationDetails["loadEventEnd"] - (long)navigationDetails["navigationStart"]);
                            serverTime = ((long)navigationDetails["responseStart"] - (long)navigationDetails["requestStart"]);
                            log.Debug("Hard Navigation : {}" + txnName);
                            speedIndex = Convert.ToDouble(jsExe.ExecuteScript(CollectorConstants.getSpeedIndex()));
                            collectedData.Add("SpeedIndex", speedIndex);
                            log.Debug("SpeedIndex for Transaction:" + txnName + " is " + speedIndex);

                        }
                        else {
                            navType = false;
                            collectedData.Add("StartTime", getCurrentMilliSeconds());
                            navigationDetails = null;
                            log.Debug("Soft Navigation : {}" + txnName);
                        }
                        collectedData.Add("NavigationTime", navigationDetails);
                        collectedData.Add("NavType", navType);

                    }
                    // Fetch Client-side Resource Details via Resource Timing API
                    if (isResourceAPIEnabled)
                    {
                        long beforeLength, afterLength = 0;

                        do
                        {
                            beforeLength = (long)jsExe.ExecuteScript(CollectorConstants.getResourceLength());
                            Thread.Sleep(CollectorConstants.getResourceSettleTime());
                            afterLength = (long)jsExe.ExecuteScript(CollectorConstants.getResourceLength());

                        } while (beforeLength < afterLength);

                        if (CollectorConstants.getUserAgent().Contains("Trident"))
                        {
                            var serializer = new System.Web.Script.Serialization.JavaScriptSerializer();
                            resourceDetails = serializer.Deserialize<List<Dictionary<string, object>>>
                                (jsExe.ExecuteScript(CollectorConstants.getResourceTimeIE()).ToString());
                        }
                        else {
                            resourceDetails = ((IReadOnlyCollection<Object>)jsExe.ExecuteScript(CollectorConstants.getResourceTime())).ToDictionaries();
                        }
                        jsExe.ExecuteScript(CollectorConstants.clearResourceTiming());
                        log.Debug("Collected Resource Timing API : " + resourceDetails.ToString());
                        collectedData.Add("ResourceTime", resourceDetails);
                    }

                    if (navigationDetails == null && resourceDetails.Count <= 0)
                    {
                        log.Info("The transaction " + txnName + " probably did not make a server request and hence will be ignored.");
                        return rtnValue;
                    }

                    // Fetch Client-side Menory Details via Memory API

                    if (isMemoryAPIEnabled)
                    {
                        if (CollectorConstants.getUserAgent().Contains("Chrome"))
                        {
                            heapUsage = (Dictionary<String, Object>)jsExe.ExecuteScript(CollectorConstants.getHeapUsage());
                            log.Debug("Heap Usage : " + heapUsage.ToString());
                            if (heapUsage.ContainsKey("totalJSHeapSize"))
                            {
                                memoryDetails.Add("jsHeapSizeLimit", heapUsage["jsHeapSizeLimit"]);
                                memoryDetails.Add("totalJSHeapSize", heapUsage["totalJSHeapSize"]);
                                memoryDetails.Add("usedJSHeapSize", heapUsage["usedJSHeapSize"]);
                                if (getPrevTxnHeapSize() == 0)
                                {
                                    setPrevTxnHeapSize((long)heapUsage["usedJSHeapSize"]);
                                    memoryDetails.Add("currentPageUsage", getPrevTxnHeapSize());
                                }
                                else {
                                    currentHeapUsage = ((long)heapUsage["usedJSHeapSize"] - getPrevTxnHeapSize());
                                    setPrevTxnHeapSize((long)heapUsage["usedJSHeapSize"]);
                                    memoryDetails.Add("currentPageUsage", currentHeapUsage);
                                }
                            }
                            else {
                                memoryDetails.Add("jsHeapSizeLimit", 0);
                                memoryDetails.Add("totalJSHeapSize", 0);
                                memoryDetails.Add("usedJSHeapSize", 0);
                                memoryDetails.Add("currentPageUsage", 0);

                            }
                        }
                        else {
                            memoryDetails.Add("jsHeapSizeLimit", 0);
                            memoryDetails.Add("totalJSHeapSize", 0);
                            memoryDetails.Add("usedJSHeapSize", 0);
                            memoryDetails.Add("currentPageUsage", 0);
                        }
                        log.Debug("Collected Memory Details : " + memoryDetails.ToString());
                        collectedData.Add("Memory", memoryDetails);
                    }

                    //Fetch dom element count
                    long domElements = (long)jsExe.ExecuteScript(CollectorConstants.getDomLength());
                    log.Debug("DOM Element Count :{} " + domElements);
                    collectedData.Add("DomElements", domElements);


                    //Fetch Browser Details
                    browserDetails.Add("UserAgent", CollectorConstants.getUserAgent());
                    collectedData.Add("Browser", browserDetails);


                    // Fetch Client-side details Details via ResourceTiming API
                    if (isMarkAPIEnabled)
                    {
                        Thread.Sleep(CollectorConstants.getMarkWaitTime());
                        if (CollectorConstants.getUserAgent().Contains("Trident"))
                        {
                            var serializer = new System.Web.Script.Serialization.JavaScriptSerializer();
                            markDetails = serializer.Deserialize<List<Dictionary<string, object>>>
                                (jsExe.ExecuteScript(CollectorConstants.getMarkTimeIE()).ToString());
                        }
                        else
                        {
                            markDetails = ((IReadOnlyCollection<Object>)jsExe.ExecuteScript(CollectorConstants.getResourceTime())).ToDictionaries();
                        }


                        if (markDetails.Count > 0)
                        {
                            collectedData.Add("MarkTime", markDetails);
                        }
                        jsExe.ExecuteScript(CollectorConstants.clearMarkTiming());
                        log.Info("Collected mark details from Resource Timing API : " + markDetails.ToString());
                    }

                    //Store DOM if required for analysis
                    if (config.clientConfig.ContainsKey("isDOMNeeded") && Convert.ToBoolean(config.clientConfig["isDOMNeeded"].ToString()))
                    {
                        if (navType == true)
                        {
                            document = document.Append((jsExe.ExecuteScript(CollectorConstants.getDom())).ToString());
                        }
                        else {
                            document = null;
                        }
                    }
                    else {
                        document = null;
                    }

                    collectedData.Add("DOMContent", document);
                    log.Debug("DOM Element : {}" + document);


                    log.Debug("Calling data persist for " + txnName + " asynchronously");
                    AsyncpersistData(collectedData);
                    log.Debug("Completed calling data persist for " + txnName + " asynchronously");
                    rtnValue.Add("UploadStatus", "Success");
                    rtnValue.Add("RunID", config.clientConfig["RunID"]);
                    rtnValue.Add("totalTime", totalTime);
                    rtnValue.Add("serverTime", serverTime);
                    rtnValue.Add("txnName", txnName);
                    if (txnStatus == 1)
                    {
                        rtnValue.Add("txnStatus", "Pass");
                    }
                    else {
                        rtnValue.Add("txnStatus", "Fail");
                    }
                }
                else {
                    log.Error("Exception in collateData for transaction " + txnName + " in getting configuration");
                }
            }
            catch (Exception e)
            {
                log.Error("Exception in collateData for transaction " + txnName + " at " + e);
            }
            log.Debug("Completed collating data for " + txnName);
            return rtnValue;
        }

        private static void AsyncpersistData(Dictionary<String, Object> collectedData)
        {
            //new Thread(() => { PersistData(collectedData); }).Start();
            PersistData(collectedData);

        }

        private static void PersistData(Dictionary<String, Object> collectedData) {
            log.Debug("Starting  persistData for transaction " + collectedData["TxnName"]);
            String strsuccess = "Success";
            try
            {
                Dictionary<String, Object> hostDetails = new Dictionary<String, Object>();
                Dictionary<String, Object> details = new Dictionary<String, Object>();
                Dictionary<String, Object> others = new Dictionary<String, Object>();
                Dictionary<String, Double> resourceTime = new Dictionary<String, Double>();

                hostDetails.Add("name", Dns.GetHostName());

                others.Add("domElementCount", collectedData["DomElements"]);
                others.Add("dom", collectedData["DOMContent"].ToString());
                if (collectedData.ContainsKey("msFirstPaint"))
                {
                    others.Add("msFirstPaint", collectedData["msFirstPaint"]);
                }

                details.Add("ClientName", collectedData["ClientName"]);
                details.Add("ProjectName", collectedData["ProjectName"]);
                details.Add("Scenario", collectedData["Scenario"]);
                details.Add("licenseKey", collectedData.ContainsKey("licenseKey")?collectedData["licenseKey"]:"");

                details.Add("dataStoreUrl", collectedData.ContainsKey("dataStoreUrl") ? collectedData["dataStoreUrl"]:"");
                details.Add("transactionName", collectedData["TxnName"]);
                details.Add("url", collectedData["Url"]);
                details.Add("txnStatus", collectedData["txnStatus"]);
                details.Add("Release", collectedData["Release"]);
                details.Add("RunID", collectedData["RunID"].ToString());
                details.Add("RunTime", collectedData["RunTime"]);

                details.Add("BuildNumber", collectedData["BuildNumber"]);
                details.Add("staticResourceExtension", collectedData["staticResourceExtension"]);
                details.Add("imageResourceExtension", collectedData["imageResourceExtension"]);
                details.Add("resourceDurationThreshold", collectedData["resourceDurationThreshold"]);
                details.Add("source", "CodedUI");

                log.Debug("Calling calculateBackendTime");
                resourceTime = CalculateBackendTime((List<Dictionary<string, object>>)collectedData["ResourceTime"], collectedData["NavType"].ToString());
                log.Debug("Completed calculateBackendTime");
                if (Convert.ToBoolean(collectedData["NavType"]))
                {
                    details.Add("NavType", "Hard");
                    details.Add("speedIndex", collectedData["SpeedIndex"]);
                    details.Add("StartTime", collectedData["StartTime"]);
                    details.Add("resourceLoadTime", resourceTime["backendTime"]);
                    details.Add("visuallyComplete", resourceTime["totalTime"]);

                }
                else {
                    details.Add("NavType", "Soft");
                    //details.put("SoftNavTotalTime",(Long.parseLong(collectedData.get("SoftNavTotalTime").toString()) <= 0 ? 0 : collectedData.get("SoftNavTotalTime")));
                    details.Add("StartTime", collectedData["StartTime"]);
                    details.Add("resourceLoadTime", resourceTime["backendTime"]);
                    details.Add("visuallyComplete", resourceTime["totalTime"]);

                }

                Dictionary<string, object> jsonDoc = new Dictionary<string, object>();
                jsonDoc.Add("details", details);
                jsonDoc.Add("host", hostDetails);

                jsonDoc.Add("platform", collectedData["Browser"]);
                jsonDoc.Add("memory", collectedData["Memory"]);
                jsonDoc.Add("others", others);
                jsonDoc.Add("navtime", collectedData["NavigationTime"]);
                
                if (collectedData.ContainsKey("MarkTime"))
                {
                    jsonDoc.Add("marktime", markListProcessed((List<Dictionary<string, object>>)collectedData["MarkTime"]));
                }

                Boolean crawlEnabled = (collectedData.ContainsKey("isResourceCrawlingEnabled") ? Convert.ToBoolean(collectedData["isResourceCrawlingEnabled"]) : true);
                log.Debug("Crawl Enabled is " + crawlEnabled + " for " + collectedData["TxnName"]);
                if (crawlEnabled)
                {
                    jsonDoc.Add("resources", CrawlUtils.getResourceDetails((List<Dictionary<String, Object>>)collectedData["ResourceTime"]));
                }
                else
                {
                    jsonDoc.Add("resources", collectedData["ResourceTime"]);
                }

                log.Debug("Crawl Completed: ");

                log.Debug("Data collected for " + collectedData["TxnName"] + " : " + jsonDoc.ToString());
                ICXOptimizeService cxOpService = new CXOptimizeServiceImpl();
                String post = cxOpService.uploadPerformanceData(JSONUtils.MapToJsonString(jsonDoc));

                log.Debug("Post Status : " +  post);

                //Checking if the response is null. If so, then it will go to finally block
                if (post == null)
                {
                    log.Error("Unable to insert stats into datastore for " + collectedData["TxnName"] + ". Please check logs for further details.");
                    return;
                }
                if (!post.Contains(strsuccess))
                {
                    log.Error("The data could not be uploaded for " + collectedData["TxnName"] + ". The response from data store is " + post);
                }
                else {
                    log.Info("DataUploaded for succesfully for {}" + collectedData["TxnName"]);
                }

            }
            catch (Exception e)
            {
                log.Error("Exception in persistData for " + collectedData["TxnName"] + " at " + e);
            }
        }

        private static Dictionary<String, Double> CalculateBackendTime(List<Dictionary<String, Object>> resourceDetailsOrg, String NavType)
        {
            log.Debug("Starting  calculateBackendTime for transaction");
            List<Dictionary<String, Double>> resourceDetails = new List<Dictionary<String, Double>>();
            List<Dictionary<String, Double>> resourceDetails1 = new List<Dictionary<String, Double>>();
            Dictionary<String, Double> result = new Dictionary<string, double>();

            foreach (Dictionary<String, Object> resource in resourceDetailsOrg)
            {
                Dictionary<String, Double> newmap1 = new Dictionary<String, Double>();
                newmap1.Add("fetchStart", Convert.ToDouble(resource["fetchStart"]));
                newmap1.Add("responseEnd", Convert.ToDouble(resource["responseEnd"]));

                resourceDetails.Add(newmap1);
            }

            // First, sort by start time, then end time
            //log.debug("Trimmed Resource List :" + resourceDetails.toString());
            resourceDetails.Sort((o1, o2) =>
            {
                Double x1 = ((Dictionary<String, Double>)o1)["fetchStart"];
                Double x2 = ((Dictionary<String, Double>)o2)["fetchStart"];
                int sComp = x1.CompareTo(x2);

                if (sComp != 0)
                {
                    return sComp;
                }
                else
                {
                    x1 = ((Dictionary<String, Double>)o1)["responseEnd"];
                    x2 = ((Dictionary<String, Double>)o2)["responseEnd"];
                    return x1.CompareTo(x2);
                }
            });

            //log.debug("Sorted Resource List :" + resourceDetails.toString());
            int size = resourceDetails.Count;

            // Next, find all resources with the same start time, and reduce
            // them to the largest end time.S
            for (int i = 0; i < size; i++)
            {
                if (!((i != (size - 1) && Object.Equals(resourceDetails[i]["fetchStart"], resourceDetails[i + 1]["fetchStart"]))))
                {
                    resourceDetails1.Add(resourceDetails[i]);
                }
            }
            //log.debug("Reduced Resource List :" + resourceDetails1.toString());

            resourceDetails.Clear();

            // Third, for every resource, if the start is less than the end of
            // any previous resource, change its start to the end.  If the new start
            // time is more than the end time, we can discard this one.
            size = resourceDetails1.Count;
            Double furthestEnd = 0.0;
            for (int i = 0; i < size; i++)
            {
                if (resourceDetails1[i]["fetchStart"] < furthestEnd)
                {
                    resourceDetails1[i]["fetchStart"] = furthestEnd;
                }
                // as long as this resource has > 0 duration, add it to our next list
                if (resourceDetails1[i]["fetchStart"] < resourceDetails1[i]["responseEnd"])
                {
                    resourceDetails.Add(resourceDetails1[i]);
                    furthestEnd = resourceDetails1[i]["responseEnd"];
                }
            }
            //log.debug("Overlap Resource List :" + resourceDetails.toString());

            resourceDetails1.Clear();
            size = resourceDetails.Count;

            // Next, find all resources with the same start time, and reduce
            // them to the largest end time.
            for (int i = 0; i < size; i++)
            {
                if (!((i != (size - 1) && Object.Equals(resourceDetails[i]["fetchStart"], resourceDetails[i + 1]["fetchStart"]))))
                {
                    resourceDetails1.Add(resourceDetails[i]);
                }
            }
            //log.debug("Reduced Resource List :" + resourceDetails.toString());
            

            Double backendTime = 0.0;
            Double fetchStart = 0.0;
            Double responseEnd = 0.0;
            size = resourceDetails1.Count;
            for (int i = 0; i < size; i++)
            {
                backendTime = backendTime + (resourceDetails1[i]["responseEnd"] - resourceDetails1[i]["fetchStart"]);
                if (i == 0)
                {
                    fetchStart = resourceDetails1[i]["fetchStart"];
                }
                if (i == (size - 1))
                {
                    responseEnd = resourceDetails1[i]["responseEnd"];
                }

            }
            log.Debug("Backend Time :" + backendTime);

            if (Convert.ToBoolean(NavType))
            {
                result.Add("totalTime", responseEnd);
                log.Debug("TotalTime :" + responseEnd);
            }
            else
            {
                result.Add("totalTime", (responseEnd - fetchStart));
                log.Debug("TotalTime :" + (responseEnd - fetchStart));
            }

            result.Add("backendTime", backendTime);
            return result;

        }

        private static Dictionary<String, Object> markListProcessed(List<Dictionary<String, Object>> markDetails)
        {

            log.Debug("Inside markListProcessed :" + markDetails.ToString());
            Dictionary<String, Object> markMap = new Dictionary<string, object>();
            List<String> markNames = new List<string>();

            foreach (Dictionary<String, Object> mark in markDetails)
            {
                markNames.Add(mark["name"].ToString().Replace("Begin", "").Replace("End", "").Replace("Start", ""));
                markMap.Add(mark["name"].ToString(), Convert.ToBoolean(mark["startTime"]));
            }

            List<String> uniqueMarks = new List<string>(markNames);
            foreach (String s in uniqueMarks)
            {
                if (markMap.ContainsKey(s + "Start") && markMap.ContainsKey(s + "Begin"))
                {
                    markMap.Add("cal_" + s + "bs", (Convert.ToDouble(markMap[s + "Start"]) - Convert.ToDouble(markMap[s + "Begin"])));
                }
                if (markMap.ContainsKey(s + "Start") && markMap.ContainsKey(s + "End"))
                {
                    markMap.Add("cal_" + s + "es", (Convert.ToDouble(markMap[s + "End"]) - Convert.ToDouble(markMap[s + "Start"])));
                }
            }

            return markMap;
        }

    }
}
