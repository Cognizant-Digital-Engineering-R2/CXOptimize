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

package com.cognizant.pace.CXOptimize.Collector.utils;


import com.cognizant.pace.CXOptimize.Collector.config.ConfigurationLoader;
import com.cognizant.pace.CXOptimize.Collector.service.CXOptimizeService;
import com.cognizant.pace.CXOptimize.Collector.service.CXOptimizeServiceImpl;
import com.cognizant.pace.CXOptimize.Collector.constant.CollectorConstants;
//import cognizant.pace.CXOptimize.Collector.service.*;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;


public class CollectorUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorUtils.class);
    //Getter & Setter for Previous Transaction Time
    private static long PrevTxnStartTime = 0;
    //Getter & Setter for Previous Transaction Time
    private static long PrevTxnHeapSize = 0;

    public static long getPrevTxnStartTime() {
        return PrevTxnStartTime;
    }

    public static void setPrevTxnStartTime(long prevTxnStartTime)

    {
        PrevTxnStartTime = prevTxnStartTime;
    }

    public static long getPrevTxnHeapSize() {
        return PrevTxnHeapSize;
    }

    public static void setPrevTxnHeapSize(long prevTxnHeapSize)

    {
        PrevTxnHeapSize = prevTxnHeapSize;
    }

    public static boolean stringContainsItemFromList(String inputString, String match) {
        String[] items = match.split("\\s*,\\s*");
        inputString = inputString.toString().toLowerCase().split("\\?")[0];
        for (String item : items) {
            if (inputString.contains(item)) {
                return true;
            }
        }
        return false;
    }

    public static long compress(String input, final String filename) {
        long compressed = 0;
        try {
            Reader inputReader = new StringReader(input);
            StringWriter outputWriter = new StringWriter();

            if (stringContainsItemFromList(filename, ".js")) {
                JavaScriptCompressor compressor = new JavaScriptCompressor(inputReader, null);
                compressor.compress(outputWriter, -1, true, false, false, false);
                compressed = outputWriter.toString().length();
            } else {
                CssCompressor compressor = new CssCompressor(inputReader);
                compressor.compress(outputWriter, -1);
                compressed = outputWriter.toString().length();
            }
        } catch (Exception e) {
            LOGGER.debug("Error Compressing CSS or Javascript {}",e);
            LOGGER.debug("Continuing with simple minifier");
            compressed = input.replace("\\n| {2}|\\t|\\r", "").length();
        }
        return compressed;
    }

    public static Map<String, Object> extractData(String txnName, String url, WebDriver browser, int txnStatus) {
        LOGGER.debug("Started extracting data for Transaction : {} with status {}", txnName, txnStatus);
        Map<String, Object> rtnValue = new HashMap<>();
        try {
            JavascriptExecutor jsExe = (JavascriptExecutor) browser;

            LOGGER.debug("Check Browser Type");
            String userAgent = (String) jsExe.executeScript("var nAgt = navigator.userAgent|| {}; return nAgt;");
            LOGGER.debug("User Agent : {}", userAgent);

            if (userAgent != null) {
                CollectorConstants.setUserAgent(userAgent);
                rtnValue = collateData(txnName, url, jsExe, txnStatus);
            } else {
                /*Adding conditions to check if Client Config is null or User Agent is null*/
                LOGGER.debug("No performance data will be collected for {} since UserAgent:{} is null", txnName, userAgent);
                rtnValue = null;
            }
        } catch (Exception e) {
            LOGGER.error("Exception in extractData for {} at {} ", txnName,e);
        }
        LOGGER.debug("Completed extracting data for Transaction : {} with status {}", txnName, txnStatus);
        return rtnValue;

    }

    public static Map<String, Object> collateData(String txnName, String url, JavascriptExecutor jsExe, int txnStatus) {
        LOGGER.debug("Started collate data for Transaction : {} with status {}", txnName, txnStatus);
        Map<String, Object> rtnValue = new HashMap<>();
        long currNavTime;
        long totalTime = 0;
        long serverTime = 0;
        boolean navType = false;
        double speedIndex = 0;

        Map<String, Object> collectedData = new HashMap<>();

        try {
            Map<String, Object> navigationDetails = new HashMap<>();
            Map<String, Object> browserDetails = new HashMap<>();
            Map<String, Object> heapUsage = new HashMap<>();
            Map<String, Object> memoryDetails = new HashMap<>();
            ArrayList<Map<String, Object>> resourceDetails = new ArrayList<>();
            ArrayList<Map<String, Object>> markDetails = new ArrayList<>();
            boolean isNavigationAPIEnabled, isResourceAPIEnabled, isMemoryAPIEnabled, isMarkAPIEnabled;
            StringBuilder document = new StringBuilder();
            long loadEventEnd, currentHeapUsage, msFirstPaint;
            long startTime, endTime, duration;

            //Load Configuration from remote service

            ConfigurationLoader config = ConfigurationLoader.getInstance();
            if (config.clientConfig.size() > 0)
            {
                LOGGER.debug("Configuration Loading..");

                for (Map.Entry pair : config.clientConfig.entrySet()) {
                    collectedData.put(pair.getKey().toString(), pair.getValue());
                }
                collectedData.put("TxnName", txnName);
                collectedData.put("txnStatus", txnStatus);
                collectedData.put("Url", url);

                isNavigationAPIEnabled = ((config.clientConfig.containsKey("isNavigationAPIEnabled")) && Boolean.parseBoolean(config.clientConfig.get("isNavigationAPIEnabled").toString()));
                isResourceAPIEnabled = ((config.clientConfig.containsKey("isResourceAPIEnabled")) && Boolean.parseBoolean(config.clientConfig.get("isResourceAPIEnabled").toString()));
                isMemoryAPIEnabled = ((config.clientConfig.containsKey("isMemoryAPIEnabled")) && Boolean.parseBoolean(config.clientConfig.get("isMemoryAPIEnabled").toString()));
                isMarkAPIEnabled = ((config.clientConfig.containsKey("isMarkAPIEnabled")) && Boolean.parseBoolean(config.clientConfig.get("isMarkAPIEnabled").toString()));

                LOGGER.debug("Configuration Loading completed");
                LOGGER.debug("Navigation API data collection started for {}", txnName);

                if (true == isNavigationAPIEnabled)
                {
                    LOGGER.debug("User Agent : {}", CollectorConstants.getUserAgent());
                    if (CollectorConstants.getUserAgent().contains("Trident"))
                    {
                        LOGGER.debug("Processing IE logic");
                        startTime = System.currentTimeMillis();
                        do
                        {
                            loadEventEnd = Long.parseLong(jsExe.executeScript(CollectorConstants.getLoadEventEndIE()).toString());
                            endTime = System.currentTimeMillis();
                            duration = (endTime - startTime) / 1000;
                            if (duration > 180)
                            {
                                break;
                            }
                        }
                        while (loadEventEnd <= 0);


                        JSONObject jsonObj = new JSONObject((jsExe.executeScript(CollectorConstants.getNavigationTimeIE())).toString());
                        navigationDetails = JsonUtils.toMap(jsonObj);
                    }
                    else
                    {
                        LOGGER.debug("Processing other browser logic");
                        //Loop until loadEventEnd is non zero or 3 minutes to avoid infinite loop
                        startTime = System.currentTimeMillis();
                        do
                        {
                            //navigationDetails = (Map<String, Object>)jsExe.executeScript("var performance = window.performance || window.webkitPerformance || window.mozPerformance || window.msPerformance || {};var timings = performance.timing || {}; return timings.loadEventEnd;");
                            loadEventEnd = (long) jsExe.executeScript(CollectorConstants.getLoadEventEnd());
                            endTime = System.currentTimeMillis();
                            duration = (endTime - startTime) / 1000;
                            if (duration > 180)
                            {
                                break;
                            }
                        }
                        while (loadEventEnd <= 0); //Adding the loop to avoid loadEventEnd = 0

                        navigationDetails = (Map<String, Object>) jsExe.executeScript(CollectorConstants.getNavigationTime());
                        if (CollectorConstants.getUserAgent().contains("Chrome") && !CollectorConstants.getUserAgent().contains("Edge"))
                        {
                            msFirstPaint = (long) jsExe.executeScript(CollectorConstants.getFirstPaint());
							if (msFirstPaint!=-9999)
							{
								collectedData.put("msFirstPaint", msFirstPaint);
							}
							else
							{
								collectedData.put("msFirstPaint", navigationDetails.get("loadEventEnd"));
							}
								
                            
                        }
                        else
                        {
                            //No first paint event available for firefox
                            if(!CollectorConstants.getUserAgent().contains("Edge"))
                            {
                                collectedData.put("msFirstPaint", navigationDetails.get("loadEventEnd"));
                            }
                        }

                    }

                    LOGGER.debug("Collected Navigation API data {} for Transaction :{}", navigationDetails.toString(), txnName);
                    //validate if this is a new transaction. If true persist data else break immediately

                    currNavTime = (long) navigationDetails.get("navigationStart");

                    if (currNavTime > getPrevTxnStartTime())
                    {
                        setPrevTxnStartTime(currNavTime);
                        navType = true;
                        collectedData.put("StartTime", currNavTime);
                        totalTime = ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart"));
                        serverTime = ((long) navigationDetails.get("responseStart") - (long) navigationDetails.get("requestStart"));
                        LOGGER.debug("Hard Navigation : {}" + txnName);
                        speedIndex = Double.parseDouble(jsExe.executeScript(CollectorConstants.getSpeedIndex()).toString());
                        collectedData.put("SpeedIndex", speedIndex);
                        LOGGER.debug("SpeedIndex for Transaction:{} is {} ", txnName, speedIndex);

                    }
                    else
                    {
                        navType = false;
                        collectedData.put("StartTime", System.currentTimeMillis());
                        navigationDetails = null;
                        LOGGER.debug("Soft Navigation : {}" + txnName);
                    }
                    collectedData.put("NavigationTime", navigationDetails);
                    collectedData.put("NavType", navType);

                }

                //Mutation Observer Logic to wait until all resources gets downloaded


                // Fetch Client-side Resource Details via Resource Timing API
                if (true == isResourceAPIEnabled)
                {
                    //Work around to wait for all resources to download (if no resource is called in 2 seconds it just collects data)

                    long beforeLength,afterLength = 0;
                    do {
                        beforeLength = (long) jsExe.executeScript(CollectorConstants.getResourceLength());
                        Thread.sleep(CollectorConstants.getResourceSettleTime());
                        afterLength = (long) jsExe.executeScript(CollectorConstants.getResourceLength());

                    } while(beforeLength < afterLength);

                    if (CollectorConstants.getUserAgent().contains("Trident"))
                    {
                        JSONArray resources = new JSONArray((jsExe.executeScript(CollectorConstants.getResourceTimeIE())).toString());
                        resourceDetails = (ArrayList) JsonUtils.toList(resources);
                    }
                    else
                    {
                        resourceDetails = (ArrayList) jsExe.executeScript(CollectorConstants.getResourceTime());
                    }
                    jsExe.executeScript(CollectorConstants.clearResourceTiming());
                    LOGGER.debug("Collected Resource Timing API : {}", resourceDetails.toString());
                    collectedData.put("ResourceTime", resourceDetails);
                }

                if (navigationDetails == null && resourceDetails.size() <= 0)
                {
                    LOGGER.info("The transaction {} probably did not make a server request and hence will be ignored.", txnName);
                    return rtnValue;
                }

                // Fetch Client-side Memory Details via Memory API

                if (true == isMemoryAPIEnabled)
                {
                    if (CollectorConstants.getUserAgent().contains("Chrome"))
                    {
                        heapUsage = (Map<String, Object>) jsExe.executeScript(CollectorConstants.getHeapUsage());
                        LOGGER.debug("Heap Usage : {}", heapUsage.toString());
                        if (heapUsage.containsKey("totalJSHeapSize"))
                        {
                            memoryDetails.put("jsHeapSizeLimit", heapUsage.get("jsHeapSizeLimit"));
                            memoryDetails.put("totalJSHeapSize", heapUsage.get("totalJSHeapSize"));
                            memoryDetails.put("usedJSHeapSize", heapUsage.get("usedJSHeapSize"));
                            if (getPrevTxnHeapSize() == 0)
                            {
                                setPrevTxnHeapSize((long) heapUsage.get("usedJSHeapSize"));
                                memoryDetails.put("currentPageUsage", getPrevTxnHeapSize());
                            }
                            else
                            {
                                currentHeapUsage = ((long) heapUsage.get("usedJSHeapSize") - getPrevTxnHeapSize());
                                setPrevTxnHeapSize((long) heapUsage.get("usedJSHeapSize"));
                                memoryDetails.put("currentPageUsage", currentHeapUsage);
                            }
                        }
                        else
                        {
                            memoryDetails.put("jsHeapSizeLimit", 0);
                            memoryDetails.put("totalJSHeapSize", 0);
                            memoryDetails.put("usedJSHeapSize", 0);
                            memoryDetails.put("currentPageUsage", 0);
                        }
                    }
                    else
                    {
                        memoryDetails.put("jsHeapSizeLimit", 0);
                        memoryDetails.put("totalJSHeapSize", 0);
                        memoryDetails.put("usedJSHeapSize", 0);
                        memoryDetails.put("currentPageUsage", 0);
                    }
                    LOGGER.debug("Collected Memory Details : {}", memoryDetails.toString());
                    collectedData.put("Memory", memoryDetails);
                }

                //Fetch dom element count
                long domElements = (long) jsExe.executeScript(CollectorConstants.getDomLength());
                LOGGER.debug("DOM Element Count :{} " + domElements);
                collectedData.put("DomElements", domElements);


                //Fetch Browser Details
                browserDetails.put("UserAgent", CollectorConstants.getUserAgent());
                collectedData.put("Browser", browserDetails);


                // Fetch Client-side details Details via ResourceTiming API
                if (true == isMarkAPIEnabled)
                {
                    Thread.sleep(CollectorConstants.getMarkWaitTime());
                    if (CollectorConstants.getUserAgent().contains("Trident"))
                    {
                        JSONArray mark = new JSONArray((jsExe.executeScript(CollectorConstants.getMarkTimeIE())).toString());
                        markDetails = (ArrayList) JsonUtils.toList(mark);
                    }
                    else
                    {
                        markDetails = (ArrayList) jsExe.executeScript(CollectorConstants.getMarkTime());
                    }

                    if (markDetails.size() > 0)
                    {
                        collectedData.put("MarkTime", markDetails);
                    }
                    jsExe.executeScript(CollectorConstants.clearMarkTiming());
                    LOGGER.info("Collected mark details from Resource Timing API :{} " + markDetails.toString());
                }

                //Store DOM if required for analysis
                if (config.clientConfig.containsKey("isDOMNeeded") && Boolean.parseBoolean(config.clientConfig.get("isDOMNeeded").toString()) == true)
                {
                    if (navType == true)
                    {
                        document = document.append((jsExe.executeScript(CollectorConstants.getDom())).toString());
                    }
                    else
                    {
                        document = null;
                    }
                }
                else
                {
                    document = null;
                }

                collectedData.put("DOMContent", document);
                LOGGER.debug("DOM Element : {}" + document);


                LOGGER.debug("Calling data persist for {} asynchronously", txnName);
                long scriptTime = (System.currentTimeMillis()- CollectorConstants.getScriptStartTime());
                LOGGER.debug("Script Execution Time for collecting Performance Data for {} : {} ms.",txnName,scriptTime);
                collectedData.put("ScriptTime", scriptTime);
                asyncpersistData(collectedData);
                LOGGER.debug("Completed calling data persist for {} asynchronously", txnName);
                rtnValue.put("UploadStatus", "Success");
                rtnValue.put("RunID", config.clientConfig.get("RunID"));
                rtnValue.put("totalTime", totalTime);
                rtnValue.put("serverTime", serverTime);
                rtnValue.put("txnName", txnName);
                if (txnStatus == 1)
                {
                    rtnValue.put("txnStatus", "Pass");
                }
                else
                {
                    rtnValue.put("txnStatus", "Fail");
                }
            } else {
                LOGGER.error("Exception in collateData for transaction {} in getting configuration", txnName);
            }
        } catch (Exception e) {
            LOGGER.error("Exception in collateData for transaction {} at {}", txnName,e);
        }
        LOGGER.debug("Completed collating data for {}", txnName);
        return rtnValue;
    }

    private static void asyncpersistData(final Map<String, Object> collectedData) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    persistData(collectedData);
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Exception in asyncpersistData for {} at {}", collectedData.get("TxnName"),e);
                }
            }
        };
        new Thread(task, collectedData.get("TxnName").toString()).start();
    }

    private static void persistData(Map<String, Object> collectedData) throws InterruptedException, ExecutionException {
        LOGGER.debug("Starting  persistData for transaction {}", collectedData.get("TxnName"));
        String strsuccess = "Success";
        try {
            Map<String, Object> hostDetails = new HashMap<>();
            Map<String, Object> details = new HashMap<>();
            Map<String, Object> others = new HashMap<>();
            Map<String, Double> resourceTime = new HashMap<>();

            InetAddress inetAddr = InetAddress.getLocalHost();
            hostDetails.put("name", inetAddr.getHostName());

            others.put("domElementCount", collectedData.get("DomElements"));
            others.put("dom", collectedData.get("DOMContent"));
            if (collectedData.containsKey("msFirstPaint")) {
                others.put("msFirstPaint", collectedData.get("msFirstPaint"));
            }

            details.put("ClientName", collectedData.get("ClientName"));
            details.put("ProjectName", collectedData.get("ProjectName"));
            details.put("Scenario", collectedData.get("Scenario"));
            details.put("licenseKey", collectedData.get("licenseKey"));
            details.put("dataStoreUrl", collectedData.get("dataStoreUrl"));
            details.put("transactionName", collectedData.get("TxnName").toString().trim());
            details.put("url", collectedData.get("Url"));
            details.put("txnStatus", collectedData.get("txnStatus"));
            details.put("Release", collectedData.get("Release"));
            details.put("RunID", (collectedData.get("RunID").toString()));
            details.put("RunTime", collectedData.get("RunTime"));
            details.put("BuildNumber", collectedData.get("BuildNumber"));
            details.put("staticResourceExtension", collectedData.get("staticResourceExtension"));
            details.put("imageResourceExtension", collectedData.get("imageResourceExtension"));
            details.put("resourceDurationThreshold", collectedData.get("resourceDurationThreshold"));
            details.put("source", "Selenium");
            details.put("ScriptTime", collectedData.get("ScriptTime"));

            LOGGER.debug("Calling calculateBackendTime");
            resourceTime = calculateBackendTime((ArrayList) collectedData.get("ResourceTime"), collectedData.get("NavType").toString());
            LOGGER.debug("Completed calculateBackendTime");
            if (Boolean.valueOf(collectedData.get("NavType").toString())) {
                details.put("NavType", "Hard");
                details.put("speedIndex", collectedData.get("SpeedIndex"));
                details.put("StartTime", collectedData.get("StartTime"));
                details.put("resourceLoadTime", resourceTime.get("backendTime"));
                details.put("visuallyComplete", resourceTime.get("totalTime"));

            } else {
                details.put("NavType", "Soft");
                //details.put("SoftNavTotalTime",(Long.parseLong(collectedData.get("SoftNavTotalTime").toString()) <= 0 ? 0 : collectedData.get("SoftNavTotalTime")));
                details.put("StartTime", collectedData.get("StartTime"));
                details.put("resourceLoadTime", resourceTime.get("backendTime"));
                details.put("visuallyComplete", resourceTime.get("totalTime"));

            }

            JSONObject jsonDoc = new JSONObject();
            jsonDoc.put("details", details);
            jsonDoc.put("host", hostDetails);
            jsonDoc.put("platform", collectedData.get("Browser"));
            jsonDoc.put("memory", collectedData.get("Memory"));
            jsonDoc.put("others", others);
            jsonDoc.put("navtime", collectedData.get("NavigationTime"));
            if (collectedData.containsKey("MarkTime")) {
                jsonDoc.put("marktime", markListProcessed((ArrayList) collectedData.get("MarkTime")));
            }

            boolean crawlEnabled = (collectedData.containsKey("isResourceCrawlingEnabled") ? Boolean.valueOf(collectedData.get("isResourceCrawlingEnabled").toString()) : true);
            LOGGER.debug("Crawl Enabled is {} for {}", crawlEnabled, collectedData.get("TxnName"));
            if (crawlEnabled) {
                jsonDoc.put("resources", CrawlUtils.getResourceDetails((ArrayList) collectedData.get("ResourceTime")));
            } else {
                jsonDoc.put("resources", collectedData.get("ResourceTime"));
            }

            LOGGER.debug("Crawl Completed: ");

            LOGGER.debug("Data collected for {} : {}", collectedData.get("TxnName"), jsonDoc.toString());
            CXOptimizeService cxOpService = new CXOptimizeServiceImpl();
            String post = cxOpService.uploadPerformanceData(jsonDoc.toString());

            LOGGER.debug("Post Status : {}", post);

            //Checking if the response is null. If so, then it will go to finally block
            if (post == null) {
                LOGGER.error("Unable to insert stats into datastore for {}. Please check logs for further details.", collectedData.get("TxnName"));
                return;
            }
            if (!post.contains(strsuccess)) {
                LOGGER.error("The data could not be uploaded for {}. The response from data store is {}", collectedData.get("TxnName"), post);
            } else {
                LOGGER.info("DataUploaded for succesfully for {}", collectedData.get("TxnName"));
            }

        } catch (Exception e) {
            LOGGER.error("Exception in persistData for {} at {}", collectedData.get("TxnName"),e);
        }
    }

    private static Map<String, Double> calculateBackendTime(ArrayList<Map<String, Object>> resourceDetailsOrg, String NavType) {
        LOGGER.debug("Starting  calculateBackendTime for transaction");
        ArrayList<Map<String, Double>> resourceDetails = new ArrayList<>();
        ArrayList<Map<String, Double>> resourceDetails1 = new ArrayList<>();
        HashMap<String, Double> newmap1 = new HashMap<>();
        Map<String, Double> result = new HashMap<>();
        for (Map<String, Object> resource : resourceDetailsOrg) {
            newmap1.put("fetchStart", Double.parseDouble(resource.get("fetchStart").toString()));
            newmap1.put("responseEnd", Double.parseDouble(resource.get("responseEnd").toString()));
            resourceDetails.add((HashMap) newmap1.clone());
        }

        // First, sort by start time, then end time
        LOGGER.debug("Trimmed Resource List :" + resourceDetails.toString());
        Collections.sort(resourceDetails, new Comparator() {

            public int compare(Object o1, Object o2) {

                Double x1 = ((Map<String, Double>) o1).get("fetchStart");
                Double x2 = ((Map<String, Double>) o2).get("fetchStart");
                int sComp = x1.compareTo(x2);

                if (sComp != 0) {
                    return sComp;
                } else {
                    x1 = ((Map<String, Double>) o1).get("responseEnd");
                    x2 = ((Map<String, Double>) o2).get("responseEnd");
                    return x1.compareTo(x2);
                }
            }
        });

        LOGGER.debug("Sorted Resource List :{}", resourceDetails.toString());
        int size = resourceDetails.size();
        // Next, find all resources with the same start time, and reduce
        // them to the largest end time.
        for (int i = 0; i < size; i++) {

            if (i != (size - 1) && Objects.equals(resourceDetails.get(i).get("fetchStart"), resourceDetails.get(i + 1).get("fetchStart"))) {
            } else {
                resourceDetails1.add(resourceDetails.get(i));
            }
        }

        LOGGER.debug("Reduced Resource List :{}", resourceDetails1.toString());

        resourceDetails.clear();

        // Third, for every resource, if the start is less than the end of
        // any previous resource, change its start to the end.  If the new start
        // time is more than the end time, we can discard this one.
        size = resourceDetails1.size();
        Double furthestEnd = 0.0;
        for (int i = 0; i < size; i++) {
            if (resourceDetails1.get(i).get("fetchStart") < furthestEnd) {
                resourceDetails1.get(i).put("fetchStart", furthestEnd);
            }
            // as long as this resource has > 0 duration, add it to our next list
            if (resourceDetails1.get(i).get("fetchStart") < resourceDetails1.get(i).get("responseEnd")) {
                resourceDetails.add(resourceDetails1.get(i));
                furthestEnd = resourceDetails1.get(i).get("responseEnd");
            }
        }
        LOGGER.debug("Overlap Resource List :{}", resourceDetails.toString());

        resourceDetails1.clear();
        size = resourceDetails.size();
        // Next, find all resources with the same start time, and reduce
        // them to the largest end time.
        for (int i = 0; i < size; i++) {
            if (i != (size - 1) && Objects.equals(resourceDetails.get(i).get("fetchStart"), resourceDetails.get(i + 1).get("fetchStart"))) {
            } else {
                resourceDetails1.add(resourceDetails.get(i));
            }
        }
        LOGGER.debug("Reduced Resource List :{}", resourceDetails.toString());

        Double backendTime = 0.0;
        Double fetchStart = 0.0;
        Double responseEnd = 0.0;
        size = resourceDetails1.size();
        for(int i = 0; i < size; i++)
        {
            backendTime = backendTime + (resourceDetails1.get(i).get("responseEnd") - resourceDetails1.get(i).get("fetchStart"));
            if(i==0)
            {
                fetchStart = resourceDetails1.get(i).get("fetchStart");
            }
            if(i == (size-1))
            {
                responseEnd = resourceDetails1.get(i).get("responseEnd");
            }

        }
        LOGGER.debug("Backend Time :{}",backendTime);

        if (Boolean.valueOf(NavType))
        {
            result.put("totalTime", responseEnd);
            LOGGER.debug("TotalTime :{}",responseEnd);
        }
        else
        {
            result.put("totalTime", (responseEnd - fetchStart));
            LOGGER.debug("TotalTime :{}",(responseEnd - fetchStart));
        }

        result.put("backendTime", backendTime);
        return result;
    }

    private static Map<String, Object> markListProcessed(ArrayList<Map<String, Object>> markDetails) {

        LOGGER.debug("Inside markListProcessed :{}", markDetails.toString());
        HashMap<String, Object> markMap = new HashMap<>();
        List<String> markNames = new ArrayList<>();

        for (Map<String, Object> mark : markDetails) {
            markNames.add(mark.get("name").toString().replace("Begin", "").replace("End", "").replace("Start", ""));
            markMap.put(mark.get("name").toString(), Double.parseDouble(mark.get("startTime").toString()));
        }

        Set<String> uniqueMarks = new HashSet<>(markNames);
        for (String s : uniqueMarks) {
            if (markMap.containsKey(s + "Start") && markMap.containsKey(s + "Begin")) {
                markMap.put("cal_" + s + "bs", (Double.parseDouble(markMap.get(s + "Start").toString()) - Double.parseDouble(markMap.get(s + "Begin").toString())));
            }
            if (markMap.containsKey(s + "Start") && markMap.containsKey(s + "End")) {
                markMap.put("cal_" + s + "es", (Double.parseDouble(markMap.get(s + "End").toString()) - Double.parseDouble(markMap.get(s + "Start").toString())));
            }
        }

        return markMap;
    }
}
