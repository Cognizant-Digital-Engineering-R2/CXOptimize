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
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CXOptimizeServiceImpl implements CXOptimizeService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CXOptimizeServiceImpl.class);

    public String getAuthToken()
    {
        LOGGER.debug("Entering  getAuthToken");
        String url = CollectorConstants.getBeaconURL() + "/authToken";
        String body = "{\"username\":\"" + CollectorConstants.getUserName() + "\",\"password\":\"" + CollectorConstants.getPassword() + "\"}";
        return HttpUtils.callService(url, "POST", body, null).get("authToken").toString();

    }

    public Map<String, Object> getConfiguration()
    {
        LOGGER.debug("Entering  getConfiguration");
        LOGGER.debug("Get Configuration {}",CollectorConstants.getBeaconURL());
        LOGGER.debug("Get Configuration {}",CollectorConstants.getClientName());
        LOGGER.debug("Get Configuration {}",CollectorConstants.getProjectName());
        LOGGER.debug("Get Configuration {}",CollectorConstants.getScenarioName());
        String url = CollectorConstants.getBeaconURL() + "/getConfig?ClientName=" + CollectorConstants.getClientName() + "&ProjectName=" + CollectorConstants.getProjectName() + "&Scenario=" + CollectorConstants.getScenarioName();
        if (CollectorConstants.getLoadTest().equals("true")) {
            url = url + "&isLoadTest=true";
        }
        //Map<String, Object> result = new HashMap<String, Object>();
        //result = HttpUtils.callService(url,"GET",null,ConfigurationLoader.getApiToken());
        try {
            JSONObject jsonObj = new JSONObject(HttpUtils.callService(url, "GET", null, CollectorConstants.getApiToken()).get("response").toString());
            return JsonUtils.toMap(jsonObj);
        } catch (Exception e) {
            LOGGER.error("Exception in parsing configuration : {}",e);
            return null;
        }

    }

    public String uploadPerformanceData(String body) {
        String url = CollectorConstants.getBeaconURL() + "/insertStats";
        Map<String, Object> result = HttpUtils.callService(url, "POST", body, CollectorConstants.getApiToken());
        if (result.get("status").equals("pass")) {
            LOGGER.debug("Data uploaded successfully : {}", result.get("response").toString());
            return result.get("response").toString();
        } else {
            if (result.get("reason").equals("JWTExpiry")) {
                LOGGER.debug("JWT Token expired.Retrying again");
                CollectorConstants.setApiToken(getAuthToken());
                LOGGER.debug("New JWT Token set : {}", CollectorConstants.getApiToken());
                result = HttpUtils.callService(url, "POST", body, CollectorConstants.getApiToken());
                if (result.get("status").equals("pass")) {
                    LOGGER.debug("Data uploaded successfully for retry : {}", result.get("response").toString());
                    return result.get("response").toString();
                } else {
                    LOGGER.debug("Data uploaded failed for retry");
                    return null;
                }
            } else {
                LOGGER.debug("Data uploaded failed for first try");
                return null;
            }
        }

    }

}
