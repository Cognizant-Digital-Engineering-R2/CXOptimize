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

package com.cognizant.pace.CXOptimize.Collector.service;

import com.cognizant.pace.CXOptimize.Collector.constant.CollectorConstants;
import com.cognizant.pace.CXOptimize.Collector.utils.HttpUtils;
import com.cognizant.pace.CXOptimize.Collector.utils.JsonUtils;
import com.cognizant.pace.CXOptimize.Collector.utils.ValidationUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class CXOptimizeServiceImpl implements CXOptimizeService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CXOptimizeServiceImpl.class);

    public boolean getAuthToken()
    {
        LOGGER.debug("CXOP - Entering  getAuthToken");
        String url = CollectorConstants.getBeaconURL() + "/authToken";
        String body = "{\"username\":\"" + CollectorConstants.getUserName() + "\",\"password\":\"" + CollectorConstants.getPassword() + "\"}";
        boolean isValidToken = false;
        try {
            isValidToken = ValidationUtils.validateAuthToken(HttpUtils.callService(url, "POST", body, null).get("authToken").toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return isValidToken;

    }

    public Map<String, Object> getConfiguration()
    {
        LOGGER.debug("CXOP - Entering  getConfiguration");
        LOGGER.debug("CXOP - Get Configuration {}",CollectorConstants.getBeaconURL());
        LOGGER.debug("CXOP - Get Configuration {}",CollectorConstants.getClientName());
        LOGGER.debug("CXOP - Get Configuration {}",CollectorConstants.getProjectName());
        LOGGER.debug("CXOP - Get Configuration {}",CollectorConstants.getScenarioName());
        String url = CollectorConstants.getBeaconURL() + "/getConfig?ClientName=" + CollectorConstants.getClientName() + "&ProjectName=" + CollectorConstants.getProjectName() + "&Scenario=" + CollectorConstants.getScenarioName();
        if (CollectorConstants.getLoadTest().equals("true")) {
            url = url + "&isLoadTest=true";
        }

        try {
            JSONObject jsonObj = new JSONObject(HttpUtils.callService(url, "GET", null, CollectorConstants.getApiToken()).get("response").toString());
            if(CollectorConstants.getClientName().toLowerCase().equals(jsonObj.getString("ClientName")) || CollectorConstants.getProjectName().toLowerCase().equals(jsonObj.getString("ProjectName")) || CollectorConstants.getScenarioName().toLowerCase().equals(jsonObj.getString("Scenario")))
            {
                return JsonUtils.toMap(jsonObj);
            }else{
                LOGGER.debug("CXOP - Wrong Configuration from the server");
                return null;
            }

        } catch (Exception e) {
            LOGGER.error("CXOP - Exception in parsing configuration : {}",e);
            return null;
        }

    }

    public String uploadPerformanceData(String body) {
        String url = CollectorConstants.getBeaconURL() + "/insertStats";
        if((System.currentTimeMillis() - CollectorConstants.getTokenStartTime()) > (CollectorConstants.getTokenExpiry())){
            getAuthToken();
        }
        Map<String, Object> result = HttpUtils.callService(url, "POST", body, CollectorConstants.getApiToken());
        if (result.get("status").equals("pass")) {
            return result.get("response").toString();
        } else {
            LOGGER.debug("CXOP - Data uploaded failed");
            return null;
        }

    }

}
