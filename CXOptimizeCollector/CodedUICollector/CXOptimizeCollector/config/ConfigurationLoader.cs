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
using com.cognizant.pace.CXOptimize.Collector.service;
using com.cognizant.pace.CXOptimize.Collector.utils;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace com.cognizant.pace.CXOptimize.Collector.config
{
    public class ConfigurationLoader
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        private static ConfigurationLoader instance = null;
        private static Object mutex = new Object();
        public Dictionary<String, Object> clientConfig = null;

        
        string propertyDirPath = string.Empty;

        private void GetPropertyFile(string fullPathName)
        {
            string lpropertyFilePath = fullPathName + "//collectordependency";

            if (!Directory.Exists(lpropertyFilePath))
            {
                GetPropertyFile(new DirectoryInfo(fullPathName).Parent.FullName);
            }
            else
            {
                propertyDirPath = lpropertyFilePath;
            }
                
        }
        /*
        private Dictionary<String, String> GetBeaconPropertyLoadConfig(String path)
        {

            string fullPathName = string.Empty;
            if (path != null)
            {
                fullPathName = propertyDirPath + "\\Collector.properties";
            }
            else
            {
                string projectDir = AppDomain.CurrentDomain.SetupInformation.ApplicationBase;
                LoggingMessage.DisplayMessage("CXOptimize - Project Target Dir Path -> " + projectDir, LoggingMessage.MessageType.DEBUG, log);
                GetPropertyFile(projectDir);

            }
            string[] split;
            string line = String.Empty;
            Dictionary<String, String> config = null;

            //string projectDir = AppDomain.CurrentDomain.SetupInformation.ApplicationBase;

          
            string environmentKey = ConfigurationManager.AppSettings["Environment"];
            if (environmentKey == null || environmentKey == string.Empty)
                fullPathName = propertyDirPath + "\\Collector.properties";
            else
                fullPathName = string.Format("{0}\\{1}_Collector.properties", propertyDirPath, environmentKey);

            LoggingMessage.DisplayMessage("CXOptimize - Config Path-> " + fullPathName, LoggingMessage.MessageType.DEBUG, log);

            if (File.Exists(fullPathName))
            {
                config = new Dictionary<string, string>();
                using (StreamReader file = new StreamReader(fullPathName))
                {
                    while ((line = file.ReadLine()) != null)
                    {
                        split = line.Split('=');
                        if (split.Length == 2)
                            config.Add(split[0], split[1]);
                    }
                }

            }
            else
            { 
                throw new ArgumentException("Collector Property file missing");
            }

            return config;
        }
        */

        protected ConfigurationLoader()
        {

            try
            {
                log.Debug("Starting Loading Configuration");

                String path = CollectorConstants.getCollectorProperties();

                if (path == null)
                {
                    GetPropertyFile(AppDomain.CurrentDomain.SetupInformation.ApplicationBase);
                    path = propertyDirPath + "\\Collector.properties";
                }
                else
                {
                    path = path + "\\Collector.properties";
                }

                Dictionary<String, String> config;
                string[] split;
                string line = String.Empty;

                if (File.Exists(path))
                {
                    config = new Dictionary<string, string>();
                    using (StreamReader file = new StreamReader(path))
                    {
                        while ((line = file.ReadLine()) != null)
                        {
                            split = line.Split('=');
                            if (split.Length == 2)
                                config.Add(split[0], split[1]);
                        }
                    }

                }
                else
                {
                    throw new ArgumentException("Collector Property file missing");
                }


                if (config != null)
                {
                    log.Debug("Properties Load Successfull from file : " + config.ToString());
                    CollectorConstants.setBeaconURL(config["beaconUrl"]);
                    CollectorConstants.setClientName(config["clientName"]);
                    CollectorConstants.setProjectName(config["projectName"]);
                    CollectorConstants.setScenarioName(config["scenarioName"]);
                    CollectorConstants.setLicenseKey(config["LicenseKey"]);
                    CollectorConstants.setUserName(config["UserName"]);
                    CollectorConstants.setPassword(config["Password"]);
                    /*
                    if (config.ContainsKey("Release") && config["Release"] != String.Empty)
                    {
                        CollectorConstants.setRelease(config["Release"].ToString());
                    }
                    else
                    {
                        CollectorConstants.setRelease("NA");
                    }
                    if (config.ContainsKey("Build") && config["Build"] != String.Empty)
                    {
                        CollectorConstants.setBuild(config["Build"].ToString());
                    }
                    else
                    {
                        CollectorConstants.setBuild("NA");
                    }*/
                    if (config.ContainsKey("isLoadTest") && config["isLoadTest"].Contains("true"))
                    {
                        CollectorConstants.setLoadTest("true");
                    }
                    else
                    {
                        CollectorConstants.setLoadTest("false");
                    }
                    if (config.ContainsKey("markWaitTime") && config["markWaitTime"] != String.Empty)
                    {
                        CollectorConstants.setMarkWaitTime(Int32.Parse(config["markWaitTime"]));
                    }
                    else
                    {
                        CollectorConstants.setMarkWaitTime(5000);
                    }
                    if (config.ContainsKey("resourceSettleTime"))
                    {
                        CollectorConstants.setResourceSettleTime(Int32.Parse(config["resourceSettleTime"]));
                    }
                    else
                    {
                        CollectorConstants.setResourceSettleTime(2000);
                    }
                    log.Debug("Loaded initialization parameters");
                }


                if (CollectorConstants.getBeaconURL() == null || CollectorConstants.getBeaconURL() == "")
                {
                    throw new ArgumentException("Missing value for BeaconURL");
                }
                if (CollectorConstants.getClientName() == null || CollectorConstants.getClientName() == "")
                {
                    throw new ArgumentException("Missing value for ClientName");
                }
                if (CollectorConstants.getProjectName() == null || CollectorConstants.getProjectName() == "")
                {
                    throw new ArgumentException("Missing value for ProjectName");
                }
                if (CollectorConstants.getScenarioName() == null || CollectorConstants.getScenarioName() == "")
                {
                    throw new ArgumentException("Missing value for ScenarioName");
                }
                if (CollectorConstants.getUserName() == null || CollectorConstants.getUserName() == "")
                {
                    throw new ArgumentException("Missing value for UserName");
                }
                if (CollectorConstants.getPassword() == null || CollectorConstants.getPassword() == "")
                {
                    throw new ArgumentException("Missing value for Password");
                }

                if (CollectorConstants.getMarkWaitTime() == 0)
                {
                    CollectorConstants.setMarkWaitTime(5000);
                }
                if (CollectorConstants.getResourceSettleTime() == 0)
                {
                    CollectorConstants.setResourceSettleTime(2000);
                }
                if (CollectorConstants.getRelease() == null || CollectorConstants.getRelease() == string.Empty)
                {
                    CollectorConstants.setRelease(DateTime.Now.ToString("yyyyMM"));
                }
                if (CollectorConstants.getBuild() == null || CollectorConstants.getBuild() == string.Empty)
                {
                    CollectorConstants.setBuild(DateTime.Now.ToString("yyyyMMddHH"));
                }


                Dictionary<string, object> configs = new Dictionary<string, object>();
                ICXOptimizeService cxOpService = new CXOptimizeServiceImpl();
                CollectorConstants.setApiToken(cxOpService.getAuthToken());

                log.Debug("Created ApiToken for the session " + CollectorConstants.getApiToken());

                configs = cxOpService.getConfiguration();

                if (config.ContainsKey("Status")) // Failed to get the config
                {
                    log.Info("Cannot load configuration: " + config["Reason"]);
                    config.Clear();
                }
                else
                {
                    configs.Add("beaconUrl", CollectorConstants.getBeaconURL());
                    configs.Add("RunTime", configs.ContainsKey("runTimestamp") ? configs["runTimestamp"] :
                    (DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalMilliseconds);
                    CollectorConstants.setStaticExt(configs.ContainsKey("staticResourceExtension") ? configs["staticResourceExtension"].ToString() : "");
                    CollectorConstants.setImages(configs.ContainsKey("imageResourceExtension") ? configs["imageResourceExtension"].ToString() : "");
                    CollectorConstants.setResDurThreshold((configs.ContainsKey("resourceDurationThreshold") ? Double.Parse(configs["resourceDurationThreshold"].ToString())
                        : Double.Parse("10")));
                    configs.Add("Release", CollectorConstants.getRelease());
                    configs.Add("BuildNumber", CollectorConstants.getBuild());
                    clientConfig = configs;

                    log.Debug("Complete Configuration " + config.ToString());
                }
            }
            catch (Exception e)
            {
                log.Error("Cannot load configuration due to exception : " + e);
            }
        }


        public static ConfigurationLoader getInstance()
        {
            if (instance == null)
            {
                lock (mutex)
                {
                    if (instance == null)
                    {
                        instance = new ConfigurationLoader();
                    }
                }
            }
            return instance;
        }
    }
}
