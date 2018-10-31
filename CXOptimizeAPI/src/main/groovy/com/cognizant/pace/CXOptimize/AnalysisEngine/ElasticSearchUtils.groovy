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

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.json.JSONObject
import org.slf4j.LoggerFactory

class ElasticSearchUtils
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ElasticSearchUtils.class)
    static def baseFilterQuery(def configReader,def timeSearch = null,def type = null)
    {
        StringBuilder query = new StringBuilder()
        if(timeSearch == null)
        {
            query.append('{"term": {"ClientName": "').append(configReader.ClientName).append('"}},{"term": {"ProjectName": "').append(configReader.ProjectName).append('"}},{"term": {"Scenario": "').append(configReader.Scenario).append('"}}')
        }
        else
        {
            if(type == 'Current')
            {
                query.append('{"term": {"ClientName": "').append(configReader.ClientName).append('"}},{"term": {"ProjectName": "').append(configReader.ProjectName).append('"}},{"term": {"Scenario": "').append(configReader.Scenario).append('"}}').append(',{"range": {"StartTime": {"gte" : "').append(configReader.CurrentStart).append('","lte" : "').append(configReader.CurrentEnd).append('"}}}')
            }
            else
            {
                query.append('{"term": {"ClientName": "').append(configReader.ClientName).append('"}},{"term": {"ProjectName": "').append(configReader.ProjectName).append('"}},{"term": {"Scenario": "').append(configReader.Scenario).append('"}}').append(',{"range": {"StartTime": {"gte" : "').append(configReader.BaselineStart).append('","lte" : "').append(configReader.BaselineEnd).append('"}}}')
            }
        }
        log.debug 'baseFilterQuery : ' +  query

        return query
    }

    static def extractConfig(String clientName,String projectName,String scenarioName,String esURL,String path=null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"query": {"filtered": {"filter": {"and": [{"term": {"ClientName": "').append(clientName).append('"}},{"term": {"ProjectName": "').append(projectName).append('"}},{"term": {"Scenario": "').append(scenarioName).append('"}}]}}}}')
        query.append('{"query": {"bool": {"filter":[{"term": {"ClientName": "').append(clientName).append('"}},{"term": {"ProjectName": "').append(projectName).append('"}},{"term": {"Scenario": "').append(scenarioName).append('"}}]}}}')
        log.debug 'extractConfig query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.CONFIGSEARCH,query)
        log.debug 'extractConfig Response : ' + response_body
        return response_body?.hits?.hits[0]?."_source"
    }

    static def isConfigExists(String clientName,String projectName,String scenarioName,String esURL,String path=null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"query": {"filtered": {"filter": {"and": [{"term": {"ClientName": "').append(clientName).append('"}},{"term": {"ProjectName": "').append(projectName).append('"}},{"term": {"Scenario": "').append(scenarioName).append('"}}]}}}}')
        query.append('{"query": {"bool": {"filter":[{"term": {"ClientName": "').append(clientName).append('"}},{"term": {"ProjectName": "').append(projectName).append('"}},{"term": {"Scenario": "').append(scenarioName).append('"}}]}}}')
        log.debug 'extractConfig query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.CONFIGSEARCH,query)
        log.debug 'extractConfig Response : ' + response_body
        return response_body
    }

    static def elasticSearchGET(String esUrl,String path)
    {
        def response_body = null
        try
        {

            def http = new HTTPBuilder(esUrl)

            http.request(Method.GET)
                    {
                        uri.path = path
                        requestContentType = ContentType.JSON
                        //headers.'Accept-Encoding' = 'gzip,deflate'
                        response.success = { resp, reader ->
                            if (reader != null)
                            {
                                response_body = reader
                            }
                        }
                        response.failure = { resp, reader ->
                            response_body = null
                        }
                    }
        }
        catch (groovyx.net.http.HttpResponseException ex)
        {
            response_body = null
        }
        catch (java.net.ConnectException ex)
        {
            response_body = null
        }
        return response_body
    }

    static def elasticSearchPOST(String esUrl,String path,StringBuilder queryString = null)
    {
        log.debug 'POST QueryString : ' +  queryString
        log.debug 'POST QueryString : ' +  esUrl
        log.debug 'POST QueryString : ' +  path
        def response_body = null
        def bodyQuery = null
        try
        {
            if(queryString)
            {
                def jsonObj = new JSONObject(queryString.toString())
                bodyQuery = JsonUtils.jsonToMap(jsonObj)
            }
            def http = new HTTPBuilder(esUrl)

            http.request(Method.POST)
                    {
                        uri.path = path
                        requestContentType = ContentType.JSON
                        body = bodyQuery
                        //headers.'Accept-Encoding' = 'gzip,deflate'
                        response.success = { resp, reader ->
                            if (reader != null)
                            {
                                response_body = reader
                            }
                        }
                        response.failure = { resp, reader ->
                            response_body = null
                        }
                    }
        }
        catch (groovyx.net.http.HttpResponseException ex)
        {
            response_body = null
        }
        catch (java.net.ConnectException ex)
        {
            response_body = null
        }
        log.debug 'POST QueryString : ' +  response_body
        return response_body
    }

    static def elasticSearchPOST(String esUrl,String path,String queryString)
    {
        log.debug 'POST QueryString : ' +  queryString
        def response_body = null
        def bodyQuery = null
        try
        {
            /*if(queryString)
            {
                def jsonObj = new JSONObject(queryString)
                bodyQuery = JsonUtils.jsonToMap(jsonObj)
            }*/
            def http = new HTTPBuilder(esUrl)

            http.request(Method.POST)
                    {
                        uri.path = path
                        requestContentType = ContentType.JSON
                        body = queryString
                        //headers.'Accept-Encoding' = 'gzip,deflate'
                        response.success = { resp, reader ->
                            if (reader != null)
                            {
                                response_body = reader
                            }
                        }
                        response.failure = { resp, reader ->
                            response_body = null
                        }
                    }
        }
        catch (groovyx.net.http.HttpResponseException ex)
        {
            response_body = null
        }
        catch (java.net.ConnectException ex)
        {
            response_body = null
        }
        return response_body
    }

    static def elasticSearchPUT(String esUrl,String path,String queryString)
    {
        log.debug 'PUT QueryString : ' +  queryString
        def response_body = null
        def bodyQuery = null
        try
        {
            if(queryString)
            {
                def jsonObj = new JSONObject(queryString)
                bodyQuery = JsonUtils.jsonToMap(jsonObj)
            }
            def http = new HTTPBuilder(esUrl)

            http.request(Method.PUT)
                    {
                        uri.path = path
                        requestContentType = ContentType.JSON
                        body = bodyQuery
                        //headers.'Accept-Encoding' = 'gzip,deflate'
                        response.success = { resp, reader ->
                            if (reader != null)
                            {
                                response_body = reader
                            }
                        }
                        response.failure = { resp, reader ->
                            log.debug 'PUT reader : ' +  reader
                            response_body = null
                        }
                    }
        }
        catch (groovyx.net.http.HttpResponseException ex)
        {
            log.debug 'PUT Response Exception : '
            ex.printStackTrace()
            response_body = null
        }
        catch (java.net.ConnectException ex)
        {
            log.debug 'PUT Connect Exception : '
            println ex.getStackTrace()
            ex.printStackTrace()
            response_body = null
        }
        return response_body
    }

    static def elasticSearchPOSTWithMap(String esUrl,String path,def queryString)
    {
        log.debug 'POST QueryString : ' +  queryString
        def response_body = null
        try
        {
            def http = new HTTPBuilder(esUrl)

            http.request(Method.POST)
                    {
                        uri.path = path
                        requestContentType = ContentType.JSON
                        body = queryString
                        //headers.'Accept-Encoding' = 'gzip,deflate'
                        response.success = { resp, reader ->
                            if (reader != null)
                            {
                                response_body = reader
                            }
                        }
                        response.failure = { resp, reader ->
                            println reader
                            response_body = null
                        }
                    }
        }
        catch (groovyx.net.http.HttpResponseException ex)
        {
            response_body = null
        }
        catch (java.net.ConnectException ex)
        {
            response_body = null
        }
        return response_body
    }

    static def elasticSearchDELETE(String esUrl,String path)
    {
        def response_body = null
        try
        {
            def http = new HTTPBuilder(esUrl)

            http.request(Method.DELETE)
                    {
                        uri.path = path
                        requestContentType = ContentType.JSON
                        //headers.'Accept-Encoding' = 'gzip,deflate'
                        response.success = { resp, reader ->
                            if (reader != null)
                            {
                                response_body = reader
                            }
                        }
                        response.failure = { resp, reader ->
                            response_body = null
                        }
                    }
        }
        catch (groovyx.net.http.HttpResponseException ex)
        {
            response_body = null
        }
        catch (java.net.ConnectException ex)
        {
            response_body = null
        }
        return response_body
    }

    static def persistAnalysisReport(def configReader,StringBuilder analysisReport,String runID = null,String prevRunID = null,boolean override = false)
    {
        def analysisExists = null
        def persistStatus = null
        def deleteStatus = null
        def query = null

        log.debug 'persistAnalysisReport analysis Report : ' +  analysisReport

        analysisExists = ElasticSearchUtils.analysisReportExists(configReader,configReader.CurrentRun.toString(),configReader.BaselineRun.toString())
        if(override)
        {
            if(analysisExists.hits.hits.size > 0)
            {
                deleteStatus = ElasticSearchUtils.elasticSearchDELETE(configReader.esUrl,GlobalConstants.ANALYSISTABLE_WS + analysisExists.hits.hits[0]."_id" + '/')
                persistStatus = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.ANALYSISTABLE,analysisReport)
            }
            else
            {
                persistStatus = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.ANALYSISTABLE,analysisReport)
            }
        }
        else
        {
            if(analysisExists.hits.hits.size <= 0)
            {
                persistStatus = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.ANALYSISTABLE,analysisReport)
            }
        }
        return persistStatus
    }

    static def updateBaseline(def configReader,def updatedBaseline)
    {
        def baselineExists = null
        def persistStatus = null
        def deleteStatus = null

        def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)
        log.debug 'updateBaseline : ' +  updatedBaseline

        baselineExists = ElasticSearchUtils.baselineReportExists(configReader,comparisonPerc)

        if(baselineExists.hits.hits.size > 0)
        {
            deleteStatus = ElasticSearchUtils.elasticSearchDELETE(configReader.esUrl,GlobalConstants.BASELINETABLE_WS + baselineExists.hits.hits[0]."_id" +'/')
            persistStatus = ElasticSearchUtils.elasticSearchPOSTWithMap(configReader.esUrl,GlobalConstants.BASELINETABLE_WS,updatedBaseline)
        }
        else
        {
            persistStatus = ElasticSearchUtils.elasticSearchPOSTWithMap(configReader.esUrl,GlobalConstants.BASELINETABLE,updatedBaseline)
        }

        return persistStatus
    }

    static def analysisReportExists(def configReader,String runID = null,String prevRunID = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if (prevRunID == null)
        {
            if (runID == null)
            {
                //query.append('{"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(configReader.RunID.toString()).append('"}}]}}}}')
                query.append('{"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(configReader.RunID.toString()).append('"}}]}}}')
            }
            else
            {
                //query.append('{"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(runID).append('"}}]}}}}')
                query.append('{"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(runID).append('"}}]}}}')
            }
        }
        else
        {
            if (runID == null)
            {
                //query.append('{"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(configReader.RunID.toString()).append('"}}').append(',{"term": {"BaselineRunID": "').append(prevRunID).append('"}}]}}}}')
                query.append('{"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(configReader.RunID.toString()).append('"}}').append(',{"term": {"BaselineRunID": "').append(prevRunID).append('"}}]}}}')
            }
            else
            {
                //query.append('{"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(runID).append('"}}').append(',{"term": {"BaselineRunID": "').append(prevRunID).append('"}}]}}}}')
                query.append('{"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"CurrentRunID": "').append(runID).append('"}}').append(',{"term": {"BaselineRunID": "').append(prevRunID).append('"}}]}}}')
            }

        }

        log.debug 'analysisReportExists query : ' +  query

        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.ANALYSISSEARCH,query)
        return response_body
    }

    static def refreshESIndex(String baseUrl,String index)
    {
        def refreshStatus
        try
        {

            def http = new HTTPBuilder(baseUrl)
            http.request(Method.GET, ContentType.JSON)
                    {
                        uri.path = index + "/_refresh"
                        headers.'Accept-Encoding' = 'gzip,deflate'
                        headers.Accept = 'application/json';
                        response.success = { resp  ->
                            refreshStatus = resp.status
                        }
                        response.failure = { resp ->
                            refreshStatus = resp.status
                        }
                    }
        }
        catch (groovyx.net.http.HttpResponseException ex)
        {
            refreshStatus = 0
        }
        catch (java.net.ConnectException ex)
        {
            refreshStatus = 0
        }
        return refreshStatus
    }

    static def baselineReportExists(def configReader,def percentile = 'Pcnt95')
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"Percentile": "').append(percentile.toLowerCase()).append('"}}]}}}}')
        query.append('{"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"Percentile": "').append(percentile.toLowerCase()).append('"}}]}}}')
        log.debug 'baselineReportExists query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.BASELINESEARCH,query)
        return response_body
    }

    static def getAggregatedResponseTime(def configReader,String runID = null,String txnName = null,String type=null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            if (txnName == null)
            {
                //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append((runID == null ? configReader.RunID.toString() : runID)).append('"}}]}}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append((runID == null ? configReader.RunID.toString() : runID)).append('"}}]}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"max":{ "max": {"field" : "totalPageLoadTime"}},"min":{ "min": {"field" : "totalPageLoadTime"}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
            }
            else
            {
                //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append((runID == null ? configReader.RunID.toString() : runID)).append('"}}').append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append((runID == null ? configReader.RunID.toString() : runID)).append('"}}').append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"max":{ "max": {"field" : "totalPageLoadTime"}},"min":{ "min": {"field" : "totalPageLoadTime"}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
            }
        }

        if(configReader.AnalysisType == 'Time')
        {
            if(type == 'Current')
            {
                if (txnName == null)
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"max":{ "max": {"field" : "totalPageLoadTime"}},"min":{ "min": {"field" : "totalPageLoadTime"}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                }
                else
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"max":{ "max": {"field" : "totalPageLoadTime"}},"min":{ "min": {"field" : "totalPageLoadTime"}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                }
            }
            else
            {
                if (txnName == null)
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"max":{ "max": {"field" : "totalPageLoadTime"}},"min":{ "min": {"field" : "totalPageLoadTime"}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                }
                else
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}},"aggs" : {"Transaction" : {"terms" : {"field" : "TransactionName","size" : 10000 },"aggs" : {"percentile" : {"percentiles" : { "field" : "totalPageLoadTime","percents" : [90.0,95.0]}},"max":{ "max": {"field" : "totalPageLoadTime"}},"min":{ "min": {"field" : "totalPageLoadTime"}},"average" : {"avg" : {"field" : "totalPageLoadTime"}}}}}}')
                }
            }

        }

        log.debug 'getAggregatedResponseTime query : ' +  query
        //println query
        //def path = configReader.path + '/run/_search'
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)

        def txnMetricsCalc = response_body.aggregations.Transaction.buckets
        def txnSamples = [:]
        def txnMetrics = [:]
        def selectedSample = null
        def comparisonPerc = (configReader?.samplePercentile == null ? 'Pcnt95' : configReader.samplePercentile)

        txnMetricsCalc.each { it ->
            txnMetrics.put("Count",it.doc_count)
            txnMetrics.put("Max",it.max.value)
            txnMetrics.put("Min",it.min.value)
            if(comparisonPerc == 'Average')
            {
                selectedSample = ElasticSearchUtils.getSampleValueForResponseTimeAndPayLoad(configReader,it.key,(it.average.value).toString(),(runID == null ? null : runID),type)
                txnMetrics.put("Average",Float.parseFloat(selectedSample.totalPageLoadTime.toString()).round())
                txnMetrics.put("HttpCount",selectedSample.resourceCount)
                txnMetrics.put("Payload",selectedSample.resourceSize)
            }
            else
            {
                txnMetrics.put("Average",ElasticSearchUtils.getSampleValueForResponseTime(configReader,it.key,(it.average.value).toString(),(runID == null ? null : runID),type))
            }
            if(comparisonPerc == 'Pcnt90')
            {
                selectedSample = ElasticSearchUtils.getSampleValueForResponseTimeAndPayLoad(configReader,it.key,(it.percentile.values.'90.0').toString(),(runID == null ? null : runID),type)
                txnMetrics.put("Pcnt90",Float.parseFloat(selectedSample.totalPageLoadTime.toString()).round())
                txnMetrics.put("HttpCount",selectedSample.resourceCount)
                txnMetrics.put("Payload",selectedSample.resourceSize)
            }
            else
            {
                txnMetrics.put("Pcnt90",ElasticSearchUtils.getSampleValueForResponseTime(configReader,it.key,(it.percentile.values.'90.0').toString(),(runID == null ? null : runID),type))
            }

            if(comparisonPerc == 'Pcnt95')
            {
                selectedSample = ElasticSearchUtils.getSampleValueForResponseTimeAndPayLoad(configReader,it.key,(it.percentile.values.'95.0').toString(),(runID == null ? null : runID),type)
                txnMetrics.put("Pcnt95",Float.parseFloat(selectedSample.totalPageLoadTime.toString()).round())
                txnMetrics.put("HttpCount",selectedSample.resourceCount)
                txnMetrics.put("Payload",selectedSample.resourceSize)
            }
            else
            {
                txnMetrics.put("Pcnt95",ElasticSearchUtils.getSampleValueForResponseTime(configReader,it.key,(it.percentile.values.'95.0').toString(),(runID == null ? null : runID),type))
            }

            txnSamples.put(it.key,txnMetrics.clone())
        }
        return txnSamples

    }

    static def getSampleValueForResponseTimeAndPayLoad(def configReader,String transactionName,String sampleValue,String runID = null,String type= null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            query.append('{"_source": ["totalPageLoadTime","resourceCount","resourceSize"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        }

        if(configReader.AnalysisType == 'Time')
        {
            if(type == 'Current')
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
                query.append('{"_source": ["totalPageLoadTime","resourceCount","resourceSize"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            }
            else
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
                query.append('{"_source": ["totalPageLoadTime","resourceCount","resourceSize"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            }
        }

        log.debug 'getSampleForDetailedAnalysis query : ' +  query

        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)

        if (response_body?.hits?.hits.size() <= 0)
        {
            query.setLength(0)
            if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                query.append('{"_source": ["totalPageLoadTime","resourceCount","resourceSize"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
            }
            if(configReader.AnalysisType == 'Time')
            {
                if(type == 'Current')
                {
                    //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                    query.append('{"_source": ["totalPageLoadTime","resourceCount","resourceSize"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
                else
                {
                    //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                    query.append('{"_source": ["totalPageLoadTime","resourceCount","resourceSize"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
            }

            response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        }
        return response_body.hits.hits[0]."_source"
    }

    static def getSampleValueForResponseTime(def configReader,String transactionName,String sampleValue,String runID = null,String type= null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        }

        if(configReader.AnalysisType == 'Time')
        {
            if(type == 'Current')
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
                query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            }
            else
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
                query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            }
        }

        log.debug 'getSampleForDetailedAnalysis query : ' +  query

        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)

        if (response_body?.hits?.hits.size() <= 0)
        {
            query.setLength(0)
            if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
            }
            if(configReader.AnalysisType == 'Time')
            {
                if(type == 'Current')
                {
                    //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                    query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
                else
                {
                    //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                    query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
            }

            response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        }
        return Float.parseFloat(response_body.hits.hits[0]."_source".totalPageLoadTime.toString()).round()
    }

    static def getSampleForDetailedAnalysis(def configReader,String transactionName,String sampleValue,String runID = null,String type = null)
    {

        def response_body = null
        StringBuilder query = new StringBuilder()
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            //query.append('{"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            query.append('{"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        }

        if(configReader.AnalysisType == 'Time')
        {
            if(type == 'Current')
            {
                //query.append('{"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
                query.append('{"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            }
            else
            {
                //query.append('{"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
                query.append('{"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            }

        }

        log.debug 'getSampleForDetailedAnalysis query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)

        if (response_body?.hits?.hits.size() <= 0)
        {
            query.setLength(0)
            if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
            {
                //query.append('{"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                query.append('{"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.RunID.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
            }
            if(configReader.AnalysisType == 'Time')
            {
                if(type == 'Current')
                {
                    //query.append('{"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                    query.append('{"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
                else
                {
                    //query.append('{"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                    query.append('{"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
            }


            response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        }
        return response_body
    }

    static def extractAllSamples(def configReader,String runID = null,String txnName,String type = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if (runID == null)
        {
            runID = configReader.RunID
        }

        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            //query.append('{"query":{"filtered":{"filter":{"and":[').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"TransactionName": "').append(txnName).append('"}},{"term": {"RunID": "').append(runID).append('"}}]}}}}')
            query.append('{"query":{"bool":{"filter":[').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"TransactionName": "').append(txnName).append('"}},{"term": {"RunID": "').append(runID).append('"}}]}}}')
        }

        if(configReader.AnalysisType == 'Time')
        {
            //query.append('{"query":{"filtered":{"filter":{"and":[').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y',type)).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}}}')
            query.append('{"query":{"bool":{"filter":[').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y',type)).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}}')
        }

        log.debug 'extractAllSamples query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        return response_body
    }

    static def updateBaselineRunID(def configReader,String runID)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}}}')
        query.append('{"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}}')

        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.CONFIGSEARCH,query)

        //Update BaselineRunID to CurrentRunID

        query.setLength(0)
        query.append('{"doc":{"BaselineRunID":').append(runID).append('}}')
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.CONFIGTABLE_WS+ response_body.hits.hits[0]."_id" + '/_update',query)
        if (response_body.'_version' > 1)
        {
            return 1
        }
        else
        {
            return 0
        }
    }

    static def extractSampleForDetailedAnalysis(def configReader,String transactionName,String runID = null,String type = null,String sampleValue,boolean dom = false)
    {

        log.debug 'START extractSampleForDetailedAnalysis : ' + transactionName + ' : ' + sampleValue
        def response_body = null
        StringBuilder query = new StringBuilder()

        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            if(dom)
            {
                query.append('{"query":{"bool":{"filter":[{"term":{"totalPageLoadTime":').append(sampleValue).append('}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append(runID).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}},"NumOfNonStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}},"NumOfCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}},"NumOfNonCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}},"NumOfResrcRedirectCount":{"filter":{"bool":{"must_not":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcNotRedirectedCount":{"filter":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}},"NumOfNonResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}},"HostAggregation":{"filter":{"match_all":{}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
            }
            else
            {
                query.append('{"_source":{"exclude":["dom"]},"query":{"bool":{"filter":[{"term":{"totalPageLoadTime":').append(sampleValue).append('}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"RunID":"').append(runID).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}},"NumOfNonStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}},"NumOfCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}},"NumOfNonCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}},"NumOfResrcRedirectCount":{"filter":{"bool":{"must_not":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcNotRedirectedCount":{"filter":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}},"NumOfNonResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}},"HostAggregation":{"filter":{"match_all":{}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
            }

        }
        if(configReader.AnalysisType == 'Time')
        {
            if (type == 'Current')
            {
                if (dom)
                {
                    query.append('{"query":{"bool":{"filter":[{"term":{"totalPageLoadTime":').append(sampleValue).append('}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader, 'Y', 'Current')).append(']}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}},"NumOfNonStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}},"NumOfCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}},"NumOfNonCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}},"NumOfResrcRedirectCount":{"filter":{"bool":{"must_not":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcNotRedirectedCount":{"filter":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}},"NumOfNonResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}},"HostAggregation":{"filter":{"match_all":{}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
                else
                {
                    query.append('{"_source":{"exclude":["dom"]},"query":{"bool":{"filter":[{"term":{"totalPageLoadTime":').append(sampleValue).append('}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader, 'Y', 'Current')).append(']}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}},"NumOfNonStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}},"NumOfCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}},"NumOfNonCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}},"NumOfResrcRedirectCount":{"filter":{"bool":{"must_not":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcNotRedirectedCount":{"filter":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}},"NumOfNonResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}},"HostAggregation":{"filter":{"match_all":{}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
            }
            else
            {
                if (dom)
                {
                    query.append('{"query":{"bool":{"filter":[{"term":{"totalPageLoadTime":').append(sampleValue).append('}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader, 'Y', 'Baseline')).append(']}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}},"NumOfNonStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}},"NumOfCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}},"NumOfNonCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}},"NumOfResrcRedirectCount":{"filter":{"bool":{"must_not":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcNotRedirectedCount":{"filter":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}},"NumOfNonResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}},"HostAggregation":{"filter":{"match_all":{}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
                else
                {
                    query.append('{"_source":{"exclude":["dom"]},"query":{"bool":{"filter":[{"term":{"totalPageLoadTime":').append(sampleValue).append('}},{"term":{"TransactionName":"').append(transactionName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader, 'Y', 'Baseline')).append(']}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}},"NumOfNonStaticResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}},"NumOfCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}},"NumOfNonCachedResrc":{"filter":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}},"NumOfResrcRedirectCount":{"filter":{"bool":{"must_not":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcNotRedirectedCount":{"filter":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}},"NumOfResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}},"NumOfNonResrcImages":{"filter":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}},"HostAggregation":{"filter":{"match_all":{}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                }
            }
        }

        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        log.debug 'extractSampleForDetailedAnalysis query : ' +  query
        return response_body
    }

    static def getAggregatedResponseTimeForCBT(def configReader,String runID = null,String txnName = null,String type = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            if (txnName == null)
            {
                //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append(runID).append('"}}]}}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append(runID).append('"}}]}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
            }
            else
            {
                //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append(runID).append('"}}').append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append(runID).append('"}}').append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
            }
        }

        if(configReader.AnalysisType == 'Time')
        {
            if(type == 'Current')
            {
                if (txnName == null)
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                }
                else
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current'))..append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current'))..append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                }
            }
            else
            {
                if (txnName == null)
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                }
                else
                {
                    //query.append('{"size" : 0,"query": {"filtered": {"filter": {"and": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                    query.append('{"size" : 0,"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(',{"term": {"TransactionName": "').append(txnName).append('"}}]}},"aggs":{"Transaction":{"terms":{"field":"TransactionName","size":10000},"aggs":{"browser":{"terms":{"field":"ResolvedUA","size":10000},"aggs":{"percentile":{"percentiles":{"field":"totalPageLoadTime","percents":[90,95]}},"average":{"avg":{"field":"totalPageLoadTime"}}}}}}}}')
                }
            }

        }

        log.debug 'getAggregatedResponseTimeForCBT query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)

        def txnMetricsCalc = response_body.aggregations.Transaction.buckets
        def txnSamples = [:]
        def txnMetrics = [:]
        def txnMetricsList = []


        txnMetricsCalc.each { item ->
            if(item.browser.buckets.size() > 1)
            {
                for (int i=0; i < item.browser.buckets.size(); i++)
                {
                    txnMetrics.put("UA",item.browser.buckets[i].key)
                    txnMetrics.put("Count",item.browser.buckets[i].doc_count)
                    txnMetrics.put("Average",ElasticSearchUtils.getSampleValueForResponseTimeForCBT(configReader,item.key,item.browser.buckets[i].key,(item.browser.buckets[i].average.value).toString(),(runID == null ? null : runID),type))
                    txnMetrics.put("Pcnt90",ElasticSearchUtils.getSampleValueForResponseTimeForCBT(configReader,item.key,item.browser.buckets[i].key,(item.browser.buckets[i].percentile.values.'90.0').toString(),(runID == null ? null : runID),type))
                    txnMetrics.put("Pcnt95",ElasticSearchUtils.getSampleValueForResponseTimeForCBT(configReader,item.key,item.browser.buckets[i].key,(item.browser.buckets[i].percentile.values.'95.0').toString(),(runID == null ? null : runID),type))
                    txnMetricsList.add(txnMetrics.clone())
                }
                txnSamples.put(item.key,txnMetricsList.clone())
                txnMetricsList.clear()
            }
        }

        return txnSamples
    }

    static def getSampleValueForResponseTimeForCBT(def configReader,String transactionName,String agentName,String sampleValue,String runID = null,String type = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        }

        if(configReader.AnalysisType == 'Time')
        {
            //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
            query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        }

        log.debug 'getSampleValueForResponseTimeForCBT query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)

        if (response_body?.hits?.hits.size() <= 0)
        {
            query.setLength(0)
            if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
            }
            if(configReader.AnalysisType == 'Time')
            {
                //query.append('{"fields": ["totalPageLoadTime"],"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
                query.append('{"_source": ["totalPageLoadTime"],"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"sort":[{"totalPageLoadTime":{"order":"desc"}}]}')
            }
            response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        }
        //println query
        return Float.parseFloat(response_body.hits.hits[0].sort[0].toString()).round()
    }

    static def getSampleForDetailedAnalysisForCBT(def configReader,String transactionName,String agentName,String sampleValue,String runID = null,String type = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
        {
            //query.append('{"_source":{"exclude":["dom"]},"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}}}')
            query.append('{"_source":{"exclude":["dom"]},"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}}')
        }
        if(configReader.AnalysisType == 'Time')
        {
            //query.append('{"_source":{"exclude":["dom"]},"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}}}')
            query.append('{"_source":{"exclude":["dom"]},"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"gte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}}')
        }
        log.debug 'getSampleForDetailedAnalysisForCBT query : ' +  query

        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        //println response_body.hits.hits.size()
        if (response_body.hits.hits.size() <= 0)
        {
            query.setLength(0)
            if(configReader.AnalysisType == 'Run' || configReader.AnalysisType == 'Transaction')
            {
                //query.append('{"_source":{"exclude":["dom"]},"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}}}')
                query.append('{"_source":{"exclude":["dom"]},"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},{"term":{"RunID":"').append((runID == null ? configReader.CurrentRun.toString() : runID)).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}}}')
            }
            if(configReader.AnalysisType == 'Time')
            {
                //query.append('{"_source":{"exclude":["dom"]},"query":{"filtered":{"filter":{"and":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}}}')
                query.append('{"_source":{"exclude":["dom"]},"query":{"bool":{"filter":[{"range":{"totalPageLoadTime":{"lte":').append(sampleValue).append('}}},{"term":{"TransactionName":"').append(transactionName).append('"}},{"term":{"ResolvedUA":"').append(agentName).append('"}},').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}}')
            }
            response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        }

        return response_body.hits.hits[0]."_source"
    }

    static def extractSampleForDetailedAnalysisUsingID(def configReader,String DocId)
    {

        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"_source":{"exclude": ["dom"]},"query":{"filtered":{"filter":{"term": {"_id": "').append(DocId).append('"}}}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}}},"NumOfNonStaticResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}}},"NumOfCachedResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}}},"NumOfNonCachedResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}}},"NumOfResrcRedirectCount":{"filter":{"not":{"query":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}}}},"NumOfResrcNotRedirectedCount":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}}},"NumOfResrcImages":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}}},"NumOfNonResrcImages":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}}},"HostAggregation":{"filter":{"query":{"match_all": {}}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        query.append('{"_source":{"exclude": ["dom"]},"query":{"bool":{"filter":{"term": {"_id": "').append(DocId).append('"}}}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}}},"NumOfNonStaticResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}}},"NumOfCachedResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}}},"NumOfNonCachedResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}}},"NumOfResrcRedirectCount":{"filter":{"not":{"query":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}}}},"NumOfResrcNotRedirectedCount":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}}},"NumOfResrcImages":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}}},"NumOfNonResrcImages":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}}},"HostAggregation":{"filter":{"query":{"match_all": {}}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)
        log.debug 'extractSampleForDetailedAnalysisUsingID : ' +  DocId
        return response_body
    }

    static def execTimeOfRunID(def configReader,def runID,def timeline=null,def type=null,def timezone = null)
    {
        def timeArr = []
        StringBuilder release = new StringBuilder()
        StringBuilder build = new StringBuilder()
        StringBuilder query = new StringBuilder()
        StringBuilder execution = new StringBuilder()
        try
        {
            if(runID != '0')
            {
                if(timeline == null)
                {
                    //query.append('{"size":0,"query":{"filtered":{"filter":{"and":[').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append(runID).append('"}}]}}},"aggs":{"MinTime":{"min":{"field":"StartTime"}},"MaxTime":{"max":{"field":"StartTime"}},"BuildNumber":{"terms":{"field":"BuildNumber"}},"Release":{"terms":{"field":"Release"}}}}')
                    query.append('{"size":0,"query":{"bool":{"filter":[').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(',{"term": {"RunID": "').append(runID).append('"}}]}},"aggs":{"MinTime":{"min":{"field":"StartTime"}},"MaxTime":{"max":{"field":"StartTime"}},"BuildNumber":{"terms":{"field":"BuildNumber"}},"Release":{"terms":{"field":"Release"}}}}')
                }
                else
                {
                    if(type == 'Current')
                    {
                        //query.append('{"size":0,"query":{"filtered":{"filter":{"and":[').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}}},"aggs":{"MinTime":{"min":{"field":"StartTime"}},"MaxTime":{"max":{"field":"StartTime"}},"BuildNumber":{"terms":{"field":"BuildNumber"}},"Release":{"terms":{"field":"Release"}}}}')
                        query.append('{"size":0,"query":{"bool":{"filter":[').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Current')).append(']}},"aggs":{"MinTime":{"min":{"field":"StartTime"}},"MaxTime":{"max":{"field":"StartTime"}},"BuildNumber":{"terms":{"field":"BuildNumber"}},"Release":{"terms":{"field":"Release"}}}}')
                    }
                    else
                    {
                        //query.append('{"size":0,"query":{"filtered":{"filter":{"and":[').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}}},"aggs":{"MinTime":{"min":{"field":"StartTime"}},"MaxTime":{"max":{"field":"StartTime"}},"BuildNumber":{"terms":{"field":"BuildNumber"}},"Release":{"terms":{"field":"Release"}}}}')
                        query.append('{"size":0,"query":{"bool":{"filter":[').append(ElasticSearchUtils.baseFilterQuery(configReader,'Y','Baseline')).append(']}},"aggs":{"MinTime":{"min":{"field":"StartTime"}},"MaxTime":{"max":{"field":"StartTime"}},"BuildNumber":{"terms":{"field":"BuildNumber"}},"Release":{"terms":{"field":"Release"}}}}')
                    }

                }

                log.debug 'execTimeOfRunID query : ' + query

                def response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl, GlobalConstants.STATSSEARCH, query)
                int cnt = 0
                if(response_body.aggregations.BuildNumber.buckets.size > 0)
                {
                    response_body.aggregations.BuildNumber.buckets.each{it ->
                        build.append(it.key)
                        if(cnt != 0)
                        {
                            build.append('|')
                            cnt = cnt + 1
                        }
                    }
                }
                else
                {
                    build.append('')
                }

                cnt == 0
                if(response_body.aggregations.Release.buckets.size > 0)
                {
                    response_body.aggregations.Release.buckets.each{it ->
                        release.append(it.key)
                        if(cnt != 0)
                        {
                            release.append('|')
                            cnt = cnt + 1
                        }
                    }
                }
                else
                {
                    release.append('')
                }


                execution.append(CommonUtils.getStringForTimeStamp(response_body.aggregations.MinTime.value,timezone)).append(' - ').append(CommonUtils.getStringForTimeStamp(response_body.aggregations.MaxTime.value,timezone))

            }

        }
        catch(Exception e)
        {
            e.printStackTrace()
            execution.append('Time Not Available')
        }

        return [execution,release,build]
    }

    static def executionTimeOfRunID(def clientName,def projectName,def scenarioName,def runID,def timezone='UTC')
    {
        //def timeArr = []
        StringBuilder release = new StringBuilder()
        StringBuilder build = new StringBuilder()
        StringBuilder query = new StringBuilder()
        StringBuilder execution = new StringBuilder()
        try
        {
            if(clientName != null && projectName != null && scenarioName != null && runID != null)
            {
                query.append('{"size":0,"query":{"bool":{"filter":[{"term": {"ClientName": "').append(clientName).append('"}},').append('{"term": {"ProjectName": "').append(projectName).append('"}},').append('{"term": {"Scenario": "').append(scenarioName).append('"}},').append('{"term": {"RunID": "').append(runID).append('"}}]}},"aggs":{"MinTime":{"min":{"field":"StartTime"}},"MaxTime":{"max":{"field":"StartTime"}},"BuildNumber":{"terms":{"field":"BuildNumber"}},"Release":{"terms":{"field":"Release"}}}}')

                log.debug 'execTimeOfRunID query : ' + query

                def response_body = ElasticSearchUtils.elasticSearchPOST(GlobalConstants.ES_URL, GlobalConstants.STATSSEARCH, query)


                if(response_body.aggregations.BuildNumber.buckets.size > 0)
                {
                    build.append(response_body.aggregations.BuildNumber.buckets[0].key == '' ? 'NA' : response_body.aggregations.BuildNumber.buckets[0].key)
                }
                else
                {
                    build.append('NA')
                }

                if(response_body.aggregations.Release.buckets.size > 0)
                {
                    release.append(response_body.aggregations.Release.buckets[0].key =='' ? 'NA' : response_body.aggregations.Release.buckets[0].key)
                }
                else
                {
                    release.append('NA')
                }
                execution.append(CommonUtils.getStringForTimeStamp(response_body.aggregations.MinTime.value,timezone)).append(' - ').append(CommonUtils.getStringForTimeStamp(response_body.aggregations.MaxTime.value,timezone))

            }

        }
        catch(Exception e)
        {
            execution.append('NA')
        }

        return [execution,release,build]
    }


    static def getAvailableClientName(String esUrl)
    {
        def clientList = []
        StringBuilder query = new StringBuilder()
        query.append('{"size": 0,"aggs": {"langs": {"terms": {"field": "ClientName","size":100}}}}')
        log.debug('getUniqueClients Query :' + query)
        ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.CONFIGSEARCH,query).aggregations?.langs.buckets.each{it ->
                clientList.add('"' + it.key + '"')
            }


        return clientList

    }

    static def getAvailableUserName(String esUrl)
    {
        def userList = []
        StringBuilder query = new StringBuilder()
        query.append('{"size": 0,"aggs": {"langs": {"terms": {"field": "UserName","size":100}}}}')
        log.debug('getUniqueClients Query :' + query)
        ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.USERSEARCH,query).aggregations?.langs.buckets.each{it ->
                clientList.add('"' + it.key + '"')
            }


        return userList

    }

    static def getUniqueClients(String esURL)
    {
        def clientList = []
        StringBuilder query = new StringBuilder()
        query.append('{"size": 0,"aggs": {"langs": {"terms": {"field": "ClientName","size":100}}}}')
        log.debug('getUniqueClients Query :' + query)
        ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.CONFIGSEARCH,query).aggregations?.langs.buckets.each{it ->
            clientList.add(it.key)
        }
        return clientList
    }

    static def extractProjectDetails(String esURL,def clientName)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        query.append('{"size" : 0,query": {"bool": {"filter": [{"term":{"ClientName":"').append(clientName.toLowerCase()).append('"}}]}},"aggs":{"ProjectName":{"terms":{"field":"ProjectName","size":100}}}}')
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL, GlobalConstants.CONFIGSEARCH,query)
        return response_body
    }

    static def extractScenarioDetails(String esURL,def projectName)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        query.append('{"size" : 0,"query": {"bool": {"filter": [{"term":{"ProjectName":"').append(projectName.toLowerCase()).append('"}}]}},"aggs":{"Scenario":{"terms":{"field":"Scenario","size":100}}}}')
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL, GlobalConstants.CONFIGSEARCH,query)
        return response_body
    }

    static def getClientDetails(String esURL,int noOfDays,def clientList)
    {
        def response_body = null

        StringBuilder query = new StringBuilder()

        query.append('{"size":0,"query":{"bool":{"should":[')
        int clientSize = clientList.size()
        for (int i=0;i< clientSize;i++)
        {
            if(i != clientSize-1)
            {
                query.append('{"term":{"ClientName":"').append(clientList[i].toLowerCase()).append('"}},')
            }
            else
            {
                query.append('{"term":{"ClientName":"').append(clientList[i].toLowerCase()).append('"}}')
            }
        }
        //query.append(']},{"range":{"StartTime":{"gte":"now-').append(noOfDays).append('d/d","lt":"now+1h"}}}]}}},"aggs":{"ClientName":{"terms":{"field":"ClientName","size":100},"aggs":{"ProjectName":{"terms":{"field":"ProjectName","size":100},"aggs":{"Scenario":{"terms":{"field":"Scenario","size":100},"aggs":{"RunID":{"terms":{"field":"RunID","size":100}}}}}}}}}}')

        query.append('],"minimum_should_match" : 1,"filter" :{"range":{"creationTimestamp":{"gte":"').append((System.currentTimeSeconds() - (noOfDays * 24 * 60 * 60)) * 1000).append('","lt":"').append(System.currentTimeMillis()).append('"}}}}},"aggs":{"ClientName":{"terms":{"field":"ClientName","size":100},"aggs":{"ProjectName":{"terms":{"field":"ProjectName","size":100},"aggs":{"Scenario":{"terms":{"field":"Scenario","size":100}}}}}}}}')


        //println query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL, GlobalConstants.CONFIGSEARCH,query)
        return response_body
    }

    static def extractRunDetails(String esURL,int noOfDays,String clientName,String projectName,String scenario)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        /*
        query.append('{"size":0,"query":{"bool":{"should":[{"term":{"ClientName":"').append(clientName).append('"}},{"term":{"ProjectName":"').append(projectName).append('"}},{"term":{"Scenario":"').append(scenario).append('"}}')
        query.append('],"minimum_should_match" : 1,"filter" :{"range":{"StartTime":{"gte":"now-').append(noOfDays).append('d/d","lt":"now+1h"}}}}},"aggs":{"RunIDS":{"terms":{"field": "RunID","size": 10000}}}}')
        */

        query.append('{"size":0,"query":{"bool":{"filter":[{"term":{"ClientName":"').append(clientName).append('"}},{"term":{"ProjectName":"').append(projectName).append('"}},{"term":{"Scenario":"').append(scenario).append('"}}')
        query.append(',{"range":{"StartTime":{"gte":"now-').append(noOfDays).append('d/d","lt":"now+1h"}}}]}},"aggs":{"RunIDS":{"terms":{"field": "RunID","size": 10000}}}}')

        response_body = ElasticSearchUtils.elasticSearchPOST(esURL, GlobalConstants.STATSSEARCH,query)

        return response_body
    }

    static def getRules(String esUrl,String ruleCategory = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        query.append('{"query":{"match_all":{}}}')
        log.debug 'getRules query : ' +  query
        if(GlobalConstants.getESVersion() == '5')
        {
            response_body = ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.RULESSEARCH,query)
        }
        else
        {
            response_body = ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.ES6RULESSEARCH,query)
        }

        log.debug 'getRules Response : ' + response_body

        if(response_body != null && ruleCategory == null)
        {
            return response_body.hits.hits[0]."_source"
        }
        else
        {
            return response_body.hits.hits[0]."_source"."$ruleCategory"
        }


    }

    static def extractLoginStatus(String esURL,String userName)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"query": {"filtered": {"filter": {"and": [{"term": {"UserName": "').append(userName).append('"}},{"term": {"Password": "').append(password).append('"}}]}}}}')
        //query.append('{"query": {"bool": {"filter": [{"term": {"UserName": "').append(userName).append('"}},{"term": {"Password": "').append(password).append('"}}]}}}')
        query.append('{"query": {"bool": {"filter": [{"term": {"UserName": "').append(userName).append('"}}]}}}')
        log.debug 'extractLoginStatus query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.USERSEARCH,query)
        return response_body
    }

    static def IsUserExists(String esURL,String userName)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"query": {"filtered": {"filter": {"and": [{"term": {"UserName": "').append(userName).append('"}}]}}}}')
        query.append('{"query": {"bool": {"filter": [{"term": {"UserName": "').append(userName).append('"}}]}}}')
        log.debug 'IsUserExists query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.USERSEARCH,query)
        return response_body
    }

    static def IsClientLicenseExists(String esURL,String clientName)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        query.append('{"query": {"bool": {"filter": [{"term": {"ClientName": "').append(clientName).append('"}}]}}}')
        log.debug 'IsClientLicenseExists query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.CONFIGSEARCH,query)
        return response_body
    }


    static def createUser(String esURL,def query)
    {
        def response_body = null
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.USERTABLE,query)
        return response_body
    }

    static def extractAllLicense(String esURL,String ClientName = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(ClientName == null)
        {
            query.append('{"_source":["ClientName","SRNumber","licenseKey"],"query":{"constant_score":{"filter":{"range":{"subcEndDate":{"gte": "now"}}}}},"size": 10000}')
        }
        else
        {
            query.append('{"_source": ["ClientName","SRNumber","licenseKey"],"query": {"bool": {"filter": [{"term": {"ClientName": "').append(ClientName).append('"}},{"range":{"subcEndDate":{"gte": "now"}}}]}},"size": 10000}')
        }

        log.debug 'extractAllLicense query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.LICENSESEARCH,query)
        log.debug 'extractAllLicense : ' +  response_body
        return response_body
    }


    static def extractMarkDetailsUsingID(def configReader,String DocId)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        //query.append('{"_source":{"exclude": ["dom"]},"query":{"filtered":{"filter":{"term": {"_id": "').append(DocId).append('"}}}},"aggs":{"Resources":{"nested":{"path":"Resources"},"aggs":{"Total_duration":{"sum":{"field":"Resources.duration"}},"Total_dnsLookupTime":{"sum":{"field":"Resources.dnsLookupTime"}},"Total_tcpConnectTime":{"sum":{"field":"Resources.tcpConnectTime"}},"Total_redirectTime":{"sum":{"field":"Resources.redirectTime"}},"Total_cacheFetchTime":{"sum":{"field":"Resources.cacheFetchTime"}}}},"Filter":{"nested":{"path":"Resources"},"aggs":{"NumOfStaticResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"true"}}]}}}},"NumOfNonStaticResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsStaticResrc":"false"}}]}}}},"NumOfCachedResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsCached":"true"}}]}}}},"NumOfNonCachedResrc":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsCached":"false"}}]}}}},"NumOfResrcRedirectCount":{"filter":{"not":{"query":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}}}},"NumOfResrcNotRedirectedCount":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.redirectTime":0}}]}}}},"NumOfResrcImages":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsImage":"true"}}]}}}},"NumOfNonResrcImages":{"filter":{"query":{"bool":{"must":[{"term":{"Resources.IsImage":"false"}},{"term":{"Resources.IsStaticResrc":"true"}}]}}}},"HostAggregation":{"filter":{"query":{"match_all": {}}},"aggs":{"MetricForEveryHost":{"terms":{"field":"Resources.HostName"},"aggs":{"TotalDurationByHost":{"sum":{"field":"Resources.duration"}},"TotaldnsLookupTimeByHost":{"sum":{"field":"Resources.dnsLookupTime"}},"TotaltcpConnectTimeByHost":{"sum":{"field":"Resources.tcpConnectTime"}},"TotalredirectTimeByHost":{"sum":{"field":"Resources.redirectTime"}}}}}}}}},"sort":[{"totalPageLoadTime":{"order":"asc"}}]}')
        query.append('{"_source":{"exclude": ["ProjectName","parentDocId","RunTime","StartTime","url","Scenario","ClientName","BuildNumber","RunID","Release","TransactionName"]},"query":{"bool":{"filter":{"term": {"parentDocId": "').append(DocId).append('"}}}}}')
        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.MARKSEARCH,query)
        log.debug 'extractMarkDetailsUsingID : ' +  DocId
        return response_body
    }

    static def uniqueTransactionList(def configReader)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()

        query.append('{"query": {"bool": {"filter": [').append(ElasticSearchUtils.baseFilterQuery(configReader)).append(']}},"aggs": {"langs": {"terms": {"field": "TransactionName","size":100}}}}')

        response_body = ElasticSearchUtils.elasticSearchPOST(configReader.esUrl,GlobalConstants.STATSSEARCH,query)

        return response_body
    }

    static def deleteStats(String esURL,String ClientName,String ProjectName,String Scenario,String RunID = null)
    {
        def response_body = null
        StringBuilder query = new StringBuilder()
        if(RunID == null)
        {
            query.append('{"query":{"bool":{"must":[{"match":{"ClientName":"').append(ClientName).append('"}},{"match":{"ProjectName":"').append(ProjectName).append('"}},{"match":{"Scenario":"').append(Scenario).append('"}}]}}}')
        }
        else
        {
            query.append('{"query":{"bool":{"must":[{"match":{"ClientName":"').append(ClientName).append('"}},{"match":{"RunID":').append(Long.parseLong(RunID)).append('}},{"match":{"ProjectName":"').append(ProjectName).append('"}},{"match":{"Scenario":"').append(Scenario).append('"}}]}}}')
        }


        log.debug 'deleteStats query : ' +  query
        response_body = ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.STATSDELETE,query)
        if(response_body?.deleted > 0 && RunID == null)
        {
            ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.CONFIGDELETE,query)
            ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.BASELINEDELETE,query)
            ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.ANALYSISDELETE,query)
            ElasticSearchUtils.elasticSearchPOST(esURL,GlobalConstants.MARKDELETE,query)
        }

        log.debug 'deleteStats : ' +  response_body
        return response_body
    }


}