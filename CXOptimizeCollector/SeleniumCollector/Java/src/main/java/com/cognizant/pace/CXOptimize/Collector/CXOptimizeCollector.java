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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        //LOGGER.info("Started collecting data for Transaction : {}",txnName);
        int rtnValue = 0;
        CollectorConstants.setScriptStartTime(System.currentTimeMillis());
        JavascriptExecutor jsExe = (JavascriptExecutor) browser;
        jsExe.executeScript("window.performance.clearResourceTimings();");
        LOGGER.debug("Cleared Resource Timing API data");
        return rtnValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> EndTransaction(String txnName, WebDriver browser) {
        LOGGER.debug("Started collecting data for Transaction : {} with status {}", txnName, "1");
        Map<String, Object> rtnValue = new HashMap<>();
        try {

            rtnValue = CollectorUtils.extractData(txnName, browser.getCurrentUrl(), browser, 1);
        } catch (Exception e) {
            LOGGER.error("Exception collecting data for Transaction : {} with status {} at {}", txnName, "1",e);
        }
        LOGGER.debug("Completed collecting data for Transaction : {} with status {}", txnName, "1");
        return rtnValue;
    }

    public static Map<String, Object> EndTransaction(String txnName, WebDriver browser, int txnStatus) {
        LOGGER.debug("Started collecting data for Transaction : {} with status {}", txnName, txnStatus);
        Map<String, Object> rtnValue = new HashMap<>();
        try {

            rtnValue = CollectorUtils.extractData(txnName, browser.getCurrentUrl(), browser, txnStatus);
        } catch (Exception e) {
            LOGGER.error("Exception collecting data for Transaction : {} with status {} at {}", txnName, txnStatus,e);
        }
        LOGGER.debug("Completed collecting data for Transaction : {} with status {}", txnName, txnStatus);
        return rtnValue;
    }

    /*
    public static Map<String, Object> endTransaction(String txnName, WebDriver browser, int txnStatus,String checkBy,String condition)
    {
        LOGGER.debug("Started collecting data for Transaction : {} with status {}", txnName, txnStatus);
        Map<String, Object> rtnValue = new HashMap<>();
        try {

            switch(checkBy)
            {
                case "CLASSNAME":
                    LOGGER.debug("Waiting unting element located by className : {}",condition);
                    new WebDriverWait(browser,5000,10).until(ExpectedConditions.visibilityOfElementLocated((By.className(condition))));
                    break;
                case "CSSSELECTOR":
                    LOGGER.debug("Waiting unting element located by cssselector : {}",condition);
                    new WebDriverWait(browser,5000,10).until(ExpectedConditions.visibilityOfElementLocated((By.cssSelector(condition))));
                    break;
                case "ID":
                    LOGGER.debug("Waiting unting element located by id : {}",condition);


                    break;
                case "LINKTEXT":
                    LOGGER.debug("Waiting unting element located by linktext : {}",condition);
                    new WebDriverWait(browser,5000,10).until(ExpectedConditions.visibilityOfElementLocated((By.linkText(condition))));
                    break;
                case "NAME":
                    LOGGER.debug("Waiting unting element located by name : {}",condition);
                    new WebDriverWait(browser,5000,10).until(ExpectedConditions.visibilityOfElementLocated((By.name(condition))));
                    break;
                case "PARTIALLINKTEXT":
                    LOGGER.debug("Waiting unting element located by partiallinktext : {}",condition);
                    new WebDriverWait(browser,5000,10).until(ExpectedConditions.visibilityOfElementLocated((By.partialLinkText(condition))));
                    break;
                case "TAGNAME":
                    LOGGER.debug("Waiting unting element located by tagname : {}",condition);
                    new WebDriverWait(browser,5000,10).until(ExpectedConditions.visibilityOfElementLocated((By.tagName(condition))));
                    break;
                case "XPATH":
                    LOGGER.debug("Waiting unting element located by xpath : {}",condition);
                    new WebDriverWait(browser,5000,10).until(ExpectedConditions.visibilityOfElementLocated((By.xpath(condition))));
                    break;
                default:
                    LOGGER.debug("Waiting unting element located by default : {}",condition);
                    Thread.sleep(2000);

            }

            rtnValue = CollectorUtils.extractData(txnName, browser.getCurrentUrl(), browser, txnStatus);
        } catch (Exception e) {
            LOGGER.error("Exception collecting data for Transaction : {} with status {} at {}", txnName, txnStatus,e);
        }
        LOGGER.debug("Completed collecting data for Transaction : {} with status {}", txnName, txnStatus);
        return rtnValue;
    }



    @SuppressWarnings("unchecked")
    public static Map<String, Object> collectPerformanceData(String txnName, WebDriver browser, int txnStatus) {
        LOGGER.debug("Started collecting data for Transaction : {} with status {}", txnName, txnStatus);
        Map<String, Object> rtnValue = new HashMap<>();
        try {
            rtnValue = CollectorUtils.extractData(txnName, browser.getCurrentUrl(), browser, txnStatus);
        } catch (Exception e) {
            LOGGER.error("Exception collecting data for Transaction : {} with status {} at {}", txnName, txnStatus,e);
        }
        LOGGER.debug("Completed collecting data for Transaction : {} with status {}", txnName, txnStatus);
        return rtnValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> collectPerformanceData(String txnName, WebDriver browser) {
        LOGGER.debug("Started collecting data for Transaction : {}", txnName);
        Map<String, Object> rtnValue = new HashMap<>();
        try {
            rtnValue = CollectorUtils.extractData(txnName, browser.getCurrentUrl(), browser, 1);
        } catch (Exception e) {
            LOGGER.error("Exception collecting data for Transaction : {} at {}", txnName,e);
        }
        LOGGER.debug("Completed collecting data for Transaction : {}", txnName);
        return rtnValue;
    }


    @SuppressWarnings("unchecked")
    public static int startSoftNavigation(String txnName, WebDriver browser) {
        //LOGGER.debug("Started collecting data for Transaction : {}",pTxnName);
        int rtnValue = 0;
        JavascriptExecutor jsExe = (JavascriptExecutor) browser;
        jsExe.executeScript("window.performance.clearResourceTimings();");
        LOGGER.debug("Cleared Resource Timing API data");
        return rtnValue;
    }



    @SuppressWarnings("unchecked")
    public static Map<String, Object> endSoftNavigation(String txnName, WebDriver browser, int txnStatus) {
        LOGGER.debug("Started collecting data for Transaction : {} with status {}", txnName, txnStatus);
        Map<String, Object> rtnValue = new HashMap<>();
        try {
            rtnValue = CollectorUtils.extractData(txnName, browser.getCurrentUrl(), browser, txnStatus);
        } catch (Exception e) {
            LOGGER.error("Exception collecting data for Transaction : {} with status {} at {}", txnName, txnStatus,e);
        }
        LOGGER.debug("Completed collecting data for Transaction : {} with status {}", txnName, txnStatus);
        return rtnValue;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> endSoftNavigation(String txnName, WebDriver browser) {
        LOGGER.debug("Started collecting data for Transaction : {}", txnName);
        Map<String, Object> rtnValue = new HashMap<>();
        try {
            rtnValue = CollectorUtils.extractData(txnName, browser.getCurrentUrl(), browser, 1);
        } catch (Exception e) {
            LOGGER.error("Exception collecting data for Transaction : {} at {}", txnName,e);
        }
        LOGGER.debug("Completed collecting data for Transaction : {}", txnName);
        return rtnValue;
    }
    */

}
