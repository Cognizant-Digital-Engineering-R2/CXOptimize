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

using com.cognizant.pace.CXOptimize.Collector.constant;
using com.cognizant.pace.CXOptimize.Collector.utils;
using System;
using System.Collections.Generic;

namespace com.cognizant.pace.CXOptimize.Collector.service
{
    public class CXOptimizeServiceImpl : ICXOptimizeService
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        public string getAuthToken()
        {
            log.Debug("Entering  getAuthToken");
            String url = CollectorConstants.getBeaconURL() + "/authToken";
            String body = "{\"username\":\"" + CollectorConstants.getUserName() + "\",\"password\":\"" + CollectorConstants.getPassword() + "\"}";
            return HTTPUtils.callService(url, "POST", body, null)["authToken"].ToString();

        }

        public Dictionary<string, object> getConfiguration()
        {
            log.Debug("Entering  getConfiguration");
            log.Debug("Get Configuration " + CollectorConstants.getBeaconURL());
            log.Debug("Get Configuration " + CollectorConstants.getClientName());
            log.Debug("Get Configuration " + CollectorConstants.getProjectName());
            log.Debug("Get Configuration " + CollectorConstants.getScenarioName());
            String url = CollectorConstants.getBeaconURL() + "/getConfig?ClientName=" + CollectorConstants.getClientName() + "&ProjectName=" + CollectorConstants.getProjectName() + "&Scenario=" + CollectorConstants.getScenarioName() + "&updateRunID=true";
            if (CollectorConstants.getLoadTest().Equals("true"))
            {
                url = url + "&isLoadTest=true";
            }
            //Map<String, Object> result = new HashMap<String, Object>();
            //result = HttpUtils.callService(url,"GET",null,ConfigurationLoader.getApiToken());
            try
            {
                Dictionary<String, Object> results = HTTPUtils.callService(url, "GET", null, CollectorConstants.getApiToken());
                Dictionary<String, Object> jsonObj = JSONUtils.JsonStringToMap(results["response"].ToString());
                return jsonObj;
                
            }
            catch (Exception e)
            {
                log.Error("Exception in parsing configuration : {}", e);
                return null;
            }
        }

        public string uploadPerformanceData(string body)
        {
            String url = CollectorConstants.getBeaconURL() + "/insertStats";
            Dictionary<String, Object> result = HTTPUtils.callService(url, "POST", body, CollectorConstants.getApiToken());
            if (result.ContainsKey("status") && result["status"].Equals("pass"))
            {
                log.Debug("Data uploaded successfully : {}" + result["response"]);
                return result["response"].ToString();
            }
            else {
                if (result["reason"].Equals("JWTExpiry"))
                {
                    log.Debug("JWT Token expired.Retrying again");
                    CollectorConstants.setApiToken(getAuthToken());
                    log.Debug("New JWT Token set : " + CollectorConstants.getApiToken());
                    result = HTTPUtils.callService(url, "POST", body, CollectorConstants.getApiToken());
                    if (result["status"].Equals("pass"))
                    {
                        log.Debug("Data uploaded successfully for retry : {}" + result["response"].ToString());
                        return result["response"].ToString();
                    }
                    else {
                        log.Debug("Data uploaded failed for retry");
                        return null;
                    }
                }
                else {
                    log.Debug("Data uploaded failed for first try");
                    return null;
                }
            }
        }
    }
}
