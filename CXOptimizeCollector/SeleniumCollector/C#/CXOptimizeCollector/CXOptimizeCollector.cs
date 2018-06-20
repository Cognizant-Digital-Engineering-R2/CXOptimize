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

using com.cognizant.pace.CXOptimize.Collector.utils;
using com.cognizant.pace.CXOptimize.Collector.constant;
using OpenQA.Selenium;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace com.cognizant.pace.CXOptimize.Collector
{
    public class CXOptimizeCollector
    {

        private static readonly log4net.ILog LOGGER = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public static int StartTransaction(String txnName, IWebDriver browser)
        {
            int rtnValue = 0;
            CollectorConstants.setScriptStartTime(Convert.ToInt64((DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1))).TotalMilliseconds));
            IJavaScriptExecutor jsExe = (IJavaScriptExecutor)browser;
            jsExe.ExecuteScript(CollectorConstants.clearResourceTiming());
            LOGGER.Debug("Cleared Resource Timing API data");
            return rtnValue;
        }

        public static Dictionary<String, Object> EndTransaction(String txnName, IWebDriver browser, int txnStatus)
        {
            LOGGER.Debug("Started collecting data for Transaction : " + txnName + " with status " +  txnStatus);
            Dictionary<String, Object> rtnValue = new Dictionary<string, object>();
            try
            {
                rtnValue = CollectorUtils.extractData(txnName, browser.Url, browser, txnStatus);
            }
            catch (Exception e)
            {
                LOGGER.Error("Exception collecting data for Transaction : " + txnName + " with status " + txnStatus + " at " + e);
            }
            LOGGER.Debug("Completed collecting data for Transaction :  " + txnName + " with status " + txnStatus);
            return rtnValue;
        }

        public static Dictionary<String, Object> EndTransaction(String txnName, IWebDriver browser)
        {
            LOGGER.Debug("Started collecting data for Transaction : " + txnName + " with status 1");
            Dictionary<String, Object> rtnValue = new Dictionary<string, object>();
            try
            {
                rtnValue = CollectorUtils.extractData(txnName, browser.Url, browser,1);
            }
            catch (Exception e)
            {
                LOGGER.Error("Exception collecting data for Transaction : " + txnName + " with status 1 at " + e);
            }
            LOGGER.Debug("Completed collecting data for Transaction :  " + txnName + " with status 1");
            return rtnValue;
        }

    }
}
