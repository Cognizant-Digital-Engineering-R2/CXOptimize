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
import org.apache.commons.validator.routines.UrlValidator;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONArray;
import java.util.*;


@Component
public class AppUtils
{
    @Value("${auditlog.source}")
    private String auditSource;

    @Autowired
    private HTTPUtils httpUtils;

    private Logger AUDITLOGGER = LoggerFactory.getLogger("audit");

    public String RemoveSpecialChars(String name)
    {
        return name.toLowerCase().replaceAll("[\\/\\?\\.\\;\\'\\\"\\>\\<\\:\\,\\@\\!\\#\\$\\%\\^\\&\\*\\-\\=\\+\\(\\)\\[\\]\\{\\\\\\|}]","");
    }

    public String normaliseInput(String name)
    {
        return name.replaceAll("[\\/\\?\\.\\;\\'\\\"\\>\\<\\:\\,\\@\\!\\#\\$\\%\\^\\&\\*\\-\\=\\+\\(\\)\\[\\]\\{\\\\\\|}]","");
    }

    public Map<String,String> validateInputFields(String clientName,String projName,String scenario)
    {
        Map<String,String> retValue = new HashMap<>();
        retValue.put(GlobalConstants.CLIENTNAME,RemoveSpecialChars(clientName).trim());
        retValue.put(GlobalConstants.PROJECTNAME,RemoveSpecialChars(projName).trim());
        retValue.put(GlobalConstants.SCENARIO,RemoveSpecialChars(scenario).trim());
        if(retValue.get(GlobalConstants.CLIENTNAME).equals("") || retValue.get(GlobalConstants.PROJECTNAME).equals("") || retValue.get(GlobalConstants.SCENARIO).equals(""))
        {
            retValue.put("Status","1");
        }
        return retValue;
    }

    public Map<String, Double> calculateBackendTime(JSONArray resourceDetailsOrg, String NavType) throws JSONException {

        ArrayList<Map<String, Double>> resourceDetails = new ArrayList<>();
        ArrayList<Map<String, Double>> resourceDetails1 = new ArrayList<>();
        HashMap<String, Double> newmap1 = new HashMap<>();
        Map<String, Double> result = new HashMap<>();
        for (int i=0;i<resourceDetailsOrg.length();i++){
            JSONObject res = resourceDetailsOrg.getJSONObject(i);
            newmap1.put("fetchStart", Double.parseDouble(res.getString("fetchStart")));
            newmap1.put("responseEnd", Double.parseDouble(res.getString("responseEnd")));
            resourceDetails.add((HashMap) newmap1.clone());
        }

        // First, sort by start time, then end time

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


        int size = resourceDetails.size();
        // Next, find all resources with the same start time, and reduce
        // them to the largest end time.
        for (int i = 0; i < size; i++) {

            if (i != (size - 1) && Objects.equals(resourceDetails.get(i).get("fetchStart"), resourceDetails.get(i + 1).get("fetchStart"))) {
            } else {
                resourceDetails1.add(resourceDetails.get(i));
            }
        }



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


        if (Boolean.valueOf(NavType))
        {
            result.put("totalTime", responseEnd);

        }
        else
        {
            result.put("totalTime", (responseEnd - fetchStart));
        }

        result.put("backendTime", backendTime);
        return result;
    }

    @Async("auditLogExecutor")
    public void asyncAuditLog(long startTime,String logType,String apiName,String cName,String pName,String sName,String status,String msg,String anaType,int runID,String txnName,int txnsAnalyzed,long duration,int recommCnt,int score)
    {

        if(auditSource.equals("ES")) {
            try {
                JSONObject logRecord = new JSONObject();
                DateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSSZ");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));

                logRecord.put("StartTime", format.format(new java.util.Date(startTime)));
                logRecord.put("LogType", logType);
                logRecord.put("API", apiName);
                logRecord.put("ClientName", cName);
                logRecord.put("ProjectName", pName);
                logRecord.put("Scenario", sName);
                logRecord.put("Status", status);
                logRecord.put("ErrorMsg", msg);
                logRecord.put("AnalysisType", anaType);
                logRecord.put("RunID", runID);
                logRecord.put("TransactionName", txnName);
                logRecord.put("TransactionsAnalyzed", txnsAnalyzed);
                logRecord.put("TimeTaken", duration);
                logRecord.put("RecommendationCount", recommCnt);
                logRecord.put("Score", score);

                httpUtils.httpPost(logRecord.toString(), GlobalConstants.getESUrl() + "/" + GlobalConstants.AUDITINDEX_INSERT);


            } catch (Exception e) {

            }
        }
        else
        {
            AUDITLOGGER.info("LogType:{}, API: {}, ClientName: {}, Project:{}, Scenario:{}, Status: {}, ErrorMsg:{}, AnalysisType:{}, RunID:{},TransactionName:{},TransactionsAnalysed:{}, TimeTaken:{}, RecommendationCount:{}, Score:{}",logType,apiName,cName,pName,sName,status,msg,anaType,runID,txnName,txnsAnalyzed,duration,recommCnt,score);
        }

    }

    public static String getHostName(String url,UrlValidator urlValidator)
    {
        if(url != null && url != "")
        {
            String truncUrl = url.toLowerCase().split("\\?")[0];

            if (urlValidator.isValid(truncUrl))
            {
                try
                {
                    return new URL(truncUrl).getHost();
                }
                catch(Exception e)
                {
                    return (truncUrl.split("//")[1]).split("/")[0];
                }
            }
            else
            {
                return (truncUrl.split("//")[1]).split("/")[0];
            }

        }
        else
        {
            return "NA";
        }

    }


}
