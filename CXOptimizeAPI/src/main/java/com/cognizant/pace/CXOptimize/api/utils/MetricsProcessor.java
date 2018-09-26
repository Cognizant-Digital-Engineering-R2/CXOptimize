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

package com.cognizant.pace.CXOptimize.api.utils;


import com.cognizant.pace.CXOptimize.AnalysisEngine.GlobalConstants;
import com.cognizant.pace.CXOptimize.AnalysisEngine.PaceAnalysisEngine;
import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class MetricsProcessor {

    @Autowired
    private HTTPUtils httpUtils;

    @Autowired
    private AppUtils appUtils;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MetricsProcessor.class);

    public static double truncate(double value, int places) 
    {
        if (places < 0) {
            throw new IllegalArgumentException();
        }

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = (long) value;
        return (double) tmp / factor;
    }

    public Map<String, String> statsParser(String clientName, String projectName, String scenario, JSONObject json, String esUrl, boolean geoIpFlag, String hostIP, String geoService) throws Exception 
    {
        LOGGER.debug("Entering method statsParser");

        Map<String, String> retMap = new HashMap<>();

        if (json.has("details")) 
        {
            JSONObject details = json.getJSONObject("details");
            Map<String,Object> configDetails = new HashMap<>();
            
            //Source of data collection
            if (details.has("source") && !details.isNull("source"))
            {
                if(details.getString("source").equals("ChromePlugin"))
                {
                    configDetails = PaceAnalysisEngine.isConfigExists(clientName, projectName, scenario, esUrl,true);
                }
                else
                {
                    configDetails = PaceAnalysisEngine.isConfigExists(clientName, projectName, scenario, esUrl,false);
                }
            }

            // Map<String,Object> configDetails = PaceAnalysisEngine.isConfigExists(clientName, projectName, scenario, esUrl,false);
            if (configDetails.get("Status").toString().equals("true"))
            {
                JSONObject parentJSONDocument = new JSONObject();
                DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSSZ");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));



                String RunID = null;
                RunID = details.getString("RunID");
                long msFirstPaint = 0;

                //Build Main Document
                parentJSONDocument.put("ClientName", clientName);
                parentJSONDocument.put("ProjectName", projectName);
                parentJSONDocument.put("Scenario", scenario);

                //Run Details
                if(details.getString("source").equals("ChromePlugin"))
                {
                    parentJSONDocument.put("Release", configDetails.get("Release").toString());
                    parentJSONDocument.put("BuildNumber", configDetails.get("BuildNumber").toString());
                    parentJSONDocument.put("RunID", Long.parseLong(configDetails.get("RunID").toString()));
                    parentJSONDocument.put("RunTime", format.format(new java.util.Date(Long.parseLong(configDetails.get("RunTime").toString()))));

                    if (details.has("ScriptTime")) {
                        parentJSONDocument.put("ScriptTime", details.getLong("ScriptTime"));
                    } else {
                        parentJSONDocument.put("ScriptTime", 0);
                    }
                }
                else
                {
                    parentJSONDocument.put("Release", details.getString("Release"));
                    parentJSONDocument.put("BuildNumber", details.getString("BuildNumber"));
                    parentJSONDocument.put("RunID", Long.parseLong(RunID));
                    if (details.has("RunTime")) {
                        parentJSONDocument.put("RunTime", format.format(new java.util.Date(details.getLong("RunTime"))));
                    } else {
                        parentJSONDocument.put("RunTime", format.format(new java.util.Date(System.currentTimeMillis())));
                    }

                    if (details.has("ScriptTime")) {
                        parentJSONDocument.put("ScriptTime", details.getLong("ScriptTime"));
                    } else {
                        parentJSONDocument.put("ScriptTime", 0);
                    }

                }

                if (details.has("source") && !details.isNull("source"))
                {
                    parentJSONDocument.put("source",details.getString("source"));
                }
                else
                {
                    parentJSONDocument.put("source","unknown");
                }

                //Transaction Details
                parentJSONDocument.put("TransactionName", details.getString("transactionName").trim());
                UrlValidator urlValidator = new UrlValidator();
                parentJSONDocument.put("url", details.getString("url"));
                String parentDomain = "";
                if (urlValidator.isValid(details.getString("url")))
                {
                    parentDomain = (new URL(details.getString("url")).getHost());
                }
                else
                {
                    parentDomain = (details.getString("url").split("//")[1]).split("/")[0];
                }

                //Navigation Details
                String navType = "";
                if (details.has("NavType") && !details.isNull("NavType")) 
                {
                    navType = details.getString("NavType");
                } 
                else 
                {
                    navType = "Hard";
                }

                parentJSONDocument.put("NavType", navType);

                long visuallyComplete = 0;

                if ("hard".equals(navType.toLowerCase()))
                {
                    if(details.getString("source").equals("ChromePlugin"))
                    {
                        if (json.has("resources"))
                        {
                            //JSONArray resource = json.getJSONArray("resources");
                            Map<String, Double> resourceTime = appUtils.calculateBackendTime(json.getJSONArray("resources"),navType);
                            parentJSONDocument.put("resourceLoadTime", resourceTime.get("backendTime"));
                            visuallyComplete = (long) resourceTime.get("totalTime").doubleValue();
                            parentJSONDocument.put("speedIndex", (details.has("speedIndex") ? details.getDouble("speedIndex") : 0));

                        }

                    }
                    else
                    {
                        if (details.has("visuallyComplete"))
                        {
                            visuallyComplete = (long) details.getDouble("visuallyComplete");
                        }
                        parentJSONDocument.put("speedIndex", (details.has("speedIndex") ? details.getDouble("speedIndex") : 0));
                        parentJSONDocument.put("resourceLoadTime", (details.has("resourceLoadTime") ? details.getDouble("resourceLoadTime") : 0));
                    }

                }
                else
                {
                    if(details.getString("source").equals("ChromePlugin"))
                    {
                        if (json.has("resources"))
                        {
                            //JSONArray resource = json.getJSONArray("resources");
                            Map<String, Double> resourceTime = appUtils.calculateBackendTime(json.getJSONArray("resources"),navType);
                            parentJSONDocument.put("resourceLoadTime", resourceTime.get("backendTime"));
                            visuallyComplete = (long) resourceTime.get("totalTime").doubleValue();
                            parentJSONDocument.put("totalPageLoadTime", visuallyComplete);
                           // parentJSONDocument.put("resourceLoadTime", resourceTime.get("backendTime"));
                            parentJSONDocument.put("visuallyComplete", visuallyComplete);

                        }

                    }
                    else
                    {
                        if (details.has("visuallyComplete")) {
                            visuallyComplete = (long) details.getDouble("visuallyComplete");
                        } else {
                            visuallyComplete = (long) details.getDouble("resourceLoadTime");
                        }
                        parentJSONDocument.put("totalPageLoadTime", visuallyComplete);
                        parentJSONDocument.put("resourceLoadTime", details.getDouble("resourceLoadTime"));
                        parentJSONDocument.put("visuallyComplete", visuallyComplete);
                    }
                }




                if (details.has("StartTime") && !details.isNull("StartTime")) {
                    parentJSONDocument.put("StartTime", format.format(new java.util.Date(details.getLong("StartTime"))));
                    parentJSONDocument.put("navigationStart", details.getLong("StartTime"));
                } else {
                    parentJSONDocument.put("StartTime", format.format(new java.util.Date(System.currentTimeMillis())));
                }

                if(details.getString("source").equals("ChromePlugin"))
                {
                    parentJSONDocument.put("HostName", hostIP);
                }
                else
                {
                    if (json.has("host")) {
                        JSONObject host = json.getJSONObject("host");
                        parentJSONDocument.put("HostName", host.getString("name"));
                    }

                }


                JSONObject platform = null;
                if (json.has("platform")) {
                    platform = json.getJSONObject("platform");
                    if (platform.has("UserAgent") && !platform.isNull("UserAgent") && !platform.getString("UserAgent").equals("")) {
                        parentJSONDocument.put("UserAgent", platform.getString("UserAgent"));
                        UserAgentStringParser parser = UADetectorServiceFactory.getOnlineUpdatingParser();
                        ReadableUserAgent agent = parser.parse(platform.getString("UserAgent"));

                        parentJSONDocument.put("BrowserName", agent.getName());
                        parentJSONDocument.put("Platform", agent.getOperatingSystem().getName());
                        parentJSONDocument.put("DeviceType", agent.getDeviceCategory().getCategory().getName());
                        parentJSONDocument.put("BrowserVersion", agent.getVersionNumber().toVersionString());
                        parentJSONDocument.put("ResolvedUA", agent.getDeviceCategory().getCategory().getName() + "|" + agent.getOperatingSystem().getName() + "|" + agent.getName() + "|" + agent.getVersionNumber().toVersionString());
                    }
                }

                //Get Geo Location Data
                if (geoIpFlag && hostIP != null && hostIP != "") {
                    StringBuilder location = null;
                    location = httpUtils.httpGet(geoService + "/json/" + hostIP);
                    if (location.indexOf("NOT_FOUND") < 0) {
                        JSONObject geo = new JSONObject(location.toString());
                        if (geo.getDouble("latitude") != 0 && geo.getDouble("longitude") != 0) {
                            geo.put("lat", geo.getDouble("latitude"));
                            geo.put("lon", geo.getDouble("longitude"));
                            geo.remove("longitude");
                            geo.remove("latitude");
                        } else {
                            geo = new JSONObject("{}");
                        }
                        parentJSONDocument.put("geoIP", geo);
                    }

                }

                if (json.has("others")) 
                {
                    JSONObject others = json.getJSONObject("others");
                    if (others.has("dom")) {
                        if (others.isNull("dom")) {
                            parentJSONDocument.put("dom", "");
                        } else {
                            //String dom = (String) others.get("dom");
                            parentJSONDocument.put("dom", others.getString("dom"));
                        }
                    }
                    if (others.has("domElementCount")) {
                        parentJSONDocument.put("domElementCount", others.getLong("domElementCount"));
                    }
                    if (others.has("msFirstPaint")) {
                        msFirstPaint = others.getLong("msFirstPaint");
                        parentJSONDocument.put("msFirstPaint", msFirstPaint);
                    }

                }

                if (json.has("navtime")) 
                {
                    if (!json.isNull("navtime")) 
                    {
                        JSONObject navtime = json.getJSONObject("navtime");
                        if (navtime.length() > 0) 
                        {
                            parentJSONDocument.put("connectEnd", navtime.getLong("connectEnd"));
                            parentJSONDocument.put("connectStart", navtime.getLong("connectStart"));
                            parentJSONDocument.put("domComplete", navtime.getLong("domComplete"));
                            parentJSONDocument.put("domContentLoadedEventEnd", navtime.getLong("domContentLoadedEventEnd"));
                            parentJSONDocument.put("domContentLoadedEventStart", navtime.getLong("domContentLoadedEventStart"));
                            parentJSONDocument.put("domInteractive", navtime.getLong("domInteractive"));
                            parentJSONDocument.put("domLoading", navtime.getLong("domLoading"));
                            parentJSONDocument.put("domainLookupEnd", navtime.getLong("domainLookupEnd"));
                            parentJSONDocument.put("domainLookupStart", navtime.getLong("domainLookupStart"));
                            parentJSONDocument.put("fetchStart", navtime.getLong("fetchStart"));
                            parentJSONDocument.put("loadEventEnd", navtime.getLong("loadEventEnd"));
                            parentJSONDocument.put("loadEventStart", navtime.getLong("loadEventStart"));
                            parentJSONDocument.put("navigationStart", navtime.getLong("navigationStart"));
                            parentJSONDocument.put("redirectEnd", navtime.getLong("redirectEnd"));
                            parentJSONDocument.put("redirectStart", navtime.getLong("redirectStart"));
                            parentJSONDocument.put("requestStart", navtime.getLong("requestStart"));
                            parentJSONDocument.put("responseEnd", navtime.getLong("responseEnd"));
                            parentJSONDocument.put("responseStart", navtime.getLong("responseStart"));
                            parentJSONDocument.put("secureConnectionStart", navtime.has("secureConnectionStart") ? navtime.getLong("secureConnectionStart") : 0);
                            parentJSONDocument.put("unloadEventEnd", navtime.getLong("unloadEventEnd"));
                            parentJSONDocument.put("unloadEventStart", navtime.getLong("unloadEventStart"));

                            //Modified logic to use data Paint API
                            parentJSONDocument.put("ttfpUser",msFirstPaint);
                            parentJSONDocument.put("ttfpBrowser", msFirstPaint - (navtime.getLong("fetchStart") - navtime.getLong("navigationStart")));

                            /*if (navtime.has("msFirstPaint"))
                            {
                                msFirstPaint = navtime.getLong("msFirstPaint");
                                parentJSONDocument.put("msFirstPaint", msFirstPaint);
                                parentJSONDocument.put("ttfpUser", msFirstPaint - navtime.getLong("navigationStart"));
                                parentJSONDocument.put("ttfpBrowser", msFirstPaint - navtime.getLong("fetchStart"));
                            } 
                            else 
                            {
                                if (msFirstPaint != 0) 
                                {
                                    parentJSONDocument.put("ttfpUser", msFirstPaint - navtime.getLong("navigationStart"));
                                    parentJSONDocument.put("ttfpBrowser", msFirstPaint - navtime.getLong("fetchStart"));
                                } 
                                else {
                                    parentJSONDocument.put("ttfpUser", navtime.getLong("loadEventEnd") - navtime.getLong("navigationStart"));
                                    parentJSONDocument.put("ttfpBrowser", navtime.getLong("loadEventEnd") - navtime.getLong("fetchStart"));
                                }

                            }*/

                            long unloadTime = (navtime.getLong("unloadEventEnd") - navtime.getLong("unloadEventStart")) < 0 ? 0 : (navtime.getLong("unloadEventEnd") - navtime.getLong("unloadEventStart"));
                            parentJSONDocument.put("unloadTime", unloadTime);
                            long redirectTime = (navtime.getLong("redirectEnd") - navtime.getLong("requestStart")) < 0 ? 0 : (navtime.getLong("redirectEnd") - navtime.getLong("requestStart"));
                            parentJSONDocument.put("redirectTime", redirectTime);
                            long cacheFetchTime = (navtime.getLong("domainLookupStart") - navtime.getLong("fetchStart")) < 0 ? 0 : (navtime.getLong("domainLookupStart") - navtime.getLong("fetchStart"));
                            parentJSONDocument.put("cacheFetchTime", cacheFetchTime);
                            long dnsLookupTime = (navtime.getLong("domainLookupEnd") - navtime.getLong("domainLookupStart")) < 0 ? 0 : (navtime.getLong("domainLookupEnd") - navtime.getLong("domainLookupStart"));
                            parentJSONDocument.put("dnsLookupTime", dnsLookupTime);

                            long tcpConnectTime = 0;
                            if ((navtime.has("secureConnectionStart") ? navtime.getLong("secureConnectionStart") : 0) == 0) {
                                tcpConnectTime = ((navtime.getLong("connectEnd") - navtime.getLong("connectStart")) < 0 ? 0 : (navtime.getLong("connectEnd") - navtime.getLong("connectStart")));
                            } else {
                                tcpConnectTime = ((navtime.getLong("connectEnd") - navtime.getLong("secureConnectionStart")) < 0 ? 0 : (navtime.getLong("connectEnd") - navtime.getLong("secureConnectionStart")));
                            }
                            parentJSONDocument.put("tcpConnectTime", tcpConnectTime);

                            long serverTime_ttfb = (navtime.getLong("responseStart") - navtime.getLong("requestStart")) < 0 ? 0 : (navtime.getLong("responseStart") - navtime.getLong("requestStart"));
                            parentJSONDocument.put("serverTime_ttfb", serverTime_ttfb);
                            long downloadTime = (navtime.getLong("responseEnd") - navtime.getLong("responseStart")) < 0 ? 0 : (navtime.getLong("responseEnd") - navtime.getLong("responseStart"));
                            parentJSONDocument.put("downloadTime", downloadTime);
                            long domLoadingTime = (navtime.getLong("domContentLoadedEventEnd") - navtime.getLong("domLoading")) < 0 ? 0 : (navtime.getLong("domContentLoadedEventEnd") - navtime.getLong("domLoading"));
                            parentJSONDocument.put("domLoadingTime", domLoadingTime);
                            long domProcessingTime = (navtime.getLong("domComplete") - navtime.getLong("domLoading")) < 0 ? 0 : (navtime.getLong("domComplete") - navtime.getLong("domLoading"));
                            parentJSONDocument.put("domProcessingTime", domProcessingTime);
                            long onloadTime = (navtime.getLong("loadEventEnd") - navtime.getLong("loadEventStart")) < 0 ? 0 : (navtime.getLong("loadEventEnd") - navtime.getLong("loadEventStart"));
                            parentJSONDocument.put("onloadTime", onloadTime);

                            //Calculated Fields
                            long fetchStartTime = navtime.getLong("fetchStart") - navtime.getLong("navigationStart");
                            parentJSONDocument.put("fetchStartTime", fetchStartTime);
                            //Time to DOM Interative
                            long renderingTime = (navtime.getLong("domInteractive") - navtime.getLong("navigationStart")) < 0 ? 0 : (navtime.getLong("domInteractive") - navtime.getLong("navigationStart"));
                            parentJSONDocument.put("renderingTime", renderingTime);

                            parentJSONDocument.put("ttfbUser", navtime.getLong("responseStart") - navtime.getLong("navigationStart"));
                            parentJSONDocument.put("ttfbBrowser", navtime.getLong("responseStart") - navtime.getLong("fetchStart"));

                            parentJSONDocument.put("serverTime_ttlb", (serverTime_ttfb + downloadTime));
                            parentJSONDocument.put("clientProcessing", navtime.getLong("loadEventEnd") - navtime.getLong("responseEnd"));

                            if ("hard" .equals(navType.toLowerCase()))
                            {
                                long totalLoadTime = 0;
                                if(navtime.getLong("loadEventEnd") > 0)
                                {
                                    totalLoadTime = navtime.getLong("loadEventEnd") - navtime.getLong("navigationStart");
                                }
                                else
                                {
                                    totalLoadTime = navtime.getLong("domComplete") - navtime.getLong("navigationStart");
                                }

                                parentJSONDocument.put("totalPageLoadTime", totalLoadTime);
                                if (visuallyComplete <= 0)
                                {
                                    parentJSONDocument.put("visuallyComplete", totalLoadTime);
                                    parentJSONDocument.put("clientTime", (totalLoadTime - ((long)details.getDouble("resourceLoadTime") + (navtime.getLong("responseEnd") - navtime.getLong("requestStart")))) < 0 ? 0 : (totalLoadTime - ((long)details.getDouble("resourceLoadTime") + (navtime.getLong("responseEnd") - navtime.getLong("requestStart")))));
                                }
                                else
                                {
                                    if(visuallyComplete > totalLoadTime)
                                    {
                                        parentJSONDocument.put("visuallyComplete", visuallyComplete);
                                        parentJSONDocument.put("clientTime", (visuallyComplete - ((long) details.getDouble("resourceLoadTime") + (navtime.getLong("responseEnd") - navtime.getLong("requestStart")))) < 0 ? 0 : (visuallyComplete - ((long) details.getDouble("resourceLoadTime") + (navtime.getLong("responseEnd") - navtime.getLong("requestStart")))));
                                    }
                                    else
                                    {
                                        parentJSONDocument.put("visuallyComplete", totalLoadTime);
                                        parentJSONDocument.put("clientTime", (totalLoadTime - ((long) details.getDouble("resourceLoadTime") + (navtime.getLong("responseEnd") - navtime.getLong("requestStart")))) < 0 ? 0 : (totalLoadTime - ((long) details.getDouble("resourceLoadTime") + (navtime.getLong("responseEnd") - navtime.getLong("requestStart")))));
                                    }
                                }

                            }
                            else
                            {
                                parentJSONDocument.put("clientTime", (visuallyComplete - (long)details.getDouble("resourceLoadTime")) < 0 ? 0 : (visuallyComplete - (long)details.getDouble("resourceLoadTime")));
                            }

                            parentJSONDocument.put("pagenetworkTime", (fetchStartTime + dnsLookupTime + tcpConnectTime + downloadTime));
                            parentJSONDocument.put("pageserverTime", serverTime_ttfb);
                            parentJSONDocument.put("pageDomProcessingTime", ((navtime.getLong("domComplete") - navtime.getLong("domLoading")) < 0 ? 0 : (navtime.getLong("domComplete") - navtime.getLong("domLoading"))));
                            parentJSONDocument.put("pageRenderingTime", ((navtime.getLong("loadEventEnd") - navtime.getLong("domComplete")) < 0 ? 0 : (navtime.getLong("loadEventEnd") - navtime.getLong("domComplete"))));
                            parentJSONDocument.put("waitTime", 0);
                        }
                    }
                }

                if (json.has("Memory")) {
                    JSONObject Memory = json.getJSONObject("Memory");
                    if (Memory.has("jsHeapSizeLimit")) {
                        parentJSONDocument.put("jsHeapSizeLimit", Math.round(Memory.getLong("jsHeapSizeLimit") / (1024 * 1024)));
                    } else {
                        parentJSONDocument.put("jsHeapSizeLimit", 0);
                    }

                    if (Memory.has("totalJSHeapSize")) {
                        parentJSONDocument.put("totalJSHeapSize", Math.round(Memory.getLong("totalJSHeapSize") / (1024 * 1024)));
                    } else {
                        parentJSONDocument.put("totalJSHeapSize", 0);
                    }

                    if (Memory.has("usedJSHeapSize")) {
                        parentJSONDocument.put("usedJSHeapSize", Math.round(Memory.getLong("usedJSHeapSize") / (1024 * 1024)));
                    } else {
                        parentJSONDocument.put("usedJSHeapSize", 0);
                    }

                    if (Memory.has("currentPageUsage")) {
                        parentJSONDocument.put("currentPageUsage", Math.round(Memory.getLong("currentPageUsage") / (1024 * 1024)));
                    } else {
                        parentJSONDocument.put("currentPageUsage", 0);
                    }
                }

                //Processing Resources
                double totalSize = 0;

                JSONArray childrenArray = new JSONArray();

                if (json.has("resources")) {
                    JSONArray resource = json.getJSONArray("resources");

                    int resrcSize = resource.length();
                    if (resrcSize > 0)
                    {
                        parentJSONDocument.put("resourceCount", resrcSize);
                        String staticExtension = details.getString("staticResourceExtension");
                        String imageExtension = details.getString("imageResourceExtension");
                        List<String> imgArray = Arrays.asList(imageExtension.split(","));
                        List<String> staticArray = Arrays.asList(staticExtension.split(","));
                        double resourceDurationThreshold = 0;
                        if (details.has("resourceDurationThreshold")) {
                            resourceDurationThreshold = details.getDouble("resourceDurationThreshold");
                        }


                        JSONObject res = new JSONObject();
                        boolean staticResrcStatus = false;
                        boolean isImage = false;
                        boolean flagSet = false;
                        boolean isCached = false;
                        String resourceType = "others";
                        double c_connectEnd = 0;
                        double c_connectStart = 0;
                        double c_domainLookupEnd = 0;
                        double c_domainLookupStart = 0;
                        double c_fetchStart = 0;
                        double c_redirectEnd = 0;
                        double c_redirectStart = 0;
                        double c_requestStart = 0;
                        double c_responseEnd = 0;
                        double c_responseStart = 0;
                        double c_secureConnectionStart = 0;
                        double c_cacheFetchTime = 0;
                        double c_serverTime = 0;
                        double c_duration = 0;
                        double c_clientTime = 0;
                        double c_serverTime_ttfb = 0;
                        double c_tcpConnectTime = 0;
                        double c_downloadTime = 0;
                        double c_serverTime_ttlb = 0;
                        double c_startTime = 0;
                        double c_fetchStartTime = 0;
                        String resrcURL = null;
                        //String tresrcURL = null;
                        double transferSize = 0;
                        String nextHopProtocol = null;
                        double encodedBodySize = 0;
                        double decodedBodySize = 0;
                        String lastModified = null;
                        String expiryDate = null;
                        double c_MinfSize = 0;
                        double c_Height = 0;
                        double c_Width = 0;
                        double c_OrgSize = 0;
                        String c_contentEncoding = null;
                        long c_contentLength = 0;
                        double c_Status = 0;
                        String c_cacheControl = null;
                        String c_Connection = null;
                        String c_ETag = null;
                        String c_entryType = null;
                        String c_initiatorType = null;
                        String c_host = null;
                        double c_workerStart = 0;
                        DecimalFormat df = new DecimalFormat("0.000");

                        //UrlValidator urlValidator = new UrlValidator();
                        for (int j = 0; j < resrcSize; j++) {
                            JSONObject childrenJSONDocument = new JSONObject();
                            res = resource.getJSONObject(j);
                            if(!res.getString("name").equals("about:blank")){
                                c_connectEnd = 0;
                                c_connectStart = 0;
                                c_domainLookupEnd = 0;
                                c_domainLookupStart = 0;
                                c_fetchStart = 0;
                                c_redirectEnd = 0;
                                c_redirectStart = 0;
                                c_requestStart = 0;
                                c_responseEnd = 0;
                                c_responseStart = 0;
                                c_secureConnectionStart = 0;
                                c_cacheFetchTime = 0;
                                c_serverTime = 0;
                                c_duration = 0;
                                c_clientTime = 0;
                                c_serverTime_ttfb = 0;
                                c_tcpConnectTime = 0;
                                c_downloadTime = 0;
                                c_serverTime_ttlb = 0;
                                c_startTime = 0;
                                c_fetchStartTime = 0;
                                resrcURL = "";
                                //tresrcURL = "";
                                transferSize = 0;
                                nextHopProtocol = "";
                                encodedBodySize = 0;
                                decodedBodySize = 0;
                                lastModified = "";
                                expiryDate = "";
                                c_MinfSize = 0;
                                c_Height = 0;
                                c_Width = 0;
                                c_OrgSize = 0;
                                c_contentEncoding = "";
                                c_contentLength = 0;
                                c_Status = 0;
                                c_cacheControl = "";
                                c_Connection = "";
                                c_ETag = "";
                                c_entryType = "";
                                c_initiatorType = "";
                                c_workerStart = 0;
                                staticResrcStatus = false;
                                isImage = false;
                                flagSet = false;
                                isCached = false;
                                resourceType = "others";
                                c_host = "";

                                if (res.has("connectEnd") && !res.isNull("connectEnd")) {
                                    c_connectEnd = res.getDouble("connectEnd");
                                }
                                if (res.has("connectStart") && !res.isNull("connectStart")) {
                                    c_connectStart = res.getDouble("connectStart");
                                }
                                if (res.has("domainLookupEnd") && !res.isNull("domainLookupEnd")) {
                                    c_domainLookupEnd = res.getDouble("domainLookupEnd");
                                }
                                if (res.has("domainLookupStart") && !res.isNull("domainLookupStart")) {
                                    c_domainLookupStart = res.getDouble("domainLookupStart");
                                }
                                if (res.has("fetchStart") && !res.isNull("fetchStart")) {
                                    c_fetchStart = res.getDouble("fetchStart");
                                }
                                if (res.has("redirectEnd") && !res.isNull("redirectEnd")) {
                                    c_redirectEnd = res.getDouble("redirectEnd");
                                }
                                if (res.has("redirectStart") && !res.isNull("redirectStart")) {
                                    c_redirectStart = res.getDouble("redirectStart");
                                }
                                if (res.has("requestStart") && !res.isNull("requestStart")) {
                                    c_requestStart = res.getDouble("requestStart");
                                }
                                if (res.has("startTime") && !res.isNull("startTime")) {
                                    c_startTime = res.getDouble("startTime");
                                }
                                if (res.has("responseEnd") && !res.isNull("responseEnd")) {
                                    c_responseEnd = res.getDouble("responseEnd");
                                }
                                if (res.has("duration") && !res.isNull("duration")) {
                                    c_duration = res.getDouble("duration");
                                }
                                if (res.has("responseStart") && !res.isNull("responseStart")) {
                                    c_responseStart = res.getDouble("responseStart");
                                }
                                if (res.has("secureConnectionStart") && !res.isNull("secureConnectionStart")) {
                                    c_secureConnectionStart = res.getDouble("secureConnectionStart");
                                }
                                if (res.has("transferSize") && !res.isNull("transferSize")) {
                                    transferSize = res.getDouble("transferSize");
                                }
                                if (res.has("encodedBodySize") && !res.isNull("encodedBodySize")) {
                                    encodedBodySize = res.getDouble("encodedBodySize");
                                }
                                if (res.has("decodedBodySize") && !res.isNull("decodedBodySize")) {
                                    decodedBodySize = res.getDouble("decodedBodySize");
                                }
                                if (res.has("nextHopProtocol") && !res.isNull("nextHopProtocol")) {
                                    nextHopProtocol = res.getString("nextHopProtocol");
                                }
                                if (res.has("OrgSize") && !res.isNull("OrgSize")) {
                                    c_OrgSize = res.getDouble("OrgSize");
                                }
                                if (res.has("MinfSize") && !res.isNull("MinfSize")) {
                                    c_MinfSize = res.getDouble("MinfSize");
                                }
                                if (res.has("Last-Modified") && !res.isNull("Last-Modified")) {
                                    lastModified = res.getString("Last-Modified");
                                }
                                if (res.has("Content-Encoding") && !res.isNull("Content-Encoding")) {
                                    c_contentEncoding = res.getString("Content-Encoding");
                                }
                                if (res.has("Content-Length") && !res.isNull("Content-Length")) {
                                    c_contentLength = res.getLong("Content-Length");
                                }
                                if (res.has("Status") && !res.isNull("Status")) {
                                    c_Status = res.getDouble("Status");
                                }
                                if (res.has("Connection") && !res.isNull("Connection")) {
                                    c_Connection = res.getString("Connection");
                                }
                                if (res.has("Cache-Control") && !res.isNull("Cache-Control")) {
                                    c_cacheControl = res.getString("Cache-Control");
                                }
                                if (res.has("ETag") && !res.isNull("ETag")) {
                                    c_ETag = res.getString("ETag");
                                }
                                if (res.has("Expires") && !res.isNull("Expires")) {
                                    expiryDate = res.getString("Expires");
                                }
                                if (res.has("Height") && !res.isNull("Height")) {
                                    c_Height = res.getDouble("Height");
                                }
                                if (res.has("Width") && !res.isNull("Width")) {
                                    c_Width = res.getDouble("Width");
                                }

                                if (res.has("entryType") && !res.isNull("entryType")) {
                                    c_entryType = res.getString("entryType");
                                }
                                if (res.has("initiatorType") && !res.isNull("initiatorType")) {
                                    c_initiatorType = res.getString("initiatorType");
                                }
                                if (res.has("workerStart") && !res.isNull("workerStart")) {
                                    c_workerStart = res.getDouble("workerStart");
                                }

                                if (res.has("IsCached") && !res.isNull("IsCached")) {
                                    isCached = res.getBoolean("IsCached");
                                } else {
                                    if (c_duration <= resourceDurationThreshold) {
                                        isCached = true;
                                    }
                                }

                                if (c_domainLookupStart > 0 && c_fetchStart > 0) {
                                    c_cacheFetchTime = (c_domainLookupStart - c_fetchStart) < 0 ? 0 : (c_domainLookupStart - c_fetchStart);
                                }

                                if (c_responseStart > 0 && c_requestStart > 0) {
                                    c_serverTime_ttfb = (c_responseStart - c_requestStart) < 0 ? 0 : (c_responseStart - c_requestStart);
                                }

                                if (c_duration > 0 && c_serverTime > 0) {
                                    c_clientTime = (c_duration - c_serverTime_ttfb);
                                }
                                if (c_connectEnd > 0 && c_connectStart > 0) {
                                    if (c_secureConnectionStart > 0) {
                                        c_tcpConnectTime = (c_connectEnd - c_secureConnectionStart);
                                    } else {
                                        c_tcpConnectTime = (c_connectEnd - c_connectStart);
                                    }
                                }

                                if (c_responseEnd > 0 && c_responseStart > 0) {
                                    c_downloadTime = c_responseEnd - c_responseStart;
                                }

                                c_serverTime_ttlb = c_serverTime_ttfb + c_downloadTime;

                                if (c_fetchStart > 0 && c_startTime > 0) {
                                    c_fetchStartTime = (c_fetchStart - c_startTime) < 0 ? 0 : (c_fetchStart - c_startTime);
                                }

                                if (res.has("name") && !res.isNull("name")) {
                                    resrcURL = res.getString("name");
                                    //tresrcURL = resrcURL.toLowerCase().split("\\?")[0];
                                    if (res.has("HostName") && !res.isNull("HostName"))
                                    {
                                        c_host = res.getString("HostName");
                                    }
                                    else
                                    {
                                        c_host = appUtils.getHostName(resrcURL,urlValidator);
                                    }
                                }


                                childrenJSONDocument.put("name", res.getString("name"));
                                childrenJSONDocument.put("initiatorType", c_initiatorType);
                                childrenJSONDocument.put("startTime", Double.parseDouble(df.format(c_startTime)));
                                childrenJSONDocument.put("fetchStart", Double.parseDouble(df.format(c_fetchStart)));
                                childrenJSONDocument.put("redirectStart", Double.parseDouble(df.format(c_redirectStart)));
                                childrenJSONDocument.put("redirectEnd", Double.parseDouble((df.format(c_redirectEnd))));
                                childrenJSONDocument.put("domainLookupStart", Double.parseDouble(df.format(c_domainLookupStart)));
                                childrenJSONDocument.put("domainLookupEnd", Double.parseDouble(df.format(c_domainLookupEnd)));
                                childrenJSONDocument.put("connectStart", Double.parseDouble(df.format(c_connectStart)));
                                childrenJSONDocument.put("secureConnectionStart", Double.parseDouble(df.format(c_secureConnectionStart)));
                                childrenJSONDocument.put("connectEnd", Double.parseDouble(df.format(c_connectEnd)));
                                childrenJSONDocument.put("requestStart", Double.parseDouble(df.format(c_requestStart)));
                                childrenJSONDocument.put("responseStart", Double.parseDouble(df.format(c_responseStart)));
                                childrenJSONDocument.put("responseEnd", Double.parseDouble(df.format(c_responseEnd)));
                                childrenJSONDocument.put("duration", c_duration);

                                childrenJSONDocument.put("entryType", c_entryType);
                                childrenJSONDocument.put("transferSize", Double.parseDouble(df.format(transferSize)));
                                if(platform.getString("UserAgent").contains("Trident"))
                                {
                                    totalSize = totalSize + c_contentLength;
                                }
                                else
                                {
                                    if(transferSize == 0 && encodedBodySize == 0)
                                    {
                                        totalSize = totalSize + c_contentLength;
                                    }
                                    else
                                    {
                                        if(transferSize == 0)
                                        {
                                            totalSize = totalSize + encodedBodySize;
                                        }
                                        else
                                        {
                                            totalSize = totalSize + transferSize;
                                        }
                                    }

                                }

                                childrenJSONDocument.put("decodedBodySize", Double.parseDouble(df.format(decodedBodySize)));
                                childrenJSONDocument.put("encodedBodySize", Double.parseDouble(df.format(encodedBodySize)));
                                childrenJSONDocument.put("nextHopProtocol", nextHopProtocol);
                                childrenJSONDocument.put("workerStart", Double.parseDouble(df.format(c_workerStart)));


                                childrenJSONDocument.put("OrgSize", c_OrgSize);

                                childrenJSONDocument.put("MinfSize", c_MinfSize);
                                childrenJSONDocument.put("Last-Modified", lastModified);
                                childrenJSONDocument.put("Content-Encoding", c_contentEncoding);
                                childrenJSONDocument.put("Content-Length", c_contentLength);
                                childrenJSONDocument.put("Status", c_Status);
                                childrenJSONDocument.put("Connection", c_Connection);
                                childrenJSONDocument.put("Cache-Control", c_cacheControl);
                                childrenJSONDocument.put("ETag", c_ETag);
                                childrenJSONDocument.put("Expires", expiryDate);
                                childrenJSONDocument.put("Height", c_Height);
                                childrenJSONDocument.put("Width", c_Width);


                                childrenJSONDocument.put("cacheFetchTime", Double.parseDouble(df.format(c_cacheFetchTime)));
                                childrenJSONDocument.put("redirectTime", Double.parseDouble(df.format(c_redirectEnd - c_redirectStart)));
                                childrenJSONDocument.put("dnsLookupTime", Double.parseDouble(df.format(c_domainLookupEnd - c_domainLookupStart)));
                                childrenJSONDocument.put("tcpConnectTime", Double.parseDouble(df.format(c_tcpConnectTime)));
                                childrenJSONDocument.put("serverTime_ttfb", Double.parseDouble(df.format(c_serverTime_ttfb)));
                                childrenJSONDocument.put("downloadTime", Double.parseDouble(df.format(c_downloadTime)));
                                childrenJSONDocument.put("fetchStartTime", Double.parseDouble(df.format(c_fetchStartTime)));
                                childrenJSONDocument.put("clientTime", Double.parseDouble(df.format(c_clientTime)));
                                childrenJSONDocument.put("serverTime_ttlb", Double.parseDouble(df.format(c_serverTime_ttlb)));
                                childrenJSONDocument.put("totalResourceTime", Double.parseDouble(df.format(c_duration)));
                                childrenJSONDocument.put("HostName", c_host);
                                childrenJSONDocument.put("IsCached", isCached);

                                //Check if the browser is not IE
                                if(!platform.getString("UserAgent").contains("Trident"))
                                {
                                    //If resource domain is same as parent check for transfersize to set caching
                                    if (c_host.equals(parentDomain))
                                    {
                                        if (encodedBodySize == 0 && transferSize == 0) {
                                            childrenJSONDocument.put("IsCached", true);
                                        }


                                    }
                                    else
                                    {
                                        //If resource domain is cors and the domain allows the timing value then check caching status
                                        if(c_requestStart != 0 && c_requestStart !=0 && encodedBodySize == 0 && transferSize == 0)
                                        {
                                            childrenJSONDocument.put("IsCached", true);
                                        }

                                    }
                                }

                                if (res.has("IsStaticResrc") && res.has("IsImage") && res.has("ResourceType"))
                                {
                                    childrenJSONDocument.put("IsStaticResrc", res.getBoolean("IsStaticResrc"));
                                    childrenJSONDocument.put("IsImage", res.getBoolean("IsImage"));
                                    childrenJSONDocument.put("ResourceType", res.getString("ResourceType"));
                                }
                                else
                                {
                                    for (String img : imgArray) {
                                        if ((res.getString("name").toLowerCase().split("\\?")[0]).contains(img.toLowerCase().trim())) {
                                            isImage = true;
                                            staticResrcStatus = true;
                                            flagSet = true;
                                            resourceType = img.trim();
                                            break;
                                        }
                                    }

                                    if (!flagSet) {
                                        for (String stat : staticArray) {
                                            if ((res.getString("name").toLowerCase().split("\\?")[0]).contains(stat.toLowerCase().trim())) {
                                                staticResrcStatus = true;
                                                isImage = false;
                                                resourceType = stat.trim();
                                                break;
                                            }
                                        }
                                    }

                                    childrenJSONDocument.put("IsStaticResrc", staticResrcStatus);
                                    childrenJSONDocument.put("IsImage", isImage);
                                    childrenJSONDocument.put("ResourceType", resourceType);

                                }



                            /*
                            if (urlValidator.isValid(resrcURL)) {
                                childrenJSONDocument.put("HostName", new URL(resrcURL).getHost());
                            } else {
                                childrenJSONDocument.put("HostName", resrcURL);
                            }

                            if (res.has("HostName") && !res.isNull("HostName")) {
                                childrenJSONDocument.put("HostName", res.getString("HostName"));
                            } else {
                                String trncUrl = res.getString("name").toLowerCase().split("\\?")[0];
                                if (urlValidator.isValid(trncUrl)) {
                                    childrenJSONDocument.put("HostName", new URL(trncUrl).getHost());
                                } else {
                                    childrenJSONDocument.put("HostName", "Unknown");
                                }
                            }*/
                                childrenArray.put(childrenJSONDocument);

                            }


                        }
                    }
                    else
                    {
                        parentJSONDocument.put("resourceCount", 0);
                    }
                }
                else
                {
                    parentJSONDocument.put("resourceCount", 0);
                }

                parentJSONDocument.put("resourceSize", totalSize);



                parentJSONDocument.put("Resources", childrenArray);

                LOGGER.debug("Post message : {}", parentJSONDocument.toString());

                StringBuilder status = null;
                status = httpUtils.httpPost(parentJSONDocument.toString(), GlobalConstants.getESUrl() + "/" + GlobalConstants.STATSINDEX_INSERT);
                if (status.indexOf("NOT_FOUND") < 0) {
                    retMap.put("Status", "Success");
                    retMap.put("StatsIndex", "Success");
                    JSONObject mainPostStatus = new JSONObject(status.toString());
                    if (mainPostStatus.has("result") && mainPostStatus.getString("result").equals("created")) {
                        if (json.has("marktime") && !json.isNull("marktime")) {
                            JSONObject marktime = json.getJSONObject("marktime");
                            JSONObject markJSONDocument = new JSONObject();
                            if (marktime.length() > 0) {
                                markJSONDocument.put("ProjectName", projectName);
                                markJSONDocument.put("ClientName", clientName);
                                markJSONDocument.put("Scenario", scenario);
                                markJSONDocument.put("Release", details.getString("Release"));
                                markJSONDocument.put("BuildNumber", details.getString("BuildNumber"));
                                markJSONDocument.put("RunID", Long.parseLong(RunID));
                                if (details.has("RunTime")) {
                                    markJSONDocument.put("RunTime", format.format(new java.util.Date(details.getLong("RunTime"))));
                                } else {
                                    markJSONDocument.put("RunTime", format.format(new java.util.Date(System.currentTimeMillis())));
                                }
                                markJSONDocument.put("TransactionName", details.getString("transactionName"));
                                markJSONDocument.put("url", details.getString("url"));
                                markJSONDocument.put("parentDocId", mainPostStatus.getString("_id"));
                                if (details.has("StartTime") && !details.isNull("StartTime")) {
                                    markJSONDocument.put("StartTime", format.format(new java.util.Date(details.getLong("StartTime"))));
                                } else {
                                    markJSONDocument.put("StartTime", format.format(new java.util.Date(System.currentTimeMillis())));
                                }
                                Iterator<?> keys = marktime.keys();
                                String key;
                                while (keys.hasNext()) {
                                    key = (String) keys.next();
                                    markJSONDocument.put(key, marktime.getDouble(key));
                                }

                                StringBuilder markStatus = null;
                                markStatus = httpUtils.httpPost(markJSONDocument.toString(), GlobalConstants.getESUrl() + "/" + GlobalConstants.STATSINDEX_INSERT);

                                if (markStatus.indexOf("NOT_FOUND") < 0) {
                                    retMap.put("MarkIndex", "Success");
                                } else {
                                    LOGGER.debug("Failed to insert stats data to datasource {} with {} on {}{}", markJSONDocument, markStatus, esUrl, GlobalConstants.MARKINDEX_INSERT);
                                    retMap.put("MarkIndex", "Failure");
                                }

                            }
                        }


                    }
                } else {
                    LOGGER.debug("Failed to insert stats data to datasource {} with {} on {}{}", parentJSONDocument, status, GlobalConstants.getESUrl(), GlobalConstants.STATSINDEX_INSERT);
                    retMap.put("Status", "Failed");
                    retMap.put("Reason", "Failed to insert data to data source");
                }


            } else {
                LOGGER.debug("Configuration unavailable for {} {} {}", clientName, projectName, scenario);
                retMap.put("Status", "Failed");
                retMap.put("Reason", "Configuration UnAvailable");

            }

        } else {
            LOGGER.debug("Input json doesnt have right format");
            retMap.put("Status", "Failed");
            retMap.put("Reason", "Unrecognized Input data");
        }

        return retMap;
    }

    public Map<String, String> boomerangParser(String clientName, String projectName, String scenario, JSONObject json, String esUrl, boolean geoIpFlag, String hostIP, String geoService) throws Exception
    {
        LOGGER.debug("Entering method boomerangParser");
        Map < String, String > retMap = new HashMap < > ();

        if (json.has("cxoptimize"))
        {
            JSONObject cxoptimize = json.getJSONObject("cxoptimize");
            if (cxoptimize.has("details"))
            {
                JSONObject details = cxoptimize.getJSONObject("details");
                Map<String,Object> configDetails = PaceAnalysisEngine.isConfigExists(clientName, projectName, scenario, esUrl,true);
                if (configDetails.get("Status").toString().equals("true"))
                {
                    JSONObject parentJSONDocument = new JSONObject();
                    DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSSZ");
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                    DecimalFormat df = new DecimalFormat("0.000");
                    String RunID = configDetails.get("RunID").toString();

                    //Build Main Document
                    parentJSONDocument.put("ClientName", clientName);
                    parentJSONDocument.put("ProjectName", projectName);
                    parentJSONDocument.put("Scenario", scenario);

                    //Run Details
                    /*
                    if (details.has("release")) {
                        parentJSONDocument.put("Release", details.getString("release"));
                    } else {
                        parentJSONDocument.put("Release", "");
                    }

                    parentJSONDocument.put("BuildNumber", details.getString("build"));
                    */
                    parentJSONDocument.put("Release",configDetails.get("Release").toString());
                    parentJSONDocument.put("BuildNumber",configDetails.get("BuildNumber").toString());
                    parentJSONDocument.put("RunID", Long.parseLong(RunID));
                    parentJSONDocument.put("RunTime", format.format(new java.util.Date(Long.parseLong(configDetails.get("RunTime").toString()))));

                    if (details.has("ScriptTime")) {
                        parentJSONDocument.put("ScriptTime", details.getLong("ScriptTime"));
                    } else {
                        parentJSONDocument.put("ScriptTime",0);
                    }

                    //Transaction Details
                    parentJSONDocument.put("TransactionName", details.getString("transactionName"));
                    if (json.has("r")) {
                        parentJSONDocument.put("url", json.getString("r"));
                    } else {
                        parentJSONDocument.put("url", "");
                    }

                    //Navigation Details
                    String navType = "";
                    int navigation = 0;
                    if (details.has("navType") && !details.isNull("navType"))
                    {
                        navigation = details.getInt("navType");
                        if (navigation == 0) {
                            navType = "hard";
                        } else {
                            navType = "soft";
                        }

                    }
                    parentJSONDocument.put("NavType", navType);

                    long visuallyComplete = 0;
                    long backendTime = 0;
                    long frontendTime = 0;
                    if (json.has("t"))
                    {
                        JSONObject visComplete = json.getJSONObject("t");
                        visuallyComplete = visComplete.getLong("done");
                        backendTime = visComplete.getLong("resp");
                        frontendTime = visComplete.getLong("page");
                        //parentJSONDocument.put("visuallyComplete", visuallyComplete);
                        //parentJSONDocument.put("resourceLoadTime", visComplete.getLong("resp"));
                        //parentJSONDocument.put("visuallyComplete", visuallyComplete);
                    }
                    else
                    {
                        backendTime = (details.has("resourceLoadTime") ? Long.parseLong(details.getString("resourceLoadTime")) : 0);

                        //visuallyComplete = (details.has("resourceLoadTime") ? details.getLong("resourceLoadTime") : 0);
                        //parentJSONDocument.put("visuallyComplete", visuallyComplete);
                       // parentJSONDocument.put("resourceLoadTime", (details.has("resourceLoadTime") ? details.getDouble("resourceLoadTime") : 0));
                    }


                    if ("hard".equals(navType.toLowerCase()))
                    {
                        parentJSONDocument.put("speedIndex", (details.has("pageIndex") ? details.getDouble("pageIndex") : 0));
                    }
                    //else
                    //{
                        //parentJSONDocument.put("totalPageLoadTime", visuallyComplete);
                        //parentJSONDocument.put("resourceLoadTime", (details.has("resourceLoadTime") ? details.getDouble("resourceLoadTime") : 0));
                        //parentJSONDocument.put("visuallyComplete", visuallyComplete);
                    //}

                    //Source of data collection
                    parentJSONDocument.put("source", "BoomerangJS");
                    parentJSONDocument.put("HostName", hostIP);

                    if (details.has("useragent"))
                    {
                        if (!details.getString("useragent").equals(""))
                        {
                            parentJSONDocument.put("UserAgent", details.getString("useragent"));
                            UserAgentStringParser parser = UADetectorServiceFactory.getOnlineUpdatingParser();
                            ReadableUserAgent agent = parser.parse(details.getString("useragent"));

                            parentJSONDocument.put("BrowserName", agent.getName());
                            parentJSONDocument.put("Platform", agent.getOperatingSystem().getName());
                            parentJSONDocument.put("DeviceType", agent.getDeviceCategory().getCategory().getName());
                            parentJSONDocument.put("BrowserVersion", agent.getVersionNumber().toVersionString());
                            parentJSONDocument.put("ResolvedUA", agent.getDeviceCategory().getCategory().getName() + "|" + agent.getOperatingSystem().getName() + "|" + agent.getName() + "|" + agent.getVersionNumber().toVersionString());
                        }
                    }

                    //Get Geo Location Data
                    if (geoIpFlag && hostIP != null && hostIP != "")
                    {
                        StringBuilder location = null;
                        location = httpUtils.httpGet(geoService + "/json/" + hostIP);
                        if (location.indexOf("NOT_FOUND") < 0)
                        {
                            JSONObject geo = new JSONObject(location.toString());
                            if (geo.getDouble("latitude") != 0 && geo.getDouble("longitude") != 0) {
                                geo.put("lat", geo.getDouble("latitude"));
                                geo.put("lon", geo.getDouble("longitude"));
                                geo.remove("longitude");
                                geo.remove("latitude");
                            } else {
                                geo = new JSONObject("{}");
                            }
                            parentJSONDocument.put("geoIP", geo);
                        }

                    }

                    if (details.has("dom"))
                    {
                        parentJSONDocument.put("dom", details.getString("dom"));
                    }
                    else
                    {
                        parentJSONDocument.put("dom", "");
                    }

                    if (details.has("domCount"))
                    {
                        parentJSONDocument.put("domCount", details.getLong("domCount"));
                    }
                    else
                    {
                        parentJSONDocument.put("domCount", 0);
                    }

                    if (json.has("nt"))
                    {
                        if (!json.isNull("nt"))
                        {
                            JSONObject navtime = json.getJSONObject("nt");
                            if (navtime.length() > 0)
                            {
                                long connectEnd = 0;
                                long connectStart = 0;
                                long domainLookupEnd = 0;
                                long domainLookupStart = 0;
                                long domComplete = 0;
                                long domLoading = 0;
                                long domInteractive = 0;
                                long fetchStart = 0;
                                long loadEventEnd = 0;
                                long loadEventStart = 0;
                                long navigationStart = 0;
                                long redirectEnd = 0;
                                long redirectStart = 0;
                                long redirectCount = 0;
                                long requestStart = 0;
                                long responseEnd = 0;
                                long responseStart = 0;
                                long secureConnectionStart = 0;
                                double msFirstPaint = 0;
                                long unloadEventEnd = 0;
                                long unloadEventStart = 0;
                                long domContentLoadedEventEnd = 0;
                                long domContentLoadedEventStart = 0;

                                if (navtime.has("con") && !navtime.isNull("con"))
                                {
                                    connectEnd = navtime.getJSONObject("con").getLong("end");
                                    connectStart = navtime.getJSONObject("con").getLong("st");
                                }

                                if (navtime.has("fet") && !navtime.isNull("fet"))
                                {
                                    fetchStart = connectStart = navtime.getJSONObject("fet").getLong("st");
                                }

                                if (navtime.has("req") && !navtime.isNull("req")) {
                                    requestStart = connectStart = navtime.getJSONObject("req").getLong("st");

                                }

                                if (navtime.has("res") && !navtime.isNull("res")) {
                                    responseEnd = navtime.getJSONObject("res").getLong("end");
                                    responseStart = navtime.getJSONObject("res").getLong("st");

                                }

                                if (navtime.has("nav") && !navtime.isNull("nav")) {
                                    navigationStart = navtime.getJSONObject("nav").getLong("st");

                                }

                                if (navtime.has("red") && !navtime.isNull("red")) {
                                    redirectEnd = navtime.getJSONObject("red").getLong("end");
                                    redirectStart = navtime.getJSONObject("red").getLong("st");
                                    redirectCount = navtime.getJSONObject("red").getLong("cnt");

                                }

                                if (navtime.has("dns") && !navtime.isNull("dns")) {
                                    domainLookupEnd = navtime.getJSONObject("dns").getLong("end");
                                    domainLookupStart = navtime.getJSONObject("res").getLong("st");

                                }

                                if (navtime.has("domloading")) {
                                    domLoading = navtime.getLong("domloading");
                                }

                                if (navtime.has("domint")) {
                                    domInteractive = navtime.getLong("domint");
                                }
                                if (navtime.has("domcomp")) {
                                    domComplete = navtime.getLong("domcomp");
                                }

                                if (navtime.has("domcontloaded") && !navtime.isNull("domcontloaded")) {
                                    domContentLoadedEventEnd = navtime.getJSONObject("domcontloaded").getLong("end");
                                    domContentLoadedEventStart = navtime.getJSONObject("domcontloaded").getLong("st");

                                }

                                if (navtime.has("load") && !navtime.isNull("load")) {
                                    loadEventEnd = navtime.getJSONObject("load").getLong("end");
                                    loadEventStart = navtime.getJSONObject("load").getLong("st");

                                }

                                if (navtime.has("unload") && !navtime.isNull("unload")) {
                                    unloadEventEnd = navtime.getJSONObject("unload").getLong("end");
                                    unloadEventStart = navtime.getJSONObject("unload").getLong("st");

                                }

                                if (navtime.has("secureConnectionStart")) {
                                    secureConnectionStart = navtime.getLong("secureConnectionStart");
                                }
                                if (navtime.has("first")) {
                                    msFirstPaint = navtime.getJSONObject("first").getDouble("paint") * 1000;
                                }
                                else
                                {
                                    msFirstPaint = 0;
                                }

                                //double totalPageLoadTime = (loadEventEnd - navigationStart);


                                parentJSONDocument.put("connectEnd", connectEnd);
                                parentJSONDocument.put("connectStart", connectStart);
                                parentJSONDocument.put("domComplete", domComplete);
                                parentJSONDocument.put("domContentLoadedEventEnd", domContentLoadedEventEnd);
                                parentJSONDocument.put("domContentLoadedEventStart", domContentLoadedEventStart);
                                parentJSONDocument.put("domInteractive", domInteractive);
                                parentJSONDocument.put("domLoading", domLoading);
                                parentJSONDocument.put("domainLookupEnd", domainLookupEnd);
                                parentJSONDocument.put("domainLookupStart", domainLookupStart);
                                parentJSONDocument.put("fetchStart", Double.parseDouble(df.format(fetchStart)));
                                parentJSONDocument.put("loadEventEnd", loadEventEnd);
                                parentJSONDocument.put("loadEventStart", loadEventStart);
                                parentJSONDocument.put("navigationStart", navigationStart);
                                parentJSONDocument.put("redirectEnd", redirectEnd);
                                parentJSONDocument.put("redirectStart", redirectStart);
                                parentJSONDocument.put("requestStart", requestStart);
                                parentJSONDocument.put("responseEnd", responseEnd);
                                parentJSONDocument.put("responseStart", responseStart);
                                parentJSONDocument.put("secureConnectionStart", secureConnectionStart);
                                parentJSONDocument.put("msFirstPaint", msFirstPaint);
                                parentJSONDocument.put("unloadEventEnd", unloadEventEnd);
                                parentJSONDocument.put("unloadEventStart", unloadEventStart);

                                parentJSONDocument.put("StartTime", format.format(new java.util.Date(navigationStart)));

                                if (msFirstPaint != 0) {
                                    parentJSONDocument.put("ttfpUser", msFirstPaint - navigationStart);
                                    parentJSONDocument.put("ttfpBrowser", msFirstPaint - fetchStart);
                                } else {
                                    parentJSONDocument.put("ttfpUser",loadEventEnd - navigationStart);
                                    parentJSONDocument.put("ttfpBrowser", loadEventEnd - fetchStart);
                                }


                                double unloadTime = (unloadEventEnd - unloadEventStart) < 0 ? 0 : (unloadEventEnd - unloadEventStart);
                                parentJSONDocument.put("unloadTime", Double.parseDouble(df.format(unloadTime)));
                                double redirectTime = (redirectEnd - redirectStart) < 0 ? 0 : (redirectEnd - redirectStart);
                                parentJSONDocument.put("redirectTime", Double.parseDouble(df.format(redirectTime)));
                                double cacheFetchTime = (domainLookupStart - fetchStart) < 0 ? 0 : (domainLookupStart - fetchStart);
                                parentJSONDocument.put("cacheFetchTime", Double.parseDouble(df.format(cacheFetchTime)));
                                double dnsLookupTime = (domainLookupEnd - domainLookupStart) < 0 ? 0 : (domainLookupEnd - domainLookupStart);
                                parentJSONDocument.put("dnsLookupTime", Double.parseDouble(df.format(dnsLookupTime)));
                                double tcpConnectTime = (connectEnd - (secureConnectionStart == 0 ? connectStart : secureConnectionStart)) < 0 ? 0 : (connectEnd - (secureConnectionStart == 0 ? connectStart : secureConnectionStart));
                                parentJSONDocument.put("tcpConnectTime", Double.parseDouble(df.format(tcpConnectTime)));
                                double serverTime_ttfb = (responseStart - requestStart) < 0 ? 0 : (responseStart - requestStart);
                                parentJSONDocument.put("serverTime_ttfb", Double.parseDouble(df.format(serverTime_ttfb)));
                                double downloadTime = (responseEnd - responseStart) < 0 ? 0 : (responseEnd - responseStart);
                                parentJSONDocument.put("downloadTime", Double.parseDouble(df.format(downloadTime)));
                                double domLoadingTime = (domContentLoadedEventEnd - domLoading) < 0 ? 0 : (domContentLoadedEventEnd - domLoading);
                                parentJSONDocument.put("domLoadingTime", Double.parseDouble(df.format(domLoadingTime)));
                                double domProcessingTime = (domComplete - domLoading) < 0 ? 0 : (domComplete - domLoading);
                                parentJSONDocument.put("domProcessingTime", Double.parseDouble(df.format(domProcessingTime)));
                                double onloadTime = (loadEventEnd - loadEventStart) < 0 ? 0 : (loadEventEnd - loadEventStart);
                                parentJSONDocument.put("onloadTime", Double.parseDouble(df.format(onloadTime)));


                                //Calculated Fields
                                double fetchStartTime = fetchStart - navigationStart;
                                parentJSONDocument.put("fetchStartTime", fetchStartTime);

                                //Time to DOM Interactive
                                double renderingTime = (domInteractive - navigationStart) < 0 ? 0 : (domInteractive - navigationStart);
                                parentJSONDocument.put("renderingTime", renderingTime);

                                parentJSONDocument.put("ttfbUser",responseStart - navigationStart);
                                parentJSONDocument.put("ttfbBrowser",responseStart - fetchStart);

                                parentJSONDocument.put("serverTime_ttlb", (serverTime_ttfb + downloadTime));
                                parentJSONDocument.put("clientProcessing", loadEventEnd - responseEnd);

                                if ("hard" .equals(navType.toLowerCase()))
                                {
                                    parentJSONDocument.put("totalPageLoadTime", loadEventEnd - navigationStart);
                                    if (visuallyComplete <= 0)
                                    {
                                        parentJSONDocument.put("visuallyComplete", loadEventEnd - navigationStart);
                                        parentJSONDocument.put("clientTime",((loadEventEnd - navigationStart) - (backendTime + (responseStart - requestStart))) < 0 ? 0 : ((loadEventEnd - navigationStart) - (backendTime + (responseStart - requestStart))));
                                        parentJSONDocument.put("resourceLoadTime",backendTime);
                                    }
                                    else
                                    {
                                        parentJSONDocument.put("visuallyComplete", visuallyComplete);
                                        parentJSONDocument.put("clientTime",frontendTime);
                                        parentJSONDocument.put("resourceLoadTime",backendTime);
                                    }

                                }
                                else
                                {
                                    if (visuallyComplete > 0) {
                                        parentJSONDocument.put("totalPageLoadTime", visuallyComplete);
                                        parentJSONDocument.put("visuallyComplete", visuallyComplete);
                                        parentJSONDocument.put("clientTime", frontendTime);
                                        parentJSONDocument.put("resourceLoadTime", backendTime);
                                    }
                                    else
                                    {
                                        parentJSONDocument.put("totalPageLoadTime", backendTime);
                                        parentJSONDocument.put("visuallyComplete", backendTime);
                                        parentJSONDocument.put("clientTime", 0);
                                        parentJSONDocument.put("resourceLoadTime", backendTime);

                                    }
                                }

                                parentJSONDocument.put("pagenetworkTime", (fetchStartTime + dnsLookupTime + tcpConnectTime + downloadTime));
                                parentJSONDocument.put("pageserverTime", serverTime_ttfb);
                                parentJSONDocument.put("pageDomProcessingTime", (domComplete - domLoading) < 0 ? 0 : (domComplete - domLoading));
                                parentJSONDocument.put("pageRenderingTime", (loadEventEnd - domComplete) < 0 ? 0 : (loadEventEnd - domComplete));
                                parentJSONDocument.put("waitTime", 0);

                            }

                        }
                    }

                    if (json.has("mem")) {
                        JSONObject memory = json.getJSONObject("mem");
                        if (memory.has("limit")) {
                            parentJSONDocument.put("jsHeapSizeLimit", Math.round(memory.getLong("limit") / (1024 * 1024)));
                        } else {
                            parentJSONDocument.put("jsHeapSizeLimit", 0);
                        }

                        if (memory.has("total")) {
                            parentJSONDocument.put("totalJSHeapSize", Math.round(memory.getLong("total") / (1024 * 1024)));
                        } else {
                            parentJSONDocument.put("totalJSHeapSize", 0);
                        }

                        if (memory.has("used")) {
                            parentJSONDocument.put("usedJSHeapSize", Math.round(memory.getLong("used") / (1024 * 1024)));
                        } else {
                            parentJSONDocument.put("usedJSHeapSize", 0);
                        }

                        if (memory.has("currentPageUsage")) {
                            parentJSONDocument.put("currentPageUsage", Math.round(memory.getLong("currentPageUsage") / (1024 * 1024)));
                        } else {
                            parentJSONDocument.put("currentPageUsage", 0);
                        }
                    }

                    //Processing Resources

                    JSONArray childrenArray = new JSONArray();
                    double totalSize = 0;

                    if (cxoptimize.has("resourcetiming") || cxoptimize.has("xhr"))
                    {

                        JSONArray resource = null;
                        if (cxoptimize.has("resourcetiming")) {
                            resource = cxoptimize.getJSONArray("resourcetiming");
                        }
                        if (cxoptimize.has("xhr")) {
                            if (cxoptimize.getJSONObject("xhr").has("resources")) {
                                resource = cxoptimize.getJSONObject("xhr").getJSONArray("resources");
                            }
                        }


                        int resrcSize = resource.length();
                        if (resrcSize > 0)
                        {
                            parentJSONDocument.put("resourceCount", resrcSize);
                            String staticExtension = configDetails.get("staticResourceExtension").toString();
                            String imageExtension = configDetails.get("imageResourceExtension").toString();
                            List<String> imgArray = Arrays.asList(imageExtension.split(","));
                            List<String> staticArray = Arrays.asList(staticExtension.split(","));
                            double resourceDurationThreshold = Double.parseDouble(configDetails.get("resourceDurationThreshold").toString());

                            JSONObject res = new JSONObject();
                            boolean staticResrcStatus = false;
                            boolean isImage = false;
                            boolean flagSet = false;
                            boolean isCached = false;
                            String resourceType = "others";
                            double c_connectEnd = 0;
                            double c_connectStart = 0;
                            double c_domainLookupEnd = 0;
                            double c_domainLookupStart = 0;
                            double c_fetchStart = 0;
                            double c_redirectEnd = 0;
                            double c_redirectStart = 0;
                            double c_requestStart = 0;
                            double c_responseEnd = 0;
                            double c_responseStart = 0;
                            double c_secureConnectionStart = 0;
                            double c_cacheFetchTime = 0;
                            double c_serverTime = 0;
                            double c_duration = 0;
                            double c_clientTime = 0;
                            double c_serverTime_ttfb = 0;
                            double c_tcpConnectTime = 0;
                            double c_downloadTime = 0;
                            double c_serverTime_ttlb = 0;
                            double c_startTime = 0;
                            double c_fetchStartTime = 0;
                            String resrcURL = null;
                            String tresrcURL = null;
                            double transferSize = 0;
                            String nextHopProtocol = null;
                            double encodedBodySize = 0;
                            double decodedBodySize = 0;
                            String lastModified = null;
                            String expiryDate = null;
                            double c_MinfSize = 0;
                            double c_Height = 0;
                            double c_Width = 0;
                            double c_OrgSize = 0;
                            String c_contentEncoding = null;
                            long c_contentLength = 0;
                            double c_Status = 0;
                            String c_cacheControl = null;
                            String c_Connection = null;
                            String c_ETag = null;
                            String c_entryType = null;
                            String c_initiatorType = null;
                            double c_workerStart = 0;

                            UrlValidator urlValidator = new UrlValidator();
                            for (int j = 0; j < resrcSize; j++) {
                                JSONObject childrenJSONDocument = new JSONObject();
                                res = resource.getJSONObject(j);
                                c_connectEnd = 0;
                                c_connectStart = 0;
                                c_domainLookupEnd = 0;
                                c_domainLookupStart = 0;
                                c_fetchStart = 0;
                                c_redirectEnd = 0;
                                c_redirectStart = 0;
                                c_requestStart = 0;
                                c_responseEnd = 0;
                                c_responseStart = 0;
                                c_secureConnectionStart = 0;
                                c_cacheFetchTime = 0;
                                c_serverTime = 0;
                                c_duration = 0;
                                c_clientTime = 0;
                                c_serverTime_ttfb = 0;
                                c_tcpConnectTime = 0;
                                c_downloadTime = 0;
                                c_serverTime_ttlb = 0;
                                c_startTime = 0;
                                c_fetchStartTime = 0;
                                resrcURL = "";
                                tresrcURL = "";
                                transferSize = 0;
                                nextHopProtocol = "";
                                encodedBodySize = 0;
                                decodedBodySize = 0;
                                lastModified = "";
                                expiryDate = "";
                                c_MinfSize = 0;
                                c_Height = 0;
                                c_Width = 0;
                                c_OrgSize = 0;
                                c_contentEncoding = "";
                                c_contentLength = 0;
                                c_Status = 0;
                                c_cacheControl = "";
                                c_Connection = "";
                                c_ETag = "";
                                c_entryType = "";
                                c_initiatorType = "";
                                c_workerStart = 0;
                                staticResrcStatus = false;
                                isImage = false;
                                flagSet = false;
                                isCached = false;
                                resourceType = "others";

                                if (res.has("connectEnd") && !res.isNull("connectEnd")) {
                                    c_connectEnd = res.getDouble("connectEnd");
                                }
                                if (res.has("connectStart") && !res.isNull("connectStart")) {
                                    c_connectStart = res.getDouble("connectStart");
                                }
                                if (res.has("domainLookupEnd") && !res.isNull("domainLookupEnd")) {
                                    c_domainLookupEnd = res.getDouble("domainLookupEnd");
                                }
                                if (res.has("domainLookupStart") && !res.isNull("domainLookupStart")) {
                                    c_domainLookupStart = res.getDouble("domainLookupStart");
                                }
                                if (res.has("fetchStart") && !res.isNull("fetchStart")) {
                                    c_fetchStart = res.getDouble("fetchStart");
                                }
                                if (res.has("redirectEnd") && !res.isNull("redirectEnd")) {
                                    c_redirectEnd = res.getDouble("redirectEnd");
                                }
                                if (res.has("redirectStart") && !res.isNull("redirectStart")) {
                                    c_redirectStart = res.getDouble("redirectStart");
                                }
                                if (res.has("requestStart") && !res.isNull("requestStart")) {
                                    c_requestStart = res.getDouble("requestStart");
                                }
                                if (res.has("startTime") && !res.isNull("startTime")) {
                                    c_startTime = res.getDouble("startTime");
                                }
                                if (res.has("responseEnd") && !res.isNull("responseEnd")) {
                                    c_responseEnd = res.getDouble("responseEnd");
                                }
                                if (res.has("duration") && !res.isNull("duration")) {
                                    c_duration = res.getDouble("duration");
                                }
                                if (res.has("responseStart") && !res.isNull("responseStart")) {
                                    c_responseStart = res.getDouble("responseStart");
                                }
                                if (res.has("secureConnectionStart") && !res.isNull("secureConnectionStart")) {
                                    c_secureConnectionStart = res.getDouble("secureConnectionStart");
                                }
                                if (res.has("transferSize") && !res.isNull("transferSize")) {
                                    transferSize = res.getDouble("transferSize");
                                }
                                if (res.has("encodedBodySize") && !res.isNull("encodedBodySize")) {
                                    encodedBodySize = res.getDouble("encodedBodySize");
                                }
                                if (res.has("decodedBodySize") && !res.isNull("decodedBodySize")) {
                                    decodedBodySize = res.getDouble("decodedBodySize");
                                }
                                if (res.has("nextHopProtocol") && !res.isNull("nextHopProtocol")) {
                                    nextHopProtocol = res.getString("nextHopProtocol");
                                }
                                if (res.has("OrgSize") && !res.isNull("OrgSize")) {
                                    c_OrgSize = res.getDouble("OrgSize");
                                }
                                if (res.has("MinfSize") && !res.isNull("MinfSize")) {
                                    c_MinfSize = res.getDouble("MinfSize");
                                }
                                if (res.has("Last-Modified") && !res.isNull("Last-Modified")) {
                                    lastModified = res.getString("Last-Modified");
                                }
                                if (res.has("Content-Encoding") && !res.isNull("Content-Encoding")) {
                                    c_contentEncoding = res.getString("Content-Encoding");
                                }
                                if (res.has("Content-Length") && !res.isNull("Content-Length")) {
                                    c_contentLength = res.getLong("Content-Length");
                                }
                                if (res.has("Status") && !res.isNull("Status")) {
                                    c_Status = res.getDouble("Status");
                                }
                                if (res.has("Connection") && !res.isNull("Connection")) {
                                    c_Connection = res.getString("Connection");
                                }
                                if (res.has("Cache-Control") && !res.isNull("Cache-Control")) {
                                    c_cacheControl = res.getString("Cache-Control");
                                }
                                if (res.has("ETag") && !res.isNull("ETag")) {
                                    c_ETag = res.getString("ETag");
                                }
                                if (res.has("Expires") && !res.isNull("Expires")) {
                                    expiryDate = res.getString("Expires");
                                }
                                if (res.has("Height") && !res.isNull("Height")) {
                                    c_Height = res.getDouble("Height");
                                }
                                if (res.has("Width") && !res.isNull("Width")) {
                                    c_Width = res.getDouble("Width");
                                }

                                if (res.has("entryType") && !res.isNull("entryType")) {
                                    c_entryType = res.getString("entryType");
                                }
                                if (res.has("initiatorType") && !res.isNull("initiatorType")) {
                                    c_initiatorType = res.getString("initiatorType");
                                }
                                if (res.has("workerStart") && !res.isNull("workerStart")) {
                                    c_workerStart = res.getDouble("workerStart");
                                }

                                if (res.has("IsCached") && !res.isNull("IsCached")) {
                                    isCached = res.getBoolean("IsCached");
                                } else {
                                    if (c_duration <= resourceDurationThreshold) {
                                        isCached = true;
                                    }
                                }

                                if (c_domainLookupStart > 0 && c_fetchStart > 0) {
                                    c_cacheFetchTime = (c_domainLookupStart - c_fetchStart) < 0 ? 0 : (c_domainLookupStart - c_fetchStart);
                                }

                                if (c_responseStart > 0 && c_requestStart > 0) {
                                    c_serverTime_ttfb = (c_responseStart - c_requestStart) < 0 ? 0 : (c_responseStart - c_requestStart);
                                }

                                if (c_duration > 0 && c_serverTime > 0) {
                                    c_clientTime = (c_duration - c_serverTime_ttfb);
                                }
                                if (c_connectEnd > 0 && c_connectStart > 0) {
                                    if (c_secureConnectionStart > 0) {
                                        c_tcpConnectTime = (c_connectEnd - c_secureConnectionStart);
                                    } else {
                                        c_tcpConnectTime = (c_connectEnd - c_connectStart);
                                    }
                                }

                                if (c_responseEnd > 0 && c_responseStart > 0) {
                                    c_downloadTime = c_responseEnd - c_responseStart;
                                }

                                c_serverTime_ttlb = c_serverTime_ttfb + c_downloadTime;

                                if (c_fetchStart > 0 && c_startTime > 0) {
                                    c_fetchStartTime = (c_fetchStart - c_startTime) < 0 ? 0 : (c_fetchStart - c_startTime);
                                }

                                if (res.has("name") && !res.isNull("name")) {
                                    resrcURL = res.getString("name");
                                    tresrcURL = resrcURL.toLowerCase().split("\\?")[0];
                                }

                                childrenJSONDocument.put("name", res.getString("name"));
                                childrenJSONDocument.put("initiatorType", c_initiatorType);
                                childrenJSONDocument.put("startTime", Double.parseDouble(df.format(c_startTime)));
                                childrenJSONDocument.put("fetchStart", Double.parseDouble(df.format(c_fetchStart)));
                                childrenJSONDocument.put("redirectStart", Double.parseDouble(df.format(c_redirectStart)));
                                childrenJSONDocument.put("redirectEnd", Double.parseDouble((df.format(c_redirectEnd))));
                                childrenJSONDocument.put("domainLookupStart", Double.parseDouble(df.format(c_domainLookupStart)));
                                childrenJSONDocument.put("domainLookupEnd", Double.parseDouble(df.format(c_domainLookupEnd)));
                                childrenJSONDocument.put("connectStart", Double.parseDouble(df.format(c_connectStart)));
                                childrenJSONDocument.put("secureConnectionStart", Double.parseDouble(df.format(c_secureConnectionStart)));
                                childrenJSONDocument.put("connectEnd", Double.parseDouble(df.format(c_connectEnd)));
                                childrenJSONDocument.put("requestStart", Double.parseDouble(df.format(c_requestStart)));
                                childrenJSONDocument.put("responseStart", Double.parseDouble(df.format(c_responseStart)));
                                childrenJSONDocument.put("responseEnd", Double.parseDouble(df.format(c_responseEnd)));
                                childrenJSONDocument.put("duration", c_duration);

                                childrenJSONDocument.put("entryType", c_entryType);
                                childrenJSONDocument.put("transferSize", Double.parseDouble(df.format(transferSize)));
                                if(details.getString("useragent").contains("Trident"))
                                {
                                    totalSize = totalSize + c_contentLength;
                                }
                                else
                                {
                                    totalSize = totalSize + transferSize;
                                }
                                childrenJSONDocument.put("decodedBodySize", Double.parseDouble(df.format(decodedBodySize)));
                                childrenJSONDocument.put("encodedBodySize", Double.parseDouble(df.format(encodedBodySize)));
                                childrenJSONDocument.put("nextHopProtocol", nextHopProtocol);
                                childrenJSONDocument.put("workerStart", Double.parseDouble(df.format(c_workerStart)));


                                childrenJSONDocument.put("OrgSize", c_OrgSize);
                                childrenJSONDocument.put("MinfSize", c_MinfSize);
                                childrenJSONDocument.put("Last-Modified", lastModified);
                                childrenJSONDocument.put("Content-Encoding", c_contentEncoding);
                                childrenJSONDocument.put("Content-Length", c_contentLength);
                                childrenJSONDocument.put("Status", c_Status);
                                childrenJSONDocument.put("Connection", c_Connection);
                                childrenJSONDocument.put("Cache-Control", c_cacheControl);
                                childrenJSONDocument.put("ETag", c_ETag);
                                childrenJSONDocument.put("Expires", expiryDate);
                                childrenJSONDocument.put("Height", c_Height);
                                childrenJSONDocument.put("Width", c_Width);


                                childrenJSONDocument.put("cacheFetchTime", Double.parseDouble(df.format(c_cacheFetchTime)));
                                childrenJSONDocument.put("redirectTime", Double.parseDouble(df.format(c_redirectEnd - c_redirectStart)));
                                childrenJSONDocument.put("dnsLookupTime", Double.parseDouble(df.format(c_domainLookupEnd - c_domainLookupStart)));
                                childrenJSONDocument.put("tcpConnectTime", Double.parseDouble(df.format(c_tcpConnectTime)));
                                childrenJSONDocument.put("serverTime_ttfb", Double.parseDouble(df.format(c_serverTime_ttfb)));
                                childrenJSONDocument.put("downloadTime", Double.parseDouble(df.format(c_downloadTime)));
                                childrenJSONDocument.put("fetchStartTime", Double.parseDouble(df.format(c_fetchStartTime)));
                                childrenJSONDocument.put("clientTime", Double.parseDouble(df.format(c_clientTime)));
                                childrenJSONDocument.put("serverTime_ttlb", Double.parseDouble(df.format(c_serverTime_ttlb)));
                                childrenJSONDocument.put("totalResourceTime", Double.parseDouble(df.format(c_duration)));

                                childrenJSONDocument.put("IsCached", isCached);

                                if (res.has("IsStaticResrc") && res.has("IsImage") && res.has("ResourceType")) {
                                    childrenJSONDocument.put("IsStaticResrc", res.getBoolean("IsStaticResrc"));
                                    childrenJSONDocument.put("IsImage", res.getBoolean("IsImage"));
                                    childrenJSONDocument.put("ResourceType", res.getString("ResourceType"));
                                } else {
                                    for (String img : imgArray) {
                                        if ((res.getString("name").toLowerCase().split("\\?")[0]).contains(img.toLowerCase().trim())) {
                                            isImage = true;
                                            staticResrcStatus = true;
                                            flagSet = true;
                                            resourceType = img.trim();
                                            break;
                                        }
                                    }

                                    if (!flagSet) {
                                        for (String stat : staticArray) {
                                            if ((res.getString("name").toLowerCase().split("\\?")[0]).contains(stat.toLowerCase().trim())) {
                                                staticResrcStatus = true;
                                                isImage = false;
                                                resourceType = stat.trim();
                                                break;
                                            }
                                        }
                                    }

                                    childrenJSONDocument.put("IsStaticResrc", staticResrcStatus);
                                    childrenJSONDocument.put("IsImage", isImage);
                                    childrenJSONDocument.put("ResourceType", resourceType);

                                }


                                if (urlValidator.isValid(resrcURL)) {
                                    childrenJSONDocument.put("HostName", new URL(resrcURL).getHost());
                                } else {
                                    childrenJSONDocument.put("HostName", resrcURL);
                                }

                                if (res.has("HostName") && !res.isNull("HostName")) {
                                    childrenJSONDocument.put("HostName", res.getString("HostName"));
                                } else {
                                    String trncUrl = res.getString("name").toLowerCase().split("\\?")[0];
                                    if (urlValidator.isValid(trncUrl)) {
                                        childrenJSONDocument.put("HostName", new URL(trncUrl).getHost());
                                    } else {
                                        childrenJSONDocument.put("HostName", "Unknown");
                                    }
                                }
                                childrenArray.put(childrenJSONDocument);

                            }
                        }
                        else
                        {
                            parentJSONDocument.put("resourceCount", 0);
                        }
                    }
                    else
                    {
                        parentJSONDocument.put("resourceCount", 0);
                    }
                    parentJSONDocument.put("resourceSize", totalSize);

                    parentJSONDocument.put("Resources", childrenArray);
                    if (navigation == 2) {
                        LOGGER.debug("Ajax Request");
                        LOGGER.debug("Post message : {}", parentJSONDocument.toString());
                    }
                    StringBuilder status = null;
                    status = httpUtils.httpPost(parentJSONDocument.toString(), GlobalConstants.getESUrl() + "/" + GlobalConstants.STATSINDEX_INSERT);
                    if (status.indexOf("NOT_FOUND") < 0) {
                        retMap.put("Status", "Success");
                        retMap.put("StatsIndex", "Success");
                    } else {
                        LOGGER.debug("Failed to insert stats data to datasource {} with {} on {}{}", parentJSONDocument, status, GlobalConstants.getESUrl(), GlobalConstants.STATSINDEX_INSERT);
                        retMap.put("Status", "Failed");
                        retMap.put("Reason", "Failed to insert data to data source");
                    }

                } else {
                    LOGGER.debug("Configuration unavailable for {} {} {}", clientName, projectName, scenario);
                    retMap.put("Status", "Failed");
                    retMap.put("Reason", "Configuration UnAvailable");
                }

            } else {
                LOGGER.debug("Input json doesnt have right format");
                retMap.put("Status", "Failed");
                retMap.put("Reason", "Unrecognized Input data");
            }


        } else

        {
            LOGGER.debug("Input json doesnt have right format");
            retMap.put("Status", "Failed");
            retMap.put("Reason", "Unrecognized Input data");
        }

        return retMap;

    }


}
