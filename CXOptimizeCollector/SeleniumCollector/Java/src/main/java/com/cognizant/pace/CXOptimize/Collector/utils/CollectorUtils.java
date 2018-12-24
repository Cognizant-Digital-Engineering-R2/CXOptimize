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
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;


public class CollectorUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorUtils.class);


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


    public static Map<String, Object> extractData(String txnName, String url, WebDriver browser, int txnStatus) {
        LOGGER.debug("CXOP - {} - Started extracting data", txnName);
        Map<String, Object> rtnValue = new HashMap<>();
        try {
            JavascriptExecutor jsExe = (JavascriptExecutor) browser;
            String userAgent = (String) jsExe.executeScript("var nAgt = navigator.userAgent|| {}; return nAgt;");
            LOGGER.debug("CXOP - {} - User Agent:{}", txnName, userAgent);

            if (userAgent != null) {
                CollectorConstants.setUserAgent(userAgent);
                rtnValue = collateData(txnName, url, jsExe, txnStatus);
                LOGGER.debug("CXOP - {} - Completed extracting data", txnName);
            } else {
                /*Adding conditions to check if Client Config is null or User Agent is null*/
                LOGGER.debug("CXOP - {} -  No performance data will be collected since UserAgent is {}", txnName, userAgent);
                rtnValue = null;
            }
        } catch (Exception e) {
            LOGGER.error("CXOP - {} - Exception in extractData at {} ", txnName, e);
        }

        return rtnValue;

    }

    public static Map<String, Object> collateData(String txnName, String url, JavascriptExecutor jsExe, int txnStatus) {
        LOGGER.debug("CXOP - {} - Started collate data", txnName);
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
            ArrayList<Map<String, Object>> filteredResources = new ArrayList<>();
            ArrayList<Map<String, Object>> markDetails = new ArrayList<>();
            boolean isNavigationAPIEnabled, isResourceAPIEnabled, isMemoryAPIEnabled, isMarkAPIEnabled;
            StringBuilder document = new StringBuilder();
            long loadEventEnd, currentHeapUsage, msFirstPaint;
            long startTime, endTime, duration;

            //Load Configuration from remote service
            LOGGER.debug("CXOP - {} - Started Loading Configuration", txnName);
            ConfigurationLoader config = ConfigurationLoader.getInstance();
            if (config.clientConfig.size() > 0) {


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

                LOGGER.debug("CXOP - {} - Completed Loading Configuration", txnName);

                LOGGER.debug("CXOP - {} - Navigation API data collection started", txnName);

                if (true == isNavigationAPIEnabled) {
                    LOGGER.debug("CXOP - {} - User Agent : {}", txnName, CollectorConstants.getUserAgent());
                    if (CollectorConstants.getUserAgent().contains("Trident")) {
                        LOGGER.debug("CXOP - {} - Collecting Navigation Timing Details for IE 11 or below", txnName);

                        loadEventEnd = Long.parseLong(jsExe.executeScript(CollectorConstants.getLoadEventEndIE()).toString());

                        startTime = System.currentTimeMillis();

                        while (loadEventEnd <= 0) {
                            loadEventEnd = Long.parseLong(jsExe.executeScript(CollectorConstants.getLoadEventEndIE()).toString());
                            endTime = System.currentTimeMillis();
                            duration = (endTime - startTime) / 1000;
                            if (duration > 60) {
                                break;
                            }
                            Thread.sleep(10000);
                            LOGGER.debug("CXOP - {} - Rechecking loadEventEnd since it is {}", txnName, loadEventEnd);
                        }


                        JSONObject jsonObj = new JSONObject((jsExe.executeScript(CollectorConstants.getNavigationTimeIE())).toString());
                        navigationDetails = JsonUtils.toMap(jsonObj);
                        LOGGER.debug("CXOP - {} - Navigation Timing Details : {}", txnName, navigationDetails.toString());
                    } else {
                        LOGGER.debug("CXOP - {} - Collecting Navigation Timing Details for other than IE11", txnName);
                        //Loop until loadEventEnd is non zero or 3 minutes to avoid infinite loop

                        loadEventEnd = (long) jsExe.executeScript(CollectorConstants.getLoadEventEnd());
                        startTime = System.currentTimeMillis();

                        while (loadEventEnd <= 0) {
                            loadEventEnd = (long) jsExe.executeScript(CollectorConstants.getLoadEventEnd());
                            endTime = System.currentTimeMillis();
                            duration = (endTime - startTime) / 1000;
                            if (duration > 60) {
                                break;
                            }
                            Thread.sleep(10000);
                            LOGGER.debug("CXOP - {} - Rechecking loadEventEnd since it is {}", txnName, loadEventEnd);
                        }

                        navigationDetails = (Map<String, Object>) jsExe.executeScript(CollectorConstants.getNavigationTime());
                        LOGGER.debug("CXOP - {} - Navigation Timing Details : {}", txnName, navigationDetails.toString());

                    }

                    LOGGER.debug("CXOP - {} - Navigation API data collection completed", txnName);

                    //validate if this is a new transaction. If true persist data else break immediately

                    LOGGER.debug("CXOP - {} - Previous Start Time : {}", txnName, CollectorConstants.getPrevTxnStartTime());

                    currNavTime = (long) navigationDetails.get("navigationStart");

                    LOGGER.debug("CXOP - {} - Current Start Time : {}", txnName, navigationDetails.get("navigationStart").toString());

                    if (currNavTime > CollectorConstants.getPrevTxnStartTime()) {
                        CollectorConstants.setPrevTxnStartTime(currNavTime);
                        navType = true;
                        LOGGER.debug("CXOP - {} - Setting Navigation Type to : {}", txnName, "Hard");
                        collectedData.put("StartTime", currNavTime);
                        totalTime = ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart"));
                        serverTime = ((long) navigationDetails.get("responseStart") - (long) navigationDetails.get("requestStart"));
                        ArrayList speedIdx = (ArrayList) jsExe.executeScript(CollectorConstants.getSpeedIndex());

                        if (speedIdx.size() > 0) {
                            if (Double.parseDouble(speedIdx.get(0).toString()) <= 0) {
                                if (loadEventEnd > 0) {
                                    collectedData.put("SpeedIndex", ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart")));

                                } else {
                                    collectedData.put("SpeedIndex", ((long) navigationDetails.get("domComplete") - (long) navigationDetails.get("navigationStart")));
                                }
                            } else {
                                collectedData.put("SpeedIndex", speedIdx.get(0));
                            }

                            if (Double.parseDouble(speedIdx.get(1).toString()) > 0) {
                                collectedData.put("msFirstPaint", speedIdx.get(1));

                            } else {
                                if (CollectorConstants.getUserAgent().contains("Trident")) {
                                    collectedData.put("msFirstPaint", ((long) navigationDetails.get("msFirstPaint") - (long) navigationDetails.get("navigationStart")));
                                } else{
                                    if (loadEventEnd > 0) {
                                        collectedData.put("msFirstPaint", ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart")));
                                    } else {
                                        collectedData.put("msFirstPaint", ((long) navigationDetails.get("domComplete") - (long) navigationDetails.get("navigationStart")));
                                    }

                                }

                            }

                        } else {
                            LOGGER.debug("CXOP - {} - Failed to get result for SpeedIndex script.Setting SpeedIndex to fallback logic", txnName);
                            if (CollectorConstants.getUserAgent().contains("Trident")) {
                                if (loadEventEnd > 0) {
                                    collectedData.put("SpeedIndex", ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart")));

                                } else {
                                    collectedData.put("SpeedIndex", ((long) navigationDetails.get("domComplete") - (long) navigationDetails.get("navigationStart")));

                                }
                                collectedData.put("msFirstPaint", ((long) navigationDetails.get("msFirstPaint") - (long) navigationDetails.get("navigationStart")));
                            } else{
                            if (loadEventEnd > 0) {
                                collectedData.put("SpeedIndex", ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart")));
                                collectedData.put("msFirstPaint", ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart")));
                            } else {
                                collectedData.put("SpeedIndex", ((long) navigationDetails.get("domComplete") - (long) navigationDetails.get("navigationStart")));
                                collectedData.put("msFirstPaint", ((long) navigationDetails.get("domComplete") - (long) navigationDetails.get("navigationStart")));
                            }}

                        }
                        LOGGER.debug("CXOP - {} - SpeedIndex : {} & First Paint : {}", txnName, collectedData.get("SpeedIndex"), collectedData.get("msFirstPaint"));


                    } else {
                        navType = false;
                        LOGGER.debug("CXOP - {} - Setting Navigation Type to : {}", txnName, "Soft");
                        collectedData.put("StartTime", CollectorConstants.getScriptStartTime());
                        navigationDetails = null;
                    }
                    collectedData.put("NavigationTime", navigationDetails);
                    collectedData.put("NavType", navType);

                }


                // Fetch Client-side Resource Details via Resource Timing API
                if (true == isResourceAPIEnabled) {
                    //Work around to wait for all resources to download (if no resource is called in 2 seconds it just collects data).
                    //Mutation Observer Logic to wait until all resources gets downloaded - todoitem

                    LOGGER.debug("CXOP - {} - Checking if all resource are downloaded", txnName);
                    LOGGER.debug("CXOP - {} - Resource Length Javascript : {}", txnName,CollectorConstants.getResourceLengthScript());
                    long beforeLength, afterLength = 0;
                    do {
                        beforeLength = (long) jsExe.executeScript(CollectorConstants.getResourceLengthScript());
                        Thread.sleep(CollectorConstants.getResourceSettleTime());
                        afterLength = (long) jsExe.executeScript(CollectorConstants.getResourceLengthScript());
                        LOGGER.debug("CXOP - {} - Resource count before {} seconds is {} and current count {}", txnName, CollectorConstants.getResourceSettleTime(), beforeLength, afterLength);
                    } while (beforeLength < afterLength);

                    LOGGER.debug("CXOP - {} - All resource are downloaded", txnName);

                    LOGGER.debug("CXOP - {} - Started Collecting Resource Timing Metrics", txnName);
                    if (CollectorConstants.getUserAgent().contains("Trident")) {
                        JSONArray resources = new JSONArray((jsExe.executeScript(CollectorConstants.getResourceTimeIE())).toString());
                        resourceDetails = (ArrayList) JsonUtils.toList(resources);
                    } else {
                        resourceDetails = (ArrayList) jsExe.executeScript(CollectorConstants.getResourceTime());
                    }
                    jsExe.executeScript(CollectorConstants.clearResourceTiming());

                    if (CollectorConstants.getManualResourceTimeClear().equals("true") && !navType) {
                        LOGGER.debug("CXOP - {} - Manual Resource Truncation is enabled", txnName);
                        LOGGER.debug("CXOP - {} - Size of resource before Manual Resource Truncation : {}", txnName, resourceDetails.size());
                        LOGGER.debug("CXOP - {} - Previous Hard Txn Start : {} , Script Start : {} ", txnName, CollectorConstants.getPrevTxnStartTime(),CollectorConstants.getScriptStartTime());
                        long diff =  (CollectorConstants.getScriptStartTime() - CollectorConstants.getPrevTxnStartTime());
                        LOGGER.debug("CXOP - {} - Time between first transaction and current transaction : {}", txnName, diff);
                        //remove all resources start time less than the difference
                        LOGGER.debug("CXOP - {} - Resource Before Truncation {}", resourceDetails.toString());
                        resourceDetails.removeIf(s -> (CollectorConstants.getPrevTxnStartTime() + Long.parseLong(s.get("startTime").toString())) < diff);
                        LOGGER.debug("CXOP - {} - Resource After Truncation {}", resourceDetails.toString());
                        LOGGER.debug("CXOP - {} - Size of resource after Manual Resource Truncation : {}", txnName, resourceDetails.size());
                    }

                    filteredResources = CollectorUtils.filterResources(resourceDetails);

                    collectedData.put("ResourceTime", filteredResources);
                    LOGGER.debug("CXOP - {} - Completed Collecting Resource Timing Metrics : {}", txnName, filteredResources.toString());

                    Map<String, Double> resourceTime;
                    LOGGER.debug("CXOP - {} - Calling Calculate BackendTime", txnName);
                    resourceTime = calculateBackendTime((ArrayList) collectedData.get("ResourceTime"), collectedData.get("NavType").toString(), txnName);
                    LOGGER.debug("CXOP - {} - Completed Calculate BackendTime : {}", collectedData.get("TxnName").toString(), txnName);
                    totalTime = resourceTime.get("totalTime").longValue();
                    serverTime = serverTime + resourceTime.get("backendTime").longValue();

                    collectedData.put("resourceLoadTime", resourceTime.get("backendTime"));
                    collectedData.put("visuallyComplete", resourceTime.get("totalTime"));

                } else {
                    collectedData.put("resourceLoadTime", 0);
                    collectedData.put("visuallyComplete", ((long) navigationDetails.get("loadEventEnd") - (long) navigationDetails.get("navigationStart")));
                }

                if (navigationDetails == null && resourceDetails.size() <= 0) {
                    LOGGER.info("CXOP - {} - probably did not make a server request and hence will be ignored.", txnName);
                    return rtnValue;
                }

                // Fetch Client-side Memory Details via Memory API

                if (true == isMemoryAPIEnabled) {
                    LOGGER.debug("CXOP - {} - Collecting Heap Usage", txnName);
                    if (CollectorConstants.getUserAgent().contains("Chrome")) {
                        heapUsage = (Map<String, Object>) jsExe.executeScript(CollectorConstants.getHeapUsage());

                        if (heapUsage.containsKey("totalJSHeapSize")) {
                            memoryDetails.put("jsHeapSizeLimit", heapUsage.get("jsHeapSizeLimit"));
                            memoryDetails.put("totalJSHeapSize", heapUsage.get("totalJSHeapSize"));
                            memoryDetails.put("usedJSHeapSize", heapUsage.get("usedJSHeapSize"));
                            if (CollectorConstants.getPrevTxnHeapSize() == 0) {
                                CollectorConstants.setPrevTxnHeapSize((long) heapUsage.get("usedJSHeapSize"));
                                memoryDetails.put("currentPageUsage", CollectorConstants.getPrevTxnHeapSize());
                            } else {
                                currentHeapUsage = ((long) heapUsage.get("usedJSHeapSize") - CollectorConstants.getPrevTxnHeapSize());
                                CollectorConstants.setPrevTxnHeapSize((long) heapUsage.get("usedJSHeapSize"));
                                memoryDetails.put("currentPageUsage", currentHeapUsage);
                            }
                        } else {
                            memoryDetails.put("jsHeapSizeLimit", 0);
                            memoryDetails.put("totalJSHeapSize", 0);
                            memoryDetails.put("usedJSHeapSize", 0);
                            memoryDetails.put("currentPageUsage", 0);
                        }
                    } else {
                        memoryDetails.put("jsHeapSizeLimit", 0);
                        memoryDetails.put("totalJSHeapSize", 0);
                        memoryDetails.put("usedJSHeapSize", 0);
                        memoryDetails.put("currentPageUsage", 0);
                    }
                    collectedData.put("Memory", memoryDetails);
                    LOGGER.debug("CXOP - {} - Completed Collecting Heap Usage : {}", txnName, memoryDetails.toString());
                }

                //Fetch dom element count
                long domElements = (long) jsExe.executeScript(CollectorConstants.getDomLength());
                collectedData.put("DomElements", domElements);
                LOGGER.debug("CXOP - {} - DOM Element Count :{} ", txnName, domElements);


                //Fetch Browser Details
                browserDetails.put("UserAgent", CollectorConstants.getUserAgent());
                collectedData.put("Browser", browserDetails);


                // Fetch Client-side details Details via ResourceTiming API
                if (true == isMarkAPIEnabled) {
                    LOGGER.debug("CXOP - {} - Collecting Custom Mark using Performance Mark", txnName);
                    Thread.sleep(CollectorConstants.getMarkWaitTime());
                    if (CollectorConstants.getUserAgent().contains("Trident")) {
                        JSONArray mark = new JSONArray((jsExe.executeScript(CollectorConstants.getMarkTimeIE())).toString());
                        markDetails = (ArrayList) JsonUtils.toList(mark);
                    } else {
                        markDetails = (ArrayList) jsExe.executeScript(CollectorConstants.getMarkTime());
                    }

                    if (markDetails.size() > 0) {
                        collectedData.put("MarkTime", markDetails);
                    }
                    jsExe.executeScript(CollectorConstants.clearMarkTiming());
                    LOGGER.debug("CXOP - {} - Completed collecting Custom Mark using Performance Mark : {}", txnName, markDetails.toString());
                }

                //Store DOM if required for analysis
                if (config.clientConfig.containsKey("isDOMNeeded") && Boolean.parseBoolean(config.clientConfig.get("isDOMNeeded").toString()) == true) {
                    if (navType == true) {
                        document = document.append((jsExe.executeScript(CollectorConstants.getDom())).toString());
                    } else {
                        document = null;
                    }
                } else {
                    document = null;
                }

                collectedData.put("DOMContent", document);
                //LOGGER.debug("CXOP - {} - DOM : {}",txnName,document);


                LOGGER.debug("CXOP - {} - Calling data persist for asynchronously", txnName);
                long scriptTime = (System.currentTimeMillis() - CollectorConstants.getScriptStartTime());
                LOGGER.debug("CXOP - {} - Script Execution Time for collecting Performance Data : {} ms.", txnName, scriptTime);
                collectedData.put("ScriptTime", scriptTime);
                asyncpersistData(collectedData);
                LOGGER.debug("CXOP - {} - Completed calling data persist for asynchronously", txnName);
                rtnValue.put("UploadStatus", "Success");
                rtnValue.put("RunID", config.clientConfig.get("RunID"));
                rtnValue.put("totalTime", totalTime);
                rtnValue.put("serverTime", serverTime);
                rtnValue.put("txnName", txnName);
                if (txnStatus == 1) {
                    rtnValue.put("txnStatus", "Pass");
                } else {
                    rtnValue.put("txnStatus", "Fail");
                }
                LOGGER.debug("CXOP - {} - Completed collateData successfully", txnName);
            } else {
                LOGGER.error("CXOP - {} - Exception in collateData for getting configuration", txnName);
            }
        } catch (Exception e) {
            LOGGER.error("CXOP - {} - Exception in collateData at {}", txnName, e);
        }

        return rtnValue;
    }

    private static void asyncpersistData(final Map<String, Object> collectedData) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    persistData(collectedData);
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("CXOP - {} - Exception in asyncpersistData at {}", collectedData.get("TxnName"), e);
                }
            }
        };
        new Thread(task, collectedData.get("TxnName").toString()).start();
    }

    private static void persistData(Map<String, Object> collectedData) throws InterruptedException, ExecutionException {
        LOGGER.debug("CXOP - {} - Starting  persistData", collectedData.get("TxnName"));
        String strsuccess = "Success";
        try {
            Map<String, Object> hostDetails = new HashMap<>();
            Map<String, Object> details = new HashMap<>();
            Map<String, Object> others = new HashMap<>();


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


            if (Boolean.valueOf(collectedData.get("NavType").toString())) {
                details.put("NavType", "Hard");
                details.put("speedIndex", collectedData.get("SpeedIndex"));
            } else {
                details.put("NavType", "Soft");

            }

            details.put("StartTime", collectedData.get("StartTime"));
            details.put("resourceLoadTime", collectedData.get("resourceLoadTime"));
            details.put("visuallyComplete", collectedData.get("visuallyComplete"));

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

            LOGGER.debug("CXOP - {} - Crawl Enabled : {}", collectedData.get("TxnName"), crawlEnabled);

            if (crawlEnabled) {
                jsonDoc.put("resources", CrawlUtils.getResourceDetails((ArrayList) collectedData.get("ResourceTime")));
            } else {
                jsonDoc.put("resources", collectedData.get("ResourceTime"));
            }

            LOGGER.debug("CXOP - {} - Crawl Completed", collectedData.get("TxnName"));

            LOGGER.debug("CXOP - {} - Data collected : {}", collectedData.get("TxnName"), jsonDoc.toString());
            CXOptimizeService cxOpService = new CXOptimizeServiceImpl();
            String post = cxOpService.uploadPerformanceData(jsonDoc.toString());

            LOGGER.debug("CXOP - {} - Post Status : {}", collectedData.get("TxnName"), post);

            //Checking if the response is null. If so, then it will go to finally block
            if (post == null) {
                LOGGER.error("CXOP - {} - Unable to insert stats into datastore. Please check logs for further details.", collectedData.get("TxnName"));
                return;
            }
            if (!post.contains(strsuccess)) {
                LOGGER.error("CXOP - {} - The data could not be uploaded. The response from data store is {}", collectedData.get("TxnName"), post);
            } else {
                LOGGER.info("CXOP - {} - Data uploaded succesfully", collectedData.get("TxnName"));
            }

        } catch (Exception e) {
            LOGGER.error("CXOP - {} - Exception in persistData at {}", collectedData.get("TxnName"), e);
        }
    }

    private static Map<String, Double> calculateBackendTime(ArrayList<Map<String, Object>> resourceDetailsOrg, String NavType, String txnName) {
        LOGGER.debug("CXOP - {} - Starting  calculateBackendTime with {} Navigation", txnName, NavType);
        ArrayList<Map<String, Double>> resourceDetails = new ArrayList<>();
        ArrayList<Map<String, Double>> resourceDetails1 = new ArrayList<>();
        HashMap<String, Double> newmap1 = new HashMap<>();
        Map<String, Double> result = new HashMap<>();
        for (Map<String, Object> resource : resourceDetailsOrg) {
            if (!resource.get("name").toString().equals("about:blank")) {
                newmap1.put("fetchStart", Double.parseDouble(resource.get("fetchStart").toString()));
                newmap1.put("responseEnd", Double.parseDouble(resource.get("responseEnd").toString()));
                resourceDetails.add((HashMap) newmap1.clone());
            }
        }

        // First, sort by start time, then end time
        //LOGGER.debug("Trimmed Resource List :" + resourceDetails.toString());
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

        //LOGGER.debug("Sorted Resource List :{}", resourceDetails.toString());
        int size = resourceDetails.size();
        // Next, find all resources with the same start time, and reduce
        // them to the largest end time.
        for (int i = 0; i < size; i++) {

            if (i != (size - 1) && Objects.equals(resourceDetails.get(i).get("fetchStart"), resourceDetails.get(i + 1).get("fetchStart"))) {
            } else {
                resourceDetails1.add(resourceDetails.get(i));
            }
        }

        //LOGGER.debug("Reduced Resource List :{}", resourceDetails1.toString());

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
        //LOGGER.debug("Overlap Resource List :{}", resourceDetails.toString());

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
        LOGGER.debug("CXOP - {} - Reduced Resource List :{}", txnName, resourceDetails.toString());

        Double backendTime = 0.0;
        Double fetchStart = 0.0;
        Double responseEnd = 0.0;
        size = resourceDetails1.size();
        for (int i = 0; i < size; i++) {
            backendTime = backendTime + (resourceDetails1.get(i).get("responseEnd") - resourceDetails1.get(i).get("fetchStart"));
            if (i == 0) {
                fetchStart = resourceDetails1.get(i).get("fetchStart");
            }
            if (i == (size - 1)) {
                responseEnd = resourceDetails1.get(i).get("responseEnd");
            }

        }
        LOGGER.debug("CXOP - {} - Backend Time :{}", txnName, backendTime);

        if (Boolean.valueOf(NavType)) {
            result.put("totalTime", responseEnd);
            LOGGER.debug("CXOP - {} - Navigation : {} TotalTime :{}", txnName, NavType, responseEnd);
        } else {
            result.put("totalTime", (responseEnd - fetchStart));
            LOGGER.debug("CXOP - {} - Navigation : {} TotalTime :{}", txnName, NavType, responseEnd);
        }

        result.put("backendTime", backendTime);
        LOGGER.debug("CXOP - {} - Completed  calculateBackendTime with {} Navigation", txnName, NavType);
        return result;
    }

    private static ArrayList<Map<String, Object>> filterResources(ArrayList<Map<String, Object>> resourceDetailsOrg) {
        ArrayList<Map<String, Object>> filteredResources = new ArrayList<>();
        for (Map<String, Object> resource : resourceDetailsOrg) {
            if(validResource(resource.get("name").toString())){
                filteredResources.add(resource);
            }
        }
        return filteredResources;
    }

    private static boolean validResource(String name){
        for (String filter : CollectorConstants.getResourceFilter()) {
            if (name.contains(filter)) {
                return false;
            }
        }
        return true;

    }

    private static Map<String, Object> markListProcessed(ArrayList<Map<String, Object>> markDetails) {

        LOGGER.debug("CXOP - Inside markListProcessed :{}", markDetails.toString());
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
