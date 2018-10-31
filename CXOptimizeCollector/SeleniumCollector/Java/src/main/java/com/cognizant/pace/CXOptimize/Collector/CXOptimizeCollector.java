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

package com.cognizant.pace.CXOptimize.Collector;

import com.cognizant.pace.CXOptimize.Collector.constant.CollectorConstants;
import com.cognizant.pace.CXOptimize.Collector.utils.CollectorUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides wrapper methods to access within Selenium script to collect performance metrics.
 * <br>
 * It handles use of testcase / reusable data , iterations and sub iterations,
 * environment based test data and exceptions and its causes <br>
 * Exceptions <br>
  * <br>
 *
 *
 *
 *
 */

public class CXOptimizeCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CXOptimizeCollector.class);

    @SuppressWarnings("unchecked")
    public static int StartTransaction(String txnName, WebDriver browser) {
        LOGGER.debug("CXOP - {} - Started clearing Resource Timing API data",txnName);
        int rtnValue = 0;
        CollectorConstants.setScriptStartTime(System.currentTimeMillis());

        //To support manual clear of resource timings from browser
        if(CollectorConstants.getTxnCounter() == 0)
        {
            CollectorConstants.setRunStartTime(System.currentTimeMillis());
            CollectorConstants.setTxnCounter(CollectorConstants.getTxnCounter() + 1);
        }

        JavascriptExecutor jsExe = (JavascriptExecutor) browser;
        jsExe.executeScript("window.performance.clearResourceTimings();");
        LOGGER.debug("CXOP - {} - Cleared Resource Timing API data",txnName);
        return rtnValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> EndTransaction(String txnName, WebDriver browser) {
        LOGGER.debug("CXOP - {} - Started collecting data with status {}", txnName.replaceAll(" ","_"), "1");
        Map<String, Object> rtnValue = new HashMap<>();
        try
        {
            rtnValue = CollectorUtils.extractData(txnName.replaceAll(" ","_"), browser.getCurrentUrl(), browser, 1);
            LOGGER.debug("CXOP - {} - Completed collecting data", txnName.replaceAll(" ","_"));
        } catch (Exception e)
        {
            LOGGER.error("CXOP - {} - Exception collecting data at {}", txnName.replaceAll(" ","_"),e);
        }

        return rtnValue;
    }

    public static Map<String, Object> EndTransaction(String txnName, WebDriver browser, int txnStatus) {
        LOGGER.debug("CXOP - {} - Started collecting data with status {}", txnName.replaceAll(" ","_"),txnStatus);
        Map<String, Object> rtnValue = new HashMap<>();
        try
        {
            rtnValue = CollectorUtils.extractData(txnName.replaceAll(" ","_"), browser.getCurrentUrl(), browser, txnStatus);
            LOGGER.debug("CXOP - {} - Completed collecting data with status {}", txnName.replaceAll(" ","_"),txnStatus);
        } catch (Exception e) {
            LOGGER.error("CXOP - {} - Exception collecting data at {}", txnName.replaceAll(" ","_"),e);
        }

        return rtnValue;
    }


}
