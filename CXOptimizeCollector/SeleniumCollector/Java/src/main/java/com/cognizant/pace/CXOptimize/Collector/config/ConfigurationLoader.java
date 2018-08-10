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

package com.cognizant.pace.CXOptimize.Collector.config;

import com.cognizant.pace.CXOptimize.Collector.service.CXOptimizeService;
import com.cognizant.pace.CXOptimize.Collector.service.CXOptimizeServiceImpl;
import com.cognizant.pace.CXOptimize.Collector.constant.CollectorConstants;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;


public class ConfigurationLoader {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ConfigurationLoader.class);
    private static final Object mutex = new Object();
    private static ConfigurationLoader instance = null;
    public Map<String, Object> clientConfig = null;


    protected ConfigurationLoader() {

        try {
            LOGGER.debug("CXOP - Starting Loading Configuration");
            Properties mainProperties;
            mainProperties = new Properties();
            FileInputStream file;
            Map<String, Object> config;
            String licenseKey;
            String beaconURL;
            String authToken;
            String path = CollectorConstants.getCollectorProperties();

            //To integrate with Cognizant Intelligent Scripter
            if(path == null)
            {
                path = System.getProperty("user.dir");
                path = path + File.separator + "collectordependency" + File.separator + "Collector.properties";
            }
            else
            {
                path =  path + File.separator + "Collector.properties";
            }

            LOGGER.debug("CXOP - Checking if the properties file exists in {}", path);
            File propFile = new File(path);
            boolean exists = propFile.exists();
            LOGGER.debug("CXOP - Properties file exists : {}", exists);

            if (exists) {
                file = new FileInputStream(path);
                mainProperties.load(file);
                LOGGER.debug("CXOP - Properties Load Successfull from file : {}", mainProperties.toString());
                CollectorConstants.setBeaconURL(mainProperties.getProperty("beaconUrl"));
                CollectorConstants.setClientName(mainProperties.getProperty("clientName"));
                CollectorConstants.setProjectName(mainProperties.getProperty("projectName"));
                CollectorConstants.setScenarioName(mainProperties.getProperty("scenarioName"));
                CollectorConstants.setLicenseKey(mainProperties.getProperty("LicenseKey"));
                CollectorConstants.setUserName(mainProperties.getProperty("UserName"));
                CollectorConstants.setPassword(mainProperties.getProperty("Password"));

                if (mainProperties.containsKey("isLoadTest") && mainProperties.getProperty("isLoadTest").equals("true")) {
                    CollectorConstants.setLoadTest("true");
                }
                else
                {
                    CollectorConstants.setLoadTest("false");
                }
                if (mainProperties.containsKey("markWaitTime")) {
                    CollectorConstants.setMarkWaitTime(Integer.parseInt(mainProperties.getProperty("markWaitTime").toString()));
                } else {
                    CollectorConstants.setMarkWaitTime(5000);
                }
                if (mainProperties.containsKey("resourceSettleTime")) {
                    CollectorConstants.setResourceSettleTime(Integer.parseInt(mainProperties.getProperty("resourceSettleTime").toString()));
                } else {
                    CollectorConstants.setResourceSettleTime(2000);
                }
                if (mainProperties.containsKey("manualResourceTimeClear")) {
                    CollectorConstants.setManualResourceTimeClear(mainProperties.getProperty("manualResourceTimeClear"));
                } else {
                    CollectorConstants.setManualResourceTimeClear("false");
                }
                LOGGER.debug("CXOP - Loaded initialization parameters");
                file.close();
            } else {
                mainProperties.load(ConfigurationLoader.class.getResourceAsStream("/collectordependency/Collector.properties"));
                if (mainProperties.getProperty("clientName") != null) {
                    LOGGER.debug("CXOP - Properties Load Successfull from package : {}", mainProperties.toString());
                    CollectorConstants.setBeaconURL(mainProperties.getProperty("beaconUrl"));
                    CollectorConstants.setClientName(mainProperties.getProperty("clientName"));
                    CollectorConstants.setProjectName(mainProperties.getProperty("projectName"));
                    CollectorConstants.setScenarioName(mainProperties.getProperty("scenarioName"));
                    CollectorConstants.setLicenseKey(mainProperties.getProperty("LicenseKey"));
                    CollectorConstants.setUserName(mainProperties.getProperty("UserName"));
                    CollectorConstants.setPassword(mainProperties.getProperty("Password"));

                    if (mainProperties.containsKey("isLoadTest") && mainProperties.getProperty("isLoadTest").equals("true")) {
                        CollectorConstants.setLoadTest("true");
                    }
                    else
                    {
                        CollectorConstants.setLoadTest("false");
                    }
                    if (mainProperties.containsKey("markWaitTime")) {
                        CollectorConstants.setMarkWaitTime(Integer.parseInt(mainProperties.getProperty("markWaitTime").toString()));
                    } else {
                        CollectorConstants.setMarkWaitTime(5000);
                    }

                    if (mainProperties.containsKey("resourceSettleTime")) {
                        CollectorConstants.setResourceSettleTime(Integer.parseInt(mainProperties.getProperty("resourceSettleTime").toString()));
                    } else {
                        CollectorConstants.setResourceSettleTime(2000);
                    }

                    if (mainProperties.containsKey("manualResourceTimeClear")) {
                        CollectorConstants.setManualResourceTimeClear(mainProperties.getProperty("manualResourceTimeClear"));
                    } else {
                        CollectorConstants.setManualResourceTimeClear("false");
                    }

                    LOGGER.debug("CXOP - Loaded initialization parameters");
                } else {
                    LOGGER.debug("CXOP - Properties loaded from environment variables");
                }
            }

            if (CollectorConstants.getBeaconURL() == null || CollectorConstants.getBeaconURL() == "") {
                throw new InvalidParameterException("Missing value for BeaconURL");
            }
            if (CollectorConstants.getClientName() == null || CollectorConstants.getClientName() == "") {
                throw new InvalidParameterException("Missing value for ClientName");
            }
            if (CollectorConstants.getProjectName() == null || CollectorConstants.getProjectName() == "") {
                throw new InvalidParameterException("Missing value for ProjectName");
            }
            if (CollectorConstants.getScenarioName() == null || CollectorConstants.getScenarioName() == "") {
                throw new InvalidParameterException("Missing value for ScenarioName");
            }
            if (CollectorConstants.getUserName() == null || CollectorConstants.getUserName() == "") {
                throw new InvalidParameterException("Missing value for UserName");
            }
            if (CollectorConstants.getPassword() == null || CollectorConstants.getPassword() == "") {
                throw new InvalidParameterException("Missing value for Password");
            }
            if (CollectorConstants.getMarkWaitTime() == 0) {
                CollectorConstants.setMarkWaitTime(5000);
            }
            if (CollectorConstants.getResourceSettleTime() == 0) {
                CollectorConstants.setResourceSettleTime(2000);
            }

            if (CollectorConstants.getManualResourceTimeClear().equals("true")) {
                CollectorConstants.setManualResourceTimeClear("true");
            }
            else
            {
                CollectorConstants.setManualResourceTimeClear("false");
            }
            if(CollectorConstants.getRelease() == null || CollectorConstants.getRelease() == "")
            {
                CollectorConstants.setRelease(new SimpleDateFormat("yyyyMM").format(new Date()));
            }
            if(CollectorConstants.getBuild() == null || CollectorConstants.getBuild() == "")
            {
                CollectorConstants.setBuild(new SimpleDateFormat("yyyyMMddHH").format(new Date()));
            }




            CXOptimizeService cxOpService = new CXOptimizeServiceImpl();
            CollectorConstants.setApiToken(cxOpService.getAuthToken());

            LOGGER.debug("CXOP - Created ApiToken for the session {}", CollectorConstants.getApiToken());

            config = cxOpService.getConfiguration();

            config.put("beaconUrl", CollectorConstants.getBeaconURL());
            config.put("RunTime", config.containsKey("runTimestamp") ? config.get("runTimestamp") : System.currentTimeMillis());
            config.put("Release",CollectorConstants.getRelease());
            config.put("BuildNumber",CollectorConstants.getBuild());

            CollectorConstants.setStaticExt(config.get("staticResourceExtension").toString());
            CollectorConstants.setImages(config.get("imageResourceExtension").toString());
            CollectorConstants.setResDurThreshold((config.get("resourceDurationThreshold") != null ? Double.parseDouble(config.get("resourceDurationThreshold").toString()) : Double.parseDouble("10")));

            clientConfig = config;

            LOGGER.debug("CXOP - Complete Configuration :{}", config.toString());
        } catch (Exception e) {
            LOGGER.error("CXOP - Cannot load configuration due to exception : {}",e);
        }
    }

    @SuppressWarnings("DoubleCheckedLocking")
    public static ConfigurationLoader getInstance() {
        if (instance == null) {
            synchronized (mutex) {
                if (instance == null) {
                    instance = new ConfigurationLoader();
                }
            }
        }
        return instance;
    }

    //To integrate with Cognizant Intelligent Scripter
    public static void reloadConfig()
    {
        instance = null;
    }

}
