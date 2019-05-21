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


package com.cognizant.pace.CXOptimize.AnalysisEngine

class GlobalConstants
{
    public static final String BASELINEINDEX = 'baselineindex'
    public static final String BASELINETABLE = 'baselineindex/baseline'
    public static final String BASELINETABLE_WS = 'baselineindex/baseline/'
    public static final String BASELINESEARCH = 'baselineindex/baseline/_search'
    public static final String BASELINEDELETE = 'baselineindex/_delete_by_query'
    public static final String CONFIGSEARCH = 'masterconfig/config/_search'
    public static final String RULESSEARCH = 'masterconfig/rules/_search'
    public static final String ES6RULESSEARCH = 'rulesindex/rules/_search'
    public static final String RULESDOC = 'masterconfig/rules/rulesdoc'
    public static final String ES6RULESDOC = 'rulesindex/rules/rulesdoc'
    public static final String CONFIGTABLE = 'masterconfig/config'
    public static final String CONFIGREFRESH = 'masterconfig/_refresh'
    public static final String CONFIGTABLE_WS = 'masterconfig/config/'
    public static final String CONFIGDELETE = 'masterconfig/_delete_by_query'
    public static final String ANALYSISTABLE = 'analysisindex/analysis'
    public static final String ANALYSISTABLE_WS = 'analysisindex/analysis/'
    public static final String ANALYSISSEARCH = 'analysisindex/analysis/_search'
    public static final String ANALYSISDELETE = 'analysisindex/_delete_by_query'
    public static final String STATSSEARCH = 'statsindex/run/_search'
    public static final String STATSINDEX_INSERT = 'statsindex/run'
    public static final String STATSINDEXTABLE = 'statsindex/run/'
    public static final String STATSINDEX = 'statsindex'
    public static final String STATSDELETE = 'statsindex/_delete_by_query'
    public static final String USERSEARCH = 'userdetails/user/_search'
    public static final String USERTABLE = 'userdetails/user'
    public static final String USERTABLE_WS = 'userdetails/user/'
    public static final String MARKSEARCH = 'markindex/mark/_search'
    public static final String MARKINDEX_INSERT = 'markindex/mark'
    public static final String MARKDELETE = 'markindex/_delete_by_query'
    public static final String AUDITINDEX_INSERT = 'auditindex/audit'
    public static final String CLIENTNAME = 'ClientName'
    public static final String PROJECTNAME = 'ProjectName'
    public static final String SCENARIO = 'Scenario'
    public static final String USERNAME = 'UserName'
    public static final String CPS_ERROR_MSG ='ClientName or ProjectName or Scenario cannot be null or blank'
    public static final String ANALYSISTYPE_ERROR_MSG ='Invalid Analysis Type only Run or Transaction or Time allowed'
    public static final String TIME_ERROR_MSG ='BaselineStart or BaselineEnd or CurrentStart or CurrentEnd cannot be null or blank'
    public static final String RUNID_ERROR_MSG ='Invalid RunID'
    public static final String BRUNID_ERROR_MSG ='Invalid BaselineRunID'
    public static final String TRANS_ERROR_MSG ='Invalid TransactionName'
    public static final String CONFIG_ERROR_MSG ='No configuration for the given input defined in the system'
    public static final String SUBC_ERROR_MSG ='Expired or Invalid Subscription'

    //private static long EXPIRATIONTIME = 600_000 // 10 minutes
    private static long EXPIRATIONTIME
    static long getExpirationTime()
    {
        return EXPIRATIONTIME
    }
    static void setExpirationTime(long expiryTime)
    {
        EXPIRATIONTIME = expiryTime * 60 * 1000
    }
    //private static String JWTPRIVATEKEY = "THISISSECRETKEYFORCXOPTIMISEPACEE1BB465D57CAE7ACDBBE8091F9CE83DF"
    private static String JWTPRIVATEKEY
    static String getJWTKey()
    {
        return JWTPRIVATEKEY
    }
    static String setJWTKey(String key)
    {
        JWTPRIVATEKEY = key
    }

    private static String SECRETKEY
    static String getKey()
    {
        return SECRETKEY
    }
    static String setKey(String key)
    {
        SECRETKEY = key
    }

    //Getter & Setter for ES Node Count
    private static int NODE_COUNT

    static int getNodeCount() {
        return NODE_COUNT
    }

    static void setNodeCount(int nodes) {
        NODE_COUNT = nodes
    }

    private static String ES_URL
    static String getESUrl() {
        return ES_URL
    }

    static void setESUrl(String url) {
        ES_URL = url
    }

    private static int RUNIDS_DISPLAY
    static int getRunIDCount() {
        return RUNIDS_DISPLAY
    }

    static void setRunIDCount(int count) {
        RUNIDS_DISPLAY = count
    }

    private static String ES_VERSION
    static String getESVersion() {
        return ES_VERSION
    }

    static void setESVersion(String version) {
        ES_VERSION = version
    }

}

