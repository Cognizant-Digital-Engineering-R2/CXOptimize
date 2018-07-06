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

package com.cognizant.pace.CXOptimize.api.controller;


import com.cognizant.pace.CXOptimize.AnalysisEngine.*;
import com.cognizant.pace.CXOptimize.api.utils.AppUtils;
import com.cognizant.pace.CXOptimize.api.utils.MetricsProcessor;
import com.cognizant.pace.CXOptimize.api.utils.CXOptimizeStarter;
import com.cognizant.pace.CXOptimize.api.utils.SubscriptionValidator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;



@RestController
@Configuration
@EnableWebMvc
@EnableAsync
@Component
@RequestMapping("/")

public class ApiController
{
    @Autowired
    private CXOptimizeStarter cxopInit;

    @Autowired
    private MetricsProcessor metricsProcessor;

    @Autowired
    private AppUtils appUtils;

    @Autowired
    private SubscriptionValidator subscriptionValidator;

    @Value("${kibana.url}")
    private String kibanaUrl;


    @Value("${dashboard.name}")
    private String dashboardName;

    @Value("${dashboard.placeholder}")
    private String dashboardPlaceholder;

    @Value("${scenario.placeholder}")
    private String scenarioPlaceholder;

    @Value("${project.placeholder}")
    private String projectPlaceholder;

    @Value("${client.placeholder}")
    private String clientPlaceholder;

    @Value("${elasticsearch.config.index}")
    private String configIndex;

    @Value("${elasticsearch.default.runstats.index}")
    private String statsIndex;

    @Value("${elasticsearch.url.default}")
    private String baseUrl;

    @Value("${elasticsearch.config.type}")
    private String configType;


    @Value("${runIntervalInMinutes}")
    private long runIntervalInMinutes;

    @Value("${server.port}")
    private String portNum;

    @Value("${document.rentention.period.in.days}")
    private int defaultRetentionPeriod;

    @Value("${document.extract.period.in.days}")
    private int noOfDaysPeriod;

    @Value("${dashboard.link}")
    private String dashboardLink;

    @Value("${default.config}")
    private String defaultConfig;

    @Value("${location.service}")
    private String locationService;

    @Value("${geo.location.enabled}")
    private boolean geoIpFlag;



    private Logger LOGGER = LoggerFactory.getLogger("application");
    


    @RequestMapping(method = RequestMethod.POST, value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getLoginDetails(@RequestBody String request) throws Exception
    {
        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received getLoginDetails Request");
        LOGGER.debug("Login Request {}",request);
        request=request.replace("\\\"", "\"");
        request=request.replace("\"{", "{");
        request=request.replace("}\"", "}");
        JSONObject json = new JSONObject(request);
        LOGGER.debug("Calling LoginUtils.Login Method to fetch data from datasource");
        long startTime= System.currentTimeMillis();
        String result = LoginUtils.Login(baseUrl,json.getString(GlobalConstants.USERNAME),json.getString("Password")).toString();
        long endTime= System.currentTimeMillis();
        LOGGER.debug("Execution of LoginUtils.Login Method took {} ms",(endTime-startTime));
        LOGGER.debug("Final Response: {}",result);
        long overallEnd=System.currentTimeMillis();
        JSONObject resultJson = new JSONObject(result);
        if (resultJson.getBoolean("status"))
        {
            LOGGER.debug("Login Request Passed %s",resultJson);
            appUtils.asyncAuditLog(overallStart,"API","login",resultJson.getString("client"),"NA","NA","S","","",0,"",0,0,0,0);
        }
        else
        {
            LOGGER.debug("Login Request Failed %s",resultJson);
            appUtils.asyncAuditLog(overallStart,"API","login",resultJson.getString("client"),"NA","NA","F",resultJson.getString("reason"),"",0,"",0,0,0,0);
        }

        LOGGER.info("Completed getLoginDetails Request and took {} ms",(overallEnd-overallStart));

        return result;

    }

    @RequestMapping(method = RequestMethod.POST, value = "/createUser", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String createUser(@RequestBody String request) throws Exception
    {
        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received createUser Request");
        LOGGER.info("Create User Request {}",request);
        request=request.replace("\\\"", "\"");
        request=request.replace("\"{", "{");
        request=request.replace("}\"", "}");
        LOGGER.info("Create User Request Parsed {}",request);
        JSONObject json = new JSONObject(request);
        LOGGER.debug("Calling LoginUtils.CreateUser Method to fetch data from datasource");
        long startTime= System.currentTimeMillis();
        if(json.has("UserName") && json.has("Password") && json.has("Role") && (json.getString("Role").contains("USER") || json.getString("Role").contains("ADMIN")))
        {
            String result = LoginUtils.CreateUser(baseUrl,json).toString();
            long endTime= System.currentTimeMillis();
            LOGGER.debug("Execution of LoginUtils.CreateUser Method took {} ms",(endTime-startTime));
            LOGGER.debug("Create User Response: {}",result);
            long overallEnd=System.currentTimeMillis();
            LOGGER.info("Completed createUser Request and took {} ms",(overallEnd-overallStart));
            appUtils.asyncAuditLog(overallStart,"API","createUser","NA","NA","NA","S","","",0,"",0,0,0,0);
            return result;
        }
        else
        {
            long overallEnd=System.currentTimeMillis();
            appUtils.asyncAuditLog(overallStart,"API","createUser","NA","NA","NA","F","User Creation Failed","",0,"",0,0,0,0);
            LOGGER.info("Completed createUser Request and took {} ms",(overallEnd-overallStart));
            return "{\"Status\" : \"Failed\",\n\"Reason\" : \"User Creation Failed\"}";
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/updatePassword", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String updatePassword(@RequestBody String request) throws Exception
    {
        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received updatePassword Request");
        LOGGER.debug("updatePassword Request {}",request);
        request=request.replace("\\\"", "\"");
        request=request.replace("\"{", "{");
        request=request.replace("}\"", "}");
        JSONObject json = new JSONObject(request);
        LOGGER.debug("Calling LoginUtils.UpdatePassword Method to fetch data from datasource");
        long startTime= System.currentTimeMillis();
        if(json.has("UserName") && json.has("OldPassword") && json.has("NewPassword"))
        {
            String result = LoginUtils.UpdatePassword(baseUrl,json).toString();
            long endTime= System.currentTimeMillis();
            LOGGER.debug("Execution of LoginUtils.UpdatePassword Method took {} ms",(endTime-startTime));
            LOGGER.debug("UpdatePassword Response: {}",result);
            long overallEnd=System.currentTimeMillis();
            LOGGER.info("Completed updatePassword Request and took {} ms",(overallEnd-overallStart));
            appUtils.asyncAuditLog(overallStart,"API","updatePassword","NA","NA","NA","S","","",0,"",0,0,0,0);
            return result;
        }
        else
        {
            long overallEnd=System.currentTimeMillis();
            appUtils.asyncAuditLog(overallStart,"API","updatePassword","NA","NA","NA","F","Validation Failed for input request","",0,"",0,0,0,0);
            LOGGER.info("Completed updatePassword Request and took {} ms",(overallEnd-overallStart));
            return "{\"Status\" : \"Failed\",\n\"Reason\" : \"Update failed due to missing mandatory fields UserName or OldPassword or NewPassword\"}";
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/manageClientAccess", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String manageClientAccess(@RequestBody String request) throws Exception
    {
        long overallStart=System.currentTimeMillis();
        String result = null;
        LOGGER.info("Received manageClientAccess Request");
        LOGGER.debug("manageClientAccess  Request {}", request);
        request=request.replace("\\\"", "\"");
        request=request.replace("\"{", "{");
        request=request.replace("}\"", "}");
        JSONObject json = new JSONObject(request);

        LOGGER.debug("Calling LoginUtils.ManageClientAccess Method to fetch data from datasource");
        long startTime = System.currentTimeMillis();
        result = LoginUtils.ManageClientAccess(baseUrl, json).toString();
        long endTime = System.currentTimeMillis();
        LOGGER.debug("Execution of LoginUtils.ManageClientAccess Method took {} ms", (endTime - startTime));
        LOGGER.debug("manageClientAccess Response: ()", result);
        appUtils.asyncAuditLog(overallStart,"API","manageClientAccess","NA","NA","NA","S","","",0,"",0,0,0,0);
        long overallEnd = System.currentTimeMillis();
        LOGGER.info("Completed manageClientAccess Request and took {} ms", (overallEnd - overallStart));
        return result;

    }

    @RequestMapping(method = RequestMethod.GET, value = "/getRunDetails", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody String getRunDetails(
            @RequestParam(value = "ClientName", required = true) String ClientName,
            @RequestParam(value = "ProjectName", required = true) String ProjectName,
            @RequestParam(value = "Scenario", required = true) String Scenario,
            @RequestParam(value = "TimeZone", required = false) String timeZone
    ) throws Exception
    {
        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received getRunDetails Request");
        JSONObject result=null;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getRunDetails",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {
                if(timeZone == null || timeZone.equals(""))
                {
                    validateResult.put("TimeZone","UTC");
                }
                else
                {
                    validateResult.put("TimeZone",timeZone);
                }

                result = PaceAnalysisEngine.getRunDetails(validateResult,noOfDaysPeriod);
            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","getRunDetails",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getRunDetails Request for Client: {}, Project: {}, Scenario: {} and took {} ms",ClientName,ProjectName,Scenario,(overallEnd-overallStart));
        appUtils.asyncAuditLog(overallStart,"API","getRunDetails",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
        return result.toString();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/getClientDetails", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody String getClientDetails(
            @RequestBody String request
    ) throws Exception
    {
        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received getClientDetails Request");
        request=request.replace("\\\"", "\"");
        request=request.replace("\"{", "{");
        request=request.replace("}\"", "}");
        JSONObject json = new JSONObject(request);

        if(!json.has(GlobalConstants.USERNAME) || json.getString(GlobalConstants.USERNAME).trim().equals(""))
        {
            LOGGER.error("UserName {} cannot be Blank",json.getString(GlobalConstants.USERNAME).trim());
            appUtils.asyncAuditLog(overallStart,"API","getClientDetails","NA","NA","NA","F","UserName cannot be null or blank","",0,"",0,0,0,0);
            return "{\"Status\" : \"Failed\",\n\"Reason\" : \"UserName cannot be null or blank\"}";
        }
        else
        {
            LOGGER.debug("Calling PaceAnalysisEngine.getClientRunDetails Method");
            long startTime= System.currentTimeMillis();
            String result = PaceAnalysisEngine.getClientDetails(json.getString("UserName"),baseUrl,noOfDaysPeriod,json.has("TimeZone") ? json.getString("TimeZone") : "UTC").toString();
            long endTime= System.currentTimeMillis();
            LOGGER.debug("Execution of PaceAnalysisEngine.getClientRunDetails Method took {} ms",(endTime-startTime));
            LOGGER.debug("Final Response: " + result);
            long overallEnd=System.currentTimeMillis();
            LOGGER.info("Completed getClientDetails Request and took {} ms",(overallEnd-overallStart));
            appUtils.asyncAuditLog(overallStart,"API","getClientDetails","NA","NA","NA","S","","",0,"",0,0,0,0);
            return result;
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/insertStats", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String insertStats(@RequestBody String request, HttpServletRequest servletRequest) throws Exception
    {
        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received insertStats Request");
        LOGGER.debug("insertStats  Request : {}",request);
        JSONObject body = new JSONObject(request);


        Map<String,String> response = new HashMap<>();
        String ClientName = "";
        String ProjectName = "";
        String Scenario = "";

        if(body.has("details"))
        {
            JSONObject details = new JSONObject(body.getString("details").toString());
            ClientName = details.getString("ClientName").toLowerCase().trim();
            ProjectName = details.getString("ProjectName").toLowerCase().trim();
            Scenario = details.getString("Scenario").toLowerCase().trim();
            LOGGER.debug("ClientName : {}",ClientName);
            boolean isSubscriptionValid = subscriptionValidator.validateSubscription(ClientName);
            if(isSubscriptionValid)
            {

                String ipAddress = servletRequest.getHeader("X-FORWARDED-FOR");
                LOGGER.debug("XForwarded for : {}",ipAddress);
                if (ipAddress == null)
                {
                    ipAddress = servletRequest.getRemoteAddr();
                    LOGGER.debug("GetRemote Addr : {}",ipAddress);
                }
                LOGGER.debug("Calling MetricsParser.statsParser");
                long startTime= System.currentTimeMillis();
                response = metricsProcessor.statsParser(ClientName,ProjectName,Scenario,body,baseUrl,geoIpFlag,ipAddress,locationService);
                //response = MetricsParser.statsParser(body,baseUrl,geoIpFlag,ipAddress,locationService);
                long endTime= System.currentTimeMillis();
                LOGGER.debug("Execution of method MetricsParser.statsParser took {} ms",(endTime-startTime));
                if(response.get("Status").equals("Success"))
                {
                    LOGGER.debug("Insert data success for ClientName {} ProjectName {} Scenario {} TransactionName {}",ClientName,ProjectName,Scenario,details.getString("transactionName").toLowerCase().trim());
                    appUtils.asyncAuditLog(overallStart,"API","insertStats",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
                }
                else
                {
                    LOGGER.debug("Insert data failed for ClientName {} ProjectName {} Scenario {} TransactionName {} Reason {}",ClientName,ProjectName,Scenario,details.getString("transactionName").toLowerCase().trim(),response.toString());
                    appUtils.asyncAuditLog(overallStart,"API","insertStats",ClientName,ProjectName,Scenario,"F",response.get("Reason"),"",0,"",0,0,0,0);
                }

            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","insertStats",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                response.put("Status","Failed");
                response.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }
        }
        else
        {
            LOGGER.info("Invalid Request");
            appUtils.asyncAuditLog(overallStart,"API","insertStats",ClientName,ProjectName,Scenario,"F","Invalid Request","",0,"",0,0,0,0);
            response.put("Status","Failed");
            response.put("Reason","Invalid request");
        }

        LOGGER.debug("insert stats Response:" + response.toString());
        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed insertStats Request and took {} ms",(overallEnd-overallStart));
        return new JSONObject(response.toString()).toString();

    }

    @RequestMapping(method = RequestMethod.POST, value = "/insertBoomerangStats", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String insertBoomerangStats(@RequestBody String request, HttpServletRequest servletRequest) throws Exception
    {

        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received insertBoomerangStats Request");
        LOGGER.debug("insertBoomerangStats  Request : {}",request);
        JSONObject body = new JSONObject(request);


        Map<String,String> response = new HashMap<>();
        String ClientName = "";
        String ProjectName = "";
        String Scenario = "";

        if(body.has("cxoptimize"))
        {
            //LOGGER.info("Request : {}",body.get)
            JSONObject details = body.getJSONObject("cxoptimize").getJSONObject("details");
            ClientName = details.getString("client").toLowerCase().trim();
            ProjectName = details.getString("project").toLowerCase().trim();
            Scenario = details.getString("scenario").toLowerCase().trim();
            LOGGER.debug("ClientName : {}",ClientName);
            boolean isSubscriptionValid = subscriptionValidator.validateSubscription(ClientName);
            if(isSubscriptionValid)
            {

                String ipAddress = servletRequest.getHeader("X-FORWARDED-FOR");
                LOGGER.debug("XForwarded for : {}",ipAddress);
                if (ipAddress == null)
                {
                    ipAddress = servletRequest.getRemoteAddr();
                    LOGGER.debug("GetRemote Addr : {}",ipAddress);
                }
                LOGGER.debug("Calling MetricsParser.boomerangParser");
                long startTime= System.currentTimeMillis();
                response = metricsProcessor.boomerangParser(ClientName,ProjectName,Scenario,body,baseUrl,geoIpFlag,ipAddress,locationService);
                //response = MetricsParser.boomerangParser(body,baseUrl,geoIpFlag,ipAddress,locationService);
                long endTime= System.currentTimeMillis();
                LOGGER.debug("Execution of method MetricsParser.boomerangParser took {} ms",(endTime-startTime));
                if(response.get("Status").equals("Success"))
                {
                    LOGGER.debug("Insert data success for ClientName {} ProjectName {} Scenario {} TransactionName {}",ClientName,ProjectName,Scenario,details.getString("transactionName").toLowerCase().trim());
                    appUtils.asyncAuditLog(overallStart,"API","insertBoomerangStats",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
                }
                else
                {
                    LOGGER.debug("Insert data failed for ClientName {} ProjectName {} Scenario {} TransactionName {} Reason {}",ClientName,ProjectName,Scenario,details.getString("transactionName").toLowerCase().trim(),response.toString());
                    appUtils.asyncAuditLog(overallStart,"API","insertBoomerangStats",ClientName,ProjectName,Scenario,"F",response.get("Reason"),"",0,"",0,0,0,0);
                }

            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","insertBoomerangStats",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                response.put("Status","Failed");
                response.put("Reason","Expired or Invalid Subscription");
            }
        }
        else
        {
            LOGGER.info("Invalid Request");
            appUtils.asyncAuditLog(overallStart,"API","insertBoomerangStats",ClientName,ProjectName,Scenario,"F","Invalid Request","",0,"",0,0,0,0);
            response.put("Status","Failed");
            response.put("Reason","Invalid request");
        }

        LOGGER.debug("insert stats Response:" + response.toString());
        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed insertStats Request and took {} ms",(overallEnd-overallStart));
        return new JSONObject(response.toString()).toString();


    }


    @RequestMapping(method = RequestMethod.POST, value = "/getSummaryReportWithStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getSummaryReportWithStatus(@RequestBody String request) throws Exception
    {

        long overallStart=System.currentTimeMillis();
        request=request.replace("\\\"", "\"");
        request=request.replace("\"{", "{");
        request=request.replace("}\"", "}");
        JSONObject json = new JSONObject(request);
        String ClientName = json.getString("ClientName");
        String ProjectName = json.getString("ProjectName");
        String Scenario = json.getString("Scenario");
        String analysisType = json.has("AnalysisType") ? json.getString("AnalysisType") : "Run";
        String RunID = json.has("RunID") ? json.getString("RunID") : "";
        String baselineRunID = json.has("BaselineRunID") ? json.getString("BaselineRunID") : "";
        String scale = json.has("scale") ? json.getString("scale") : "milliseconds";
        String override = json.has("Override") ? json.getString("Override") : "false";
        String response = null;
        boolean flag = true;

        LOGGER.info("Received getSummaryReportWithStatus Request for Client: {}, Project: {}, Scenario: {}",ClientName,ProjectName,Scenario);
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            flag = false;
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {

                if(analysisType.trim().equals("Run") || analysisType.trim().equals("Transaction") || analysisType.trim().equals("Time")) {
                    validateResult.put("AnalysisType",analysisType.trim());
                    if (analysisType.trim().equals("Run")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason",GlobalConstants.RUNID_ERROR_MSG);
                        }
                        if (baselineRunID == null || baselineRunID.trim().equals("") || baselineRunID.trim().matches("[0-9]+")) {
                            validateResult.put("BaselineRun", ((baselineRunID == null || baselineRunID.trim().equals("")) ? null : baselineRunID));
                        } else {
                            flag = false;
                            LOGGER.error("BaselineRunID can allow only numbers or empty value {}", baselineRunID);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.BRUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.BRUNID_ERROR_MSG);
                        }
                    }
                    if (analysisType.trim().equals("Transaction")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                        validateResult.put("BaselineRun", "0");
                    }
                    if (analysisType.trim().equals("Time"))
                    {
                        String BaselineStart = json.getString("BaselineStart");
                        String BaselineEnd = json.getString("BaselineEnd");
                        String CurrentStart = json.getString("CurrentStart");
                        String CurrentEnd = json.getString("CurrentEnd");

                        if (BaselineStart.trim().equals("") || BaselineEnd.trim().equals("") || CurrentStart.trim().equals("") || CurrentEnd.trim().equals("")) {
                            flag = false;
                            LOGGER.error(GlobalConstants.TIME_ERROR_MSG);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.TIME_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.TIME_ERROR_MSG);
                        } else {
                            validateResult.put("BaselineStart", BaselineStart);
                            validateResult.put("BaselineEnd", BaselineEnd);
                            validateResult.put("CurrentStart", CurrentStart);
                            validateResult.put("CurrentEnd", CurrentEnd);
                        }
                    }
                }
                else
                {
                    flag = false;
                    LOGGER.error("Invalid analysis type {}", analysisType.trim());
                    appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.ANALYSISTYPE_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.ANALYSISTYPE_ERROR_MSG);

                }

                if(scale != null && scale.trim().equals("seconds"))
                {
                    validateResult.put("scale","seconds");
                }
                else
                {
                    validateResult.put("scale","milliseconds");
                }

                if(flag) {

                    validateResult.put("esUrl", baseUrl);
                    validateResult.put("override", ((override == null || override.toLowerCase().trim().equals("false") || !override.toLowerCase().trim().equals("true")) ? "false" : override));

                    List<Object> summaryReport = PaceAnalysisEngine.getStatusForCICD(json,baseUrl);
                    if (summaryReport.get(3).toString().equals("false")) {
                        flag = false;
                        appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                        LOGGER.debug("getSummaryReport - Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {}", ClientName, ProjectName, Scenario);
                        validateResult.put("ClientName", appUtils.normaliseInput(ClientName));
                        validateResult.put("ProjectName", appUtils.normaliseInput(ProjectName));
                        validateResult.put("Scenario", appUtils.normaliseInput(Scenario));
                        validateResult.remove("esUrl");
                        validateResult.put("Status", "Failed");
                        validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                    }
                    else
                    {
                        flag = true;
                        response = summaryReport.get(0).toString();
                        int totalTxns = 0;
                        if (summaryReport.get(1).toString().trim().length() > 0) {
                            List<String> transactionArray = Arrays.asList(summaryReport.get(2).toString().replaceAll("[\\[\\]]", "").split(","));
                            String txnName = "";
                            String timeTaken = "";
                            String recommendationsCount = "";
                            String score = "";
                            for (String txn : transactionArray) {
                                List<String> txnDetails = Arrays.asList(txn.split("#"));

                                for (int j = 0; j < txnDetails.size(); j++) {
                                    if (j == 0) {
                                        txnName = txnDetails.get(j).trim();
                                    }
                                    if (j == 1) {
                                        timeTaken = txnDetails.get(j);
                                    }
                                    if (j == 2) {
                                        recommendationsCount = txnDetails.get(j);
                                    }
                                    if (j == 3) {
                                        score = txnDetails.get(j);
                                    }

                                }
                                totalTxns = totalTxns + 1;
                                if(analysisType.trim().equals("Time"))
                                {
                                    appUtils.asyncAuditLog(overallStart,"Transaction","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"S","",analysisType.trim(),0, txnName,0, Long.parseLong(timeTaken), Integer.parseInt(recommendationsCount), Integer.parseInt(score));
                                }
                                else
                                {
                                    appUtils.asyncAuditLog(overallStart,"Transaction","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"S","",analysisType.trim(), Integer.parseInt(RunID), txnName,0, Long.parseLong(timeTaken), Integer.parseInt(recommendationsCount), Integer.parseInt(score));
                                }

                            }
                        }
                        appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"S","",analysisType.trim(),0,"",totalTxns,0,0,0);

                    }
                }

            }
            else
            {
                flag = false;
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getSummaryReportWithStatus Request for Client: {}, Project: {}, Scenario: {} took {} ms",ClientName,ProjectName,Scenario,(overallEnd-overallStart));

        if(flag)
        {
            return response;
        }
        else
        {
            return validateResult.toString();

        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/getSummaryReport", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getSummaryReport (
            @RequestParam(value = "ClientName", required = true) String ClientName,
            @RequestParam(value = "ProjectName", required = true) String ProjectName,
            @RequestParam(value = "Scenario", required = true) String Scenario,
            @RequestParam(value = "AnalysisType", required = true) String analysisType,
            @RequestParam(value = "RunID",required = false) String RunID,
            @RequestParam(value = "BaselineRunID",required = false) String baselineRunID,
            @RequestParam(value = "BaselineStart",required = false) String BaselineStart,
            @RequestParam(value = "BaselineEnd",required = false) String BaselineEnd,
            @RequestParam(value = "CurrentStart",required = false) String CurrentStart,
            @RequestParam(value = "CurrentEnd",required = false) String CurrentEnd,
            @RequestParam(value = "Override", required = false) String override,
            @RequestParam(value = "Scale", required = false) String scale)
            throws Exception
    {

        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received getSummaryReport Request for Client: {}, Project: {}, Scenario: {}",ClientName,ProjectName,Scenario);
        String response = null;
        boolean flag = true;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            flag = false;
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {

                if(analysisType.trim().equals("Run") || analysisType.trim().equals("Transaction") || analysisType.trim().equals("Time")) {
                    validateResult.put("AnalysisType",analysisType.trim());
                    if (analysisType.trim().equals("Run")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                        if (baselineRunID == null || baselineRunID.trim().equals("") || baselineRunID.trim().matches("[0-9]+")) {
                            validateResult.put("BaselineRun", ((baselineRunID == null || baselineRunID.trim().equals("")) ? null : baselineRunID));
                        } else {
                            flag = false;
                            LOGGER.error("BaselineRunID can allow only numbers or empty value {}", baselineRunID);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.BRUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason",  GlobalConstants.BRUNID_ERROR_MSG);
                        }
                    }
                    if (analysisType.trim().equals("Transaction")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                        validateResult.put("BaselineRun", "0");
                    }
                    if (analysisType.trim().equals("Time")) {
                        if (BaselineStart.trim().equals("") || BaselineEnd.trim().equals("") || CurrentStart.trim().equals("") || CurrentEnd.trim().equals("")) {
                            flag = false;
                            LOGGER.error(GlobalConstants.TIME_ERROR_MSG);
                            appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.TIME_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.TIME_ERROR_MSG);
                        } else {
                            validateResult.put("BaselineStart", BaselineStart);
                            validateResult.put("BaselineEnd", BaselineEnd);
                            validateResult.put("CurrentStart", CurrentStart);
                            validateResult.put("CurrentEnd", CurrentEnd);
                        }
                    }
                }
                else
                {
                    flag = false;
                    LOGGER.error("Invalid analysis type {}", analysisType.trim());
                    appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.ANALYSISTYPE_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.ANALYSISTYPE_ERROR_MSG);

                }

                if(scale != null && scale.trim().equals("seconds"))
                {
                    validateResult.put("scale","seconds");
                }
                else
                {
                    validateResult.put("scale","milliseconds");
                }

                if(flag) {

                    validateResult.put("esUrl", baseUrl);
                    validateResult.put("override", ((override == null || override.toLowerCase().trim().equals("false") || !override.toLowerCase().trim().equals("true")) ? "false" : override));

                    List<Object> summaryReport = PaceAnalysisEngine.generateSummaryReport(validateResult);
                    if (summaryReport.get(3).toString().equals("false")) {
                        flag = false;
                        appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);LOGGER.debug("getSummaryReport - Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {}", ClientName, ProjectName, Scenario);
                        validateResult.put("ClientName", appUtils.normaliseInput(ClientName));
                        validateResult.put("ProjectName", appUtils.normaliseInput(ProjectName));
                        validateResult.put("Scenario", appUtils.normaliseInput(Scenario));
                        validateResult.remove("esUrl");
                        validateResult.put("Status", "Failed");
                        validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                    }
                    else
                    {
                        flag = true;
                        response = summaryReport.get(0).toString();
                        int totalTxns = 0;
                        if (summaryReport.get(1).toString().trim().length() > 0) {
                            List<String> transactionArray = Arrays.asList(summaryReport.get(2).toString().replaceAll("[\\[\\]]", "").split(","));
                            String txnName = "";
                            String timeTaken = "";
                            String recommendationsCount = "";
                            String score = "";
                            for (String txn : transactionArray) {
                                List<String> txnDetails = Arrays.asList(txn.split("#"));

                                for (int j = 0; j < txnDetails.size(); j++) {
                                    if (j == 0) {
                                        txnName = txnDetails.get(j).trim();
                                    }
                                    if (j == 1) {
                                        timeTaken = txnDetails.get(j);
                                    }
                                    if (j == 2) {
                                        recommendationsCount = txnDetails.get(j);
                                    }
                                    if (j == 3) {
                                        score = txnDetails.get(j);
                                    }

                                }
                                totalTxns = totalTxns + 1;
                                if(analysisType.trim().equals("Time"))
                                {
                                    appUtils.asyncAuditLog(overallStart,"Transaction","getSummaryReport",ClientName,ProjectName,Scenario,"S","",analysisType.trim(),0, txnName,0, Long.parseLong(timeTaken), Integer.parseInt(recommendationsCount), Integer.parseInt(score));
                                }
                                else
                                {
                                    appUtils.asyncAuditLog(overallStart,"Transaction","getSummaryReport",ClientName,ProjectName,Scenario,"S","",analysisType.trim(), Integer.parseInt(RunID), txnName,0, Long.parseLong(timeTaken), Integer.parseInt(recommendationsCount), Integer.parseInt(score));
                                }
                            }
                        }
                        appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"S","",analysisType.trim(),0,"",totalTxns,0,0,0);
                    }
                }

            }
            else
            {
                flag = false;
                LOGGER.error("Invalid Subscription {}",ClientName);
                appUtils.asyncAuditLog(overallStart,"API","getSummaryReport",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getSummaryReport Request for Client: {}, Project: {}, Scenario: {} took {} ms",ClientName,ProjectName,Scenario,(overallEnd-overallStart));

        if(flag)
        {
            return response;
        }
        else
        {
            return new JSONObject(validateResult).toString();

        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/getCBAnalysis", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE})
    public @ResponseBody String getCBAnalysis(
            @RequestParam(value = "ClientName", required = true) String ClientName,
            @RequestParam(value = "ProjectName", required = true) String ProjectName,
            @RequestParam(value = "Scenario", required = true) String Scenario,
            @RequestParam(value = "TransactionName",required = true) String transactionName,
            @RequestParam(value = "AnalysisType", required = true) String analysisType,
            @RequestParam(value = "RunID",required = false) String RunID,
            @RequestParam(value = "BaselineStart",required = false) String BaselineStart,
            @RequestParam(value = "BaselineEnd",required = false) String BaselineEnd,
            @RequestParam(value = "CurrentStart",required = false) String CurrentStart,
            @RequestParam(value = "CurrentEnd",required = false) String CurrentEnd
    )
            throws Exception
    {

        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received getCBAnalysis Request for Client: {}, Project: {}, Scenario: {} Transaction {}",ClientName,ProjectName,Scenario,transactionName);
        String response = null;
        boolean flag = true;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            flag = false;
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {

                if(analysisType.trim().equals("Run") || analysisType.trim().equals("Transaction") || analysisType.trim().equals("Time")) {
                    validateResult.put("AnalysisType",analysisType.trim());
                    if (analysisType.trim().equals("Run")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                    }
                    if (analysisType.trim().equals("Transaction")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                    }
                    if (analysisType.trim().equals("Time")) {
                        if (BaselineStart.trim().equals("") || BaselineEnd.trim().equals("") || CurrentStart.trim().equals("") || CurrentEnd.trim().equals("")) {
                            flag = false;
                            LOGGER.error("BaselineStart,BaselineEnd,CurrentStart,CurrentEnd cannot be null or blank");
                            appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.TIME_ERROR_MSG,"",0,"",0,0,0,0);

                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.TIME_ERROR_MSG);
                        } else {
                            validateResult.put("BaselineStart", BaselineStart);
                            validateResult.put("BaselineEnd", BaselineEnd);
                            validateResult.put("CurrentStart", CurrentStart);
                            validateResult.put("CurrentEnd", CurrentEnd);
                        }
                    }
                }
                else
                {
                    flag = false;
                    LOGGER.error("Invalid analysis type {}", analysisType.trim());
                    appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.ANALYSISTYPE_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason",  GlobalConstants.ANALYSISTYPE_ERROR_MSG);

                }

                if (transactionName == null || transactionName.trim().equals(""))
                {
                    flag = false;
                    LOGGER.error("TransactionName cannot be empty {}", transactionName);
                    appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.TRANS_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.TRANS_ERROR_MSG);
                    validateResult.put("TransactionName",appUtils.normaliseInput(transactionName.trim()));
                }
                else
                {
                    validateResult.put("txnName", transactionName);
                }

                if(flag) {

                    validateResult.put("esUrl", baseUrl);

                    List<Object> summaryReport = PaceAnalysisEngine.getCBTDetails(validateResult);
                    if (summaryReport.get(0).toString().equals("false")) {
                        flag = false;
                        appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                        LOGGER.debug("getCBAnalysis - Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {}", ClientName, ProjectName, Scenario);
                        validateResult.put("ClientName", appUtils.normaliseInput(ClientName));
                        validateResult.put("ProjectName", appUtils.normaliseInput(ProjectName));
                        validateResult.put("Scenario", appUtils.normaliseInput(Scenario));
                        validateResult.remove("esUrl");
                        validateResult.put("Status", "Failed");
                        validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                    }
                    else
                    {
                        flag = true;
                        response = summaryReport.get(1).toString();
                        appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
                    }
                }

            }
            else
            {
                flag = false;
                LOGGER.error("Invalid Subscription {}",ClientName);
                appUtils.asyncAuditLog(overallStart,"API","getCBAnalysis",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getCBAnalysis Request for Client: {}, Project: {}, Scenario: {} Transaction {} took {} ms",ClientName,ProjectName,Scenario,transactionName,(overallEnd-overallStart));

        if(flag)
        {
            return response;
        }
        else
        {
            return new JSONObject(validateResult).toString();

        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/getAllTransactionSamples", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody String getAllTransactionSamples(
            @RequestParam(value = "ClientName", required = true) String ClientName,
            @RequestParam(value = "ProjectName", required = true) String ProjectName,
            @RequestParam(value = "Scenario", required = true) String Scenario,
            @RequestParam(value = "AnalysisType", required = true) String analysisType,
            @RequestParam(value = "TransactionName",required = true) String transactionName,
            @RequestParam(value = "RunID",required = false) String RunID,
            @RequestParam(value = "BaselineStart",required = false) String BaselineStart,
            @RequestParam(value = "BaselineEnd",required = false) String BaselineEnd,
            @RequestParam(value = "CurrentStart",required = false) String CurrentStart,
            @RequestParam(value = "CurrentEnd",required = false) String CurrentEnd,
            @RequestParam(value = "Scale",required = false) String scale
    )
            throws Exception
    {

        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received getAllTransactionSamples Request for Client: {}, Project: {}, Scenario: {} Transaction {}",ClientName,ProjectName,Scenario,transactionName);
        String response = null;
        boolean flag = true;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            flag = false;
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {

                if(analysisType.trim().equals("Run") || analysisType.trim().equals("Transaction") || analysisType.trim().equals("Time"))
                {
                    validateResult.put("AnalysisType",analysisType.trim());
                    if (analysisType.trim().equals("Run"))
                    {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+"))
                        {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        }
                        else
                        {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                    }
                    if (analysisType.trim().equals("Transaction"))
                    {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+"))
                        {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        }
                        else
                        {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                    }
                    if (analysisType.trim().equals("Time"))
                    {
                        if (BaselineStart.trim().equals("") || BaselineEnd.trim().equals("") || CurrentStart.trim().equals("") || CurrentEnd.trim().equals("")) {
                            flag = false;
                            LOGGER.error(GlobalConstants.TIME_ERROR_MSG);
                            appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"F",GlobalConstants.TIME_ERROR_MSG,"",0,"",0,0,0,0);

                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.TIME_ERROR_MSG);
                        }
                        else
                        {
                            validateResult.put("BaselineStart", BaselineStart);
                            validateResult.put("BaselineEnd", BaselineEnd);
                            validateResult.put("CurrentStart", CurrentStart);
                            validateResult.put("CurrentEnd", CurrentEnd);
                        }
                    }
                }
                else
                {
                    flag = false;
                    LOGGER.error("Invalid analysis type {}", analysisType.trim());
                    appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"F",GlobalConstants.ANALYSISTYPE_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.ANALYSISTYPE_ERROR_MSG);

                }

                if (transactionName == null || transactionName.trim().equals(""))
                {
                    flag = false;
                    LOGGER.error("TransactionName cannot be empty {}", transactionName);
                    appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"F",GlobalConstants.TRANS_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.TRANS_ERROR_MSG);
                    validateResult.put("TransactionName",appUtils.normaliseInput(transactionName.trim()));
                }
                else
                {
                    validateResult.put("txnName", transactionName);
                }

                if(scale != null && scale.trim().equals("seconds"))
                {
                    validateResult.put("scale","seconds");
                }
                else
                {
                    validateResult.put("scale","milliseconds");
                }

                if(flag)
                {
                    validateResult.put("esUrl", baseUrl);
                    List<Object> summaryReport = PaceAnalysisEngine.getAllSamplesForTransaction(validateResult);
                    if (summaryReport.get(0).toString().equals("false"))
                    {
                        flag = false;
                        appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                        LOGGER.debug("getAllTransactionSamples - Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {}", ClientName, ProjectName, Scenario);
                        validateResult.put("ClientName", appUtils.normaliseInput(ClientName));
                        validateResult.put("ProjectName", appUtils.normaliseInput(ProjectName));
                        validateResult.put("Scenario", appUtils.normaliseInput(Scenario));
                        validateResult.remove("esUrl");
                        validateResult.put("Status", "Failed");
                        validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                    }
                    else
                    {
                        response = summaryReport.get(1).toString();
                        flag = true;
                        appUtils.asyncAuditLog(overallStart,"API","getAllTransactionSamples",ClientName,ProjectName,Scenario,"S","",analysisType.trim(),0,"",0,0,0,0);
                    }
                }

            }
            else
            {
                flag = false;
                LOGGER.error("Invalid Subscription {}",ClientName);
                appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getAllTransactionSamples Request for Client: {}, Project: {}, Scenario: {} Transaction {} took {} ms",ClientName,ProjectName,Scenario,transactionName,(overallEnd-overallStart));

        if(flag)
        {
            return response;
        }
        else
        {
            return new JSONObject(validateResult).toString();

        }


    }


    @RequestMapping(method = RequestMethod.GET, value = "/compareResources", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE})
    public @ResponseBody String compareResources(
            @RequestParam(value = "ClientName", required = true) String ClientName,
            @RequestParam(value = "ProjectName", required = true) String ProjectName,
            @RequestParam(value = "Scenario", required = true) String Scenario,
            @RequestParam(value = "TransactionName",required = true) String transactionName,
            @RequestParam(value = "AnalysisType", required = true) String analysisType,
            @RequestParam(value = "RunID",required = false) String RunID,
            @RequestParam(value = "BaselineRunID",required = false) String baselineRunID,
            @RequestParam(value = "BaselineStart",required = false) String BaselineStart,
            @RequestParam(value = "BaselineEnd",required = false) String BaselineEnd,
            @RequestParam(value = "CurrentStart",required = false) String CurrentStart,
            @RequestParam(value = "CurrentEnd",required = false) String CurrentEnd,
            @RequestParam(value = "CurrentSampleValue" ,required = true) String currentSampleValue,
            @RequestParam(value = "BaselineSampleValue" ,required = false) String baselineSampleValue
    ) throws Exception
    {

        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received compareResources Request for Client: {}, Project: {}, Scenario: {} Transaction {}",ClientName,ProjectName,Scenario,transactionName);
        String response = null;
        boolean flag = true;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            flag = false;
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {

                if(analysisType.trim().equals("Run") || analysisType.trim().equals("Transaction") || analysisType.trim().equals("Time"))
                {
                    validateResult.put("AnalysisType",analysisType.trim());
                    if (analysisType.trim().equals("Run")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                        if (baselineRunID == null || baselineRunID.trim().equals("") || baselineRunID.trim().matches("[0-9]+")) {
                            validateResult.put("BaselineRun", ((baselineRunID == null || baselineRunID.trim().equals("")) ? null : baselineRunID));
                        } else {
                            flag = false;
                            LOGGER.error("BaselineRunID can allow only numbers or empty value {}", baselineRunID);
                            appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.BRUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason",GlobalConstants.BRUNID_ERROR_MSG);
                        }
                    }
                    if (analysisType.trim().equals("Transaction")) {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+")) {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        } else {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason",GlobalConstants.RUNID_ERROR_MSG);
                        }
                        validateResult.put("BaselineRun", "0");
                    }
                    if (analysisType.trim().equals("Time")) {
                        if (BaselineStart.trim().equals("") || BaselineEnd.trim().equals("") || CurrentStart.trim().equals("") || CurrentEnd.trim().equals("")) {
                            flag = false;
                            LOGGER.error(GlobalConstants.TIME_ERROR_MSG);
                            appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.TIME_ERROR_MSG,"",0,"",0,0,0,0);

                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.TIME_ERROR_MSG);
                        } else {
                            validateResult.put("BaselineStart", BaselineStart);
                            validateResult.put("BaselineEnd", BaselineEnd);
                            validateResult.put("CurrentStart", CurrentStart);
                            validateResult.put("CurrentEnd", CurrentEnd);
                        }
                    }
                }
                else
                {
                    flag = false;
                    LOGGER.error("Invalid analysis type {}", analysisType.trim());
                    appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.ANALYSISTYPE_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.ANALYSISTYPE_ERROR_MSG);

                }

                if (transactionName == null || transactionName.trim().equals(""))
                {
                    flag = false;
                    LOGGER.error("TransactionName cannot be empty {}", transactionName);
                    appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.TRANS_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.TRANS_ERROR_MSG);
                    validateResult.put("TransactionName",appUtils.normaliseInput(transactionName));
                }
                else
                {
                    validateResult.put("txnName", transactionName.trim());
                }


                if(flag)
                {
                    validateResult.put("esUrl", baseUrl);
                    validateResult.put("currentSample",currentSampleValue.trim());
                    validateResult.put("baselineSample",baselineSampleValue.trim());
                    List<Object> summaryReport = PaceAnalysisEngine.compareResourceMetrics(validateResult);
                    if (summaryReport.get(0).toString().equals("false"))
                    {
                        flag = false;
                        appUtils.asyncAuditLog(overallStart,"API","getSummaryReportWithStatus",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                        LOGGER.debug("compareResources - Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {}", ClientName, ProjectName, Scenario);
                        validateResult.put("ClientName", appUtils.normaliseInput(ClientName));
                        validateResult.put("ProjectName", appUtils.normaliseInput(ProjectName));
                        validateResult.put("Scenario", appUtils.normaliseInput(Scenario));
                        validateResult.remove("esUrl");
                        validateResult.put("Status", "Failed");
                        validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                    }
                    else
                    {
                        response = summaryReport.get(1).toString();
                        flag = true;
                        appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
                    }
                }

            }
            else
            {
                flag = false;
                LOGGER.error("Invalid Subscription {}",ClientName);
                appUtils.asyncAuditLog(overallStart,"API","compareResources",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed compareResources Request for Client: {}, Project: {}, Scenario: {} Transaction {} took {} ms",ClientName,ProjectName,Scenario,transactionName,(overallEnd-overallStart));

        if(flag)
        {
            return response;
        }
        else
        {
            return new JSONObject(validateResult).toString();
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/getHAR", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE})
    public @ResponseBody String getHAR(
            @RequestParam(value = "ClientName", required = true) String ClientName,
            @RequestParam(value = "ProjectName", required = true) String ProjectName,
            @RequestParam(value = "Scenario", required = true) String Scenario,
            @RequestParam(value = "AnalysisType", required = true) String analysisType,
            @RequestParam(value = "RunID",required = false) String RunID,
            @RequestParam(value = "BaselineStart",required = false) String BaselineStart,
            @RequestParam(value = "BaselineEnd",required = false) String BaselineEnd,
            @RequestParam(value = "CurrentStart",required = false) String CurrentStart,
            @RequestParam(value = "CurrentEnd",required = false) String CurrentEnd,
            @RequestParam(value = "SampleValue", required = false) String sampleValue,
            @RequestParam(value = "TransactionName", required = true) String transactionName )
            throws Exception
    {
        long overallStart=System.currentTimeMillis();

        LOGGER.info("Received getHAR Request for Client: {}, Project: {}, Scenario: {} TransactionName {}",ClientName,ProjectName,Scenario,transactionName);
        String response = null;
        boolean flag = true;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            flag = false;
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {

                if(analysisType.trim().equals("Run") || analysisType.trim().equals("Transaction") || analysisType.trim().equals("Time"))
                {
                    validateResult.put("AnalysisType",analysisType.trim());
                    if (analysisType.trim().equals("Run") || analysisType.trim().equals("Transaction"))
                    {
                        if (RunID == null || RunID.trim().equals("") || RunID.trim().matches("[0-9]+"))
                        {
                            validateResult.put("CurrentRun", ((RunID == null || RunID.trim().equals("")) ? null : RunID));
                        }
                        else
                        {
                            flag = false;
                            LOGGER.error("RunID can allow only numbers or empty value {}", RunID);
                            appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.RUNID_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.RUNID_ERROR_MSG);
                        }
                    }
                    else
                    {
                        if (CurrentStart.trim().equals("") || CurrentEnd.trim().equals(""))
                        {
                            flag = false;
                            LOGGER.error(GlobalConstants.TIME_ERROR_MSG);
                            appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.TIME_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.TIME_ERROR_MSG);
                        }
                        else
                        {
                            validateResult.put("CurrentStart", CurrentStart);
                            validateResult.put("CurrentEnd", CurrentEnd);
                        }

                        if (BaselineStart.trim().equals("") || BaselineEnd.trim().equals(""))
                        {
                            flag = false;
                            LOGGER.error(GlobalConstants.TIME_ERROR_MSG);
                            appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.TIME_ERROR_MSG,"",0,"",0,0,0,0);
                            validateResult.put("Status", "Failed");
                            validateResult.put("Reason", GlobalConstants.TIME_ERROR_MSG);
                        }
                        else
                        {
                            validateResult.put("BaselineStart", BaselineStart);
                            validateResult.put("BaselineEnd", BaselineEnd);
                        }
                    }

                    if(transactionName.trim().equals(""))
                    {
                        flag = false;
                        LOGGER.error("Transaction Name {} cannot be null or blank",transactionName);
                        appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.TRANS_ERROR_MSG,"",0,"",0,0,0,0);
                        validateResult.put("Status", "Failed");
                        validateResult.put("Reason", GlobalConstants.TRANS_ERROR_MSG);
                    }
                    else
                    {
                        validateResult.put("txnName",transactionName.trim());
                    }
                }
                else
                {
                    flag = false;
                    LOGGER.error("Invalid analysis type {}", analysisType.trim());
                    appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.ANALYSISTYPE_ERROR_MSG,"",0,"",0,0,0,0);
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.ANALYSISTYPE_ERROR_MSG);
                }

            }
            else
            {
                flag = false;
                LOGGER.error("Invalid Subscription {}",ClientName);
                appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }
        long overallEnd = System.currentTimeMillis();
        if(flag)
        {
            validateResult.put("esUrl",baseUrl);
            if(sampleValue != null)
            {
                validateResult.put("currentSample", (sampleValue.trim().equals("") ? null : sampleValue));
            }

            List<Object> harData = PaceAnalysisEngine.getHar(validateResult);
            if(harData.get(0).toString().equals("false"))
            {
                flag = false;
                appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                LOGGER.debug("getHAR - Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {}", ClientName, ProjectName, Scenario);
                validateResult.put("ClientName", appUtils.normaliseInput(ClientName));
                validateResult.put("ProjectName", appUtils.normaliseInput(ProjectName));
                validateResult.put("Scenario", appUtils.normaliseInput(Scenario));
                validateResult.remove("esUrl");
                validateResult.put("Status", "Failed");
                validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
            }
            else
            {
                appUtils.asyncAuditLog(overallStart,"API","getHAR",ClientName,ProjectName,Scenario,"S","",analysisType,0,"",0,0,0,0);
                response = harData.get(1).toString();
            }
        }

        overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getHAR Request for Client: {}, Project: {}, Scenario: {} TransactionName {} and took {} ms",ClientName,ProjectName,Scenario,transactionName,(overallEnd-overallStart));
        if(flag)
        {
            return response;
        }
        else
        {
            return new JSONObject(validateResult).toString();
        }
    }




    @RequestMapping(method = RequestMethod.GET, value = "/getConfig", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getConfig(
            @RequestParam("ProjectName") String ProjectName,
            @RequestParam("ClientName") String ClientName,
            @RequestParam("Scenario") String Scenario,
            @RequestParam(value = "updateRunID", required = false) Boolean updateRunID)
            throws Exception
    {
        long overallStart=System.currentTimeMillis();

        LOGGER.info("Received getConfig Request for Client: {}, Project: {}, Scenario: {}",ClientName,ProjectName,Scenario);
        Map<String,Object> config = new HashMap();
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getConfig",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {
                String option = "false";
                if(updateRunID == null || updateRunID == true)
                {
                    if(updateRunID == null)
                    {
                        option = "time";
                    }
                    else
                    {
                        option = "true";
                    }
                }

                validateResult.put("updateRunID",option);
                validateResult.put("esUrl",baseUrl);
                config = PaceAnalysisEngine.getConfiguration(validateResult);
                if(config == null || config.size() == 0)
                {
                    appUtils.asyncAuditLog(overallStart,"API","getConfig",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                    LOGGER.debug("Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",ClientName,ProjectName,Scenario);
                    validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                    validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
                    validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
                    validateResult.remove("esUrl");
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                }

            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","getConfig",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getConfig Request for Client: {}, Project: {}, Scenario: {} and took {} ms",ClientName,ProjectName,Scenario,(overallEnd-overallStart));
        if(config == null || config.size() == 0)
        {
            return new JSONObject(validateResult).toString();
        }
        else
        {
            appUtils.asyncAuditLog(overallStart,"API","getConfig",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
            return new JSONObject(config).toString();
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/createConfig", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String createConfig(@RequestParam("ProjectName") String ProjectName,
                                             @RequestParam("ClientName") String ClientName,
                                             @RequestParam("Scenario") String Scenario,
                                             @RequestParam(value = "isNativeApp", required = false) Boolean isNativeApp,
                                             @RequestParam(value = "isLoadTest", required = false) Boolean isLoadTest,
                                             @RequestParam(value = "isMarkEnabled", required = false) Boolean isMarkEnabled) throws Exception
    {

        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received createConfig Request for Client: {}, Project: {}, Scenario: {}",ClientName,ProjectName,Scenario);

        Map<String,Object> config = new HashMap();
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);
        LOGGER.debug("Validated CreateConfig Request {}",validateResult.toString());
        if(validateResult.containsKey("Status"))
        {
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","createConfig",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {
                validateResult.put("ClientNameOrg",ClientName);
                validateResult.put("ProjectNameOrg",ProjectName);
                validateResult.put("ScenarioOrg",Scenario);
                validateResult.put("esUrl",baseUrl);
                String kibanaDashboardUrl = null;
                if(isNativeApp == null || isNativeApp == false)
                {
                    isNativeApp = false;
                    kibanaDashboardUrl = dashboardLink.replace(dashboardPlaceholder, dashboardName).replace(projectPlaceholder,validateResult.get("ProjectName")).replace(clientPlaceholder,validateResult.get("ClientName")).replace(scenarioPlaceholder,validateResult.get("Scenario"));
                }
                else
                {
                    //kibanaDashboardUrl = nativeDashboardLink.replace(dashboardPlaceholder, nativeDashboardName).replace(projectPlaceholder,validateResult.get("ProjectName")).replace(clientPlaceholder,validateResult.get("ClientName")).replace(scenarioPlaceholder,validateResult.get("Scenario"));

                }
                validateResult.put("kibanaUrl",kibanaDashboardUrl);
                validateResult.put("defaultConfig",defaultConfig);
                if(isLoadTest == null)
                {
                    isLoadTest = false;
                }
                if(isMarkEnabled == null)
                {
                    isMarkEnabled = false;
                }
                validateResult.put("isNativeApp",isNativeApp.toString());
                validateResult.put("isLoadTest",isLoadTest.toString());
                validateResult.put("isMarkEnabled",isMarkEnabled.toString());

                LOGGER.debug("Create Config Request {}",validateResult.toString());
                config = PaceAnalysisEngine.createConfiguration(validateResult);
                if(config == null || config.size() == 0 || config.containsKey("Status"))
                {

                    validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                    validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
                    validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
                    validateResult.remove("esUrl");
                    validateResult.remove("defaultConfig");
                    validateResult.remove("ClientNameOrg");
                    validateResult.remove("ProjectNameOrg");
                    validateResult.remove("ScenarioOrg");
                    validateResult.remove("isNativeApp");
                    validateResult.remove("kibanaUrl");
                    validateResult.remove("isLoadTest");
                    validateResult.remove("isMarkEnabled");
                    validateResult.put("Status", "Failed");
                    if(config == null || config.size() == 0)
                    {
                        validateResult.put("Reason", "Failed to create config in the system");
                        appUtils.asyncAuditLog(overallStart,"API","createConfig",ClientName,ProjectName,Scenario,"F","Failed to create config in the system","",0,"",0,0,0,0);
                        LOGGER.debug("Failed to create config for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",ClientName,ProjectName,Scenario);
                    }
                    else
                    {
                        validateResult.put("Reason", "Already config exists for given combination");
                        appUtils.asyncAuditLog(overallStart,"API","createConfig",ClientName,ProjectName,Scenario,"F","Config Already Exists","",0,"",0,0,0,0);
                        LOGGER.debug("Config Exists for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",ClientName,ProjectName,Scenario);
                    }
                }

            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","createConfig",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed createConfig Request for Client: {}, Project: {}, Scenario: {} and took {} ms",ClientName,ProjectName,Scenario,(overallEnd-overallStart));

        if(config == null || config.size() == 0 || config.containsKey("Status"))
        {
            return new JSONObject(validateResult).toString();
        }
        else
        {
            appUtils.asyncAuditLog(overallStart,"API","createConfig",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
            return new JSONObject(config).toString();
        }

    }

    @RequestMapping(method = RequestMethod.POST, value = "/updateConfig", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String updateConfig(@RequestBody String confRequest) throws Exception
    {

        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received updateConfig Request");
        LOGGER.debug("updateConfig Request {}",confRequest);

        confRequest=confRequest.replace("\\\"", "\"");
        confRequest=confRequest.replace("\"{", "{");
        confRequest=confRequest.replace("}\"", "}");

        JSONObject configRequest = new JSONObject(confRequest);


        Map<String,String> config = new HashMap();
        Map<String,String> validateResult = appUtils.validateInputFields(configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"));
        if(validateResult.containsKey("Status"))
        {
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","updateConfig",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"),"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid = subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {
                config = PaceAnalysisEngine.updateConfiguration(configRequest,baseUrl);
                if(config == null || config.size() == 0 )
                {
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                    appUtils.asyncAuditLog(overallStart,"API","updateConfig",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"),"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                    LOGGER.debug("No config available to update for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"));
                }
                else
                {
                    if(config.containsKey("Status") && config.get("Status").equals("0"))
                    {
                        validateResult.put("Status", "Success");
                        if(config.containsKey("Comment")) {
                            validateResult.put("Comment", config.get("Comment"));
                        }
                        appUtils.asyncAuditLog(overallStart,"API","updateConfig",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"),"S","","",0,"",0,0,0,0);
                        LOGGER.debug("Config update success for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"));

                    }
                    else
                    {
                        validateResult.put("Status", "Failed");
                        validateResult.put("Reason", "Failed to update data source");
                        appUtils.asyncAuditLog(overallStart,"API","updateConfig",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"),"F","Failed to create config in the system","",0,"",0,0,0,0);
                        LOGGER.debug("Failed to update for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"));

                    }
                }

            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","updateConfig",configRequest.getString("ClientName"),configRequest.getString("ProjectName"),configRequest.getString("Scenario"),"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",configRequest.getString("ClientName"));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);

            }
        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed updateConfig Request and took " +(overallEnd-overallStart)+ " ms");

        return new JSONObject(validateResult).toString();

    }



    @RequestMapping(method = RequestMethod.GET, value = "/checkHealth", produces = MediaType.APPLICATION_JSON_VALUE)
    public String checkHealth() throws Exception
    {
        long overallStart=System.currentTimeMillis();

        Map<String,String> result = new HashMap<>();

        result.put("API","UP");
        result.put("Elastic Search",PaceAnalysisEngine.checkHealth(baseUrl,"_cluster/health"));
        result.put("Kibana",PaceAnalysisEngine.checkHealth(kibanaUrl,"/status"));
        if(geoIpFlag)
        {
            result.put("Location Service",PaceAnalysisEngine.checkHealth(locationService,"/json/localhost"));
        }
        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed checkHealth Request and took " +(overallEnd-overallStart)+ " ms");
        appUtils.asyncAuditLog(overallStart,"API","checkHealth","NA","NA","NA","S","","",0,"",0,0,0,0);


        return new JSONObject(result).toString();
    }


    @RequestMapping(method = RequestMethod.GET, value = "/getClientList", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getClientList() throws Exception
    {
        long overallStart=System.currentTimeMillis();
        LOGGER.info("Received getClientList Request");
        String result = PaceAnalysisEngine.getClientList(baseUrl).toString();
        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getClientList Request and took " +(overallEnd-overallStart)+ " ms");
        appUtils.asyncAuditLog(overallStart,"API","getClientList","NA","NA","NA","S","","",0,"",0,0,0,0);

        return result;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/getTransactionsSLA", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getTransactionsSLA(
            @RequestParam("ProjectName") String ProjectName,
            @RequestParam("ClientName") String ClientName,
            @RequestParam("Scenario") String Scenario)
            throws Exception
    {
        long overallStart=System.currentTimeMillis();

        LOGGER.info("Received getTransactionsSLA Request for Client: {}, Project: {}, Scenario: {}",ClientName,ProjectName,Scenario);
        List<Object> result = null;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","getTransactionsSLA",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {
                validateResult.put("esUrl",baseUrl);
                result = PaceAnalysisEngine.getTransactionDetails(validateResult);
                if(result.get(1).toString().equals("false"))
                {
                    appUtils.asyncAuditLog(overallStart,"API","getTransactionsSLA",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                    LOGGER.debug("Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",ClientName,ProjectName,Scenario);
                    validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                    validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
                    validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
                    validateResult.remove("esUrl");
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                }

            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","getTransactionsSLA",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed getTransactionsSLA Request for Client: {}, Project: {}, Scenario: {} and took {} ms",ClientName,ProjectName,Scenario,(overallEnd-overallStart));
        if(result.get(1).toString().equals("false"))
        {
            return new JSONObject(validateResult).toString();
        }
        else
        {
            appUtils.asyncAuditLog(overallStart,"API","getTransactionsSLA",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
            return new JSONObject(result.get(0).toString()).toString();
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/deleteStats", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String deleteStats(
            @RequestParam("ProjectName") String ProjectName,
            @RequestParam("ClientName") String ClientName,
            @RequestParam("Scenario") String Scenario,
            @RequestParam(value = "RunID", required = false) String RunID)
            throws Exception
    {
        long overallStart=System.currentTimeMillis();

        LOGGER.info("Received deleteStats Request for Client: {}, Project: {}, Scenario: {}",ClientName,ProjectName,Scenario);
        List<Object> result = null;
        Map<String,String> validateResult = appUtils.validateInputFields(ClientName,ProjectName,Scenario);

        if(validateResult.containsKey("Status"))
        {
            LOGGER.error(GlobalConstants.CPS_ERROR_MSG);
            appUtils.asyncAuditLog(overallStart,"API","deleteStats",ClientName,ProjectName,Scenario,"F",GlobalConstants.CPS_ERROR_MSG,"",0,"",0,0,0,0);
            validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
            validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
            validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
            validateResult.put("Status","Failed");
            validateResult.put("Reason",GlobalConstants.CPS_ERROR_MSG);
        }
        else
        {
            boolean isSubscriptionValid=subscriptionValidator.validateSubscription(validateResult.get("ClientName"));
            if(isSubscriptionValid)
            {
                validateResult.put("esUrl",baseUrl);
                if(RunID != null)
                {
                    validateResult.put("RunID",RunID);
                }

                result = PaceAnalysisEngine.deleteStats(validateResult);
                if(result.get(1).toString().equals("false"))
                {
                    appUtils.asyncAuditLog(overallStart,"API","deleteStats",ClientName,ProjectName,Scenario,"F",GlobalConstants.CONFIG_ERROR_MSG,"",0,"",0,0,0,0);
                    LOGGER.debug("Config file does not exist for ClientName: {}, ProjectName: {}, Scenario: {} Combination.",ClientName,ProjectName,Scenario);
                    validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                    validateResult.put("ProjectName",appUtils.normaliseInput(ProjectName));
                    validateResult.put("Scenario",appUtils.normaliseInput(Scenario));
                    validateResult.remove("esUrl");
                    validateResult.put("Status", "Failed");
                    validateResult.put("Reason", GlobalConstants.CONFIG_ERROR_MSG);
                }

            }
            else
            {
                LOGGER.error(GlobalConstants.SUBC_ERROR_MSG);
                appUtils.asyncAuditLog(overallStart,"API","deleteStats",ClientName,ProjectName,Scenario,"F",GlobalConstants.SUBC_ERROR_MSG,"",0,"",0,0,0,0);
                validateResult.put("ClientName",appUtils.normaliseInput(ClientName));
                validateResult.put("Status","Failed");
                validateResult.put("Reason",GlobalConstants.SUBC_ERROR_MSG);
            }

        }

        long overallEnd=System.currentTimeMillis();
        LOGGER.info("Completed deleteStats Request for Client: {}, Project: {}, Scenario: {} and took {} ms",ClientName,ProjectName,Scenario,(overallEnd-overallStart));
        if(result.get(1).toString().equals("false"))
        {
            return new JSONObject(validateResult).toString();
        }
        else
        {
            appUtils.asyncAuditLog(overallStart,"API","deleteStats",ClientName,ProjectName,Scenario,"S","","",0,"",0,0,0,0);
            return new JSONObject(result.get(0).toString()).toString();
        }
    }
}