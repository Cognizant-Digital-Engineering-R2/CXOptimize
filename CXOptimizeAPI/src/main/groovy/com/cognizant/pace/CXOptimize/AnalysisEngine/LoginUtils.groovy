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

import org.slf4j.LoggerFactory

class LoginUtils
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LoginUtils.class)


    static def Login(String esUrl,String username,def password)
    {
        StringBuilder msg = new StringBuilder()
        def loginStatus = ElasticSearchUtils.extractLoginStatus(esUrl,username)

        if(loginStatus.hits.hits.size() > 0)
        {
            if(EncryptionUtils.getDecryptedPassword(loginStatus.hits.hits[0]."_source"?.Password) == EncryptionUtils.getDecryptedPassword(password.toString()))
            {
                msg.append('{"status" : true,')
                msg.append('"role" : "').append(loginStatus.hits.hits[0]."_source"?.Role == null ? "USER" : loginStatus.hits.hits[0]."_source"?.Role).append('",')
                msg.append('"reason" : "",')
                msg.append('"client" : "').append(loginStatus.hits.hits[0]."_source".Clients).append('"}')
            }
            else
            {
                msg.append('{"status" : false,')
                msg.append('"reason" : "Incorrect Password",')
                msg.append('"client" : "').append(loginStatus.hits.hits[0]."_source".Clients).append('"}')

            }
        }
        else
        {
            msg.append('{"status" : false,')
            msg.append('"reason" : "Incorrect Username",')
            msg.append('"client" : "NA"}')
        }
        return msg

    }


    static def CreateUser(String esUrl,def json)
    {
        StringBuilder msg = new StringBuilder()
        def jsonMap = JsonUtils.jsonToMap(json)
        def loginStatus = ElasticSearchUtils.IsUserExists(esUrl,jsonMap.UserName)
        log.debug "Login Status Exists : " + loginStatus
        if(loginStatus?.hits?.hits.size() > 0)
        {
            msg.append('{"status" : false,')
            msg.append('"reason" : "UserName already exists.Choose different name or add Client to existing user."}')
        }
        else
        {
            msg.append('{"UserName" :"').append(jsonMap.UserName).append('",')
            msg.append('"Password" :"').append(EncryptionUtils.getEncryptedPassword(jsonMap.Password)).append('",')
            msg.append('"Role" :"').append(jsonMap?.Role == null ? "USER" : jsonMap?.Role).append('",')
            if(jsonMap.SecurityQuestions?.a3 != null && jsonMap.SecurityQuestions?.a3 != '')
            {

                msg.append('"SecurityQuestions" :{"q1" : "').append(jsonMap.SecurityQuestions.q1).append('",')
                msg.append('"a1" : "').append(jsonMap.SecurityQuestions.a1).append('",')
                msg.append('"q2" : "').append(jsonMap.SecurityQuestions.q2).append('",')
                msg.append('"a2" : "').append(jsonMap.SecurityQuestions.a2).append('",')
                msg.append('"q3" : "').append(jsonMap.SecurityQuestions.q3).append('",')
                msg.append('"a3" : "').append(jsonMap.SecurityQuestions.a3).append('"},')
            }
            else
            {
                msg.append('"SecurityQuestions" :{"q1" : "","a1" : "","q2" : "","a2" : "","q3" : "","a3" : ""},')
            }

            boolean clientFlag = true

            if (jsonMap.Clients != null && jsonMap.Clients != '')
            {
                msg.append('"Clients" :"').append(jsonMap.Clients).append('",')
            }
            else
            {
                msg.append('"Clients" :"').append('",')
            }

            msg.append('"ContactEmail" :"').append(jsonMap.ContactEmail).append('",')
            msg.append('"updateTimeStamp" :').append(System.currentTimeMillis()).append(',')
            msg.append('"createTimeStamp" :').append(System.currentTimeMillis()).append('}')
            def response_body = ElasticSearchUtils.createUser(esUrl, msg)
            msg.setLength(0)
            if(response_body != null && response_body?.result == 'created')
            {
                msg.append('{"status" : true,')
                if (clientFlag)
                {
                    msg.append('"reason" : ""}')
                }

            }
            else
            {
                msg.append('{"status" : false,')
                msg.append('"reason" : "Failed to create user in data source"}')
            }
        }
        return msg

    }
    static def DeleteUser(String esUrl,def json)
    {
        StringBuilder msg = new StringBuilder()
        def jsonMap = JsonUtils.jsonToMap(json)
        def loginStatus = ElasticSearchUtils.extractLoginStatus(esUrl,jsonMap.UserName)
        if(loginStatus.hits.hits.size() > 0)
        {
            log.info 'Deleting User : ' + jsonMap.UserName
            log.info 'Elastic Search document : ' + loginStatus
            def deleteStatus = ElasticSearchUtils.elasticSearchDELETE(esUrl,GlobalConstants.USERTABLE_WS + loginStatus.hits.hits[0]."_id" + '/')
            msg.append('{"status" : true}')
        }
        else
        {
            msg.append('{"status" : false,')
            msg.append('"reason" : "Incorrect Username or User doesnt exists"}')
        }
        return msg

    }

    static def UpdatePassword(String esUrl,def json)
    {
        StringBuilder msg = new StringBuilder()
        StringBuilder query = new StringBuilder()
        def jsonMap = JsonUtils.jsonToMap(json)
        def loginStatus = ElasticSearchUtils.extractLoginStatus(esUrl,jsonMap.UserName)
        if(loginStatus.hits.hits.size() > 0)
        {
            if(EncryptionUtils.getDecryptedPassword(loginStatus.hits.hits[0]."_source"?.Password) == jsonMap.OldPassword)
            {
                query.append('{"doc":{"Password":"').append(EncryptionUtils.getEncryptedPassword(jsonMap.NewPassword)).append('"}}')

                def updateStatus = ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.USERTABLE_WS + loginStatus.hits.hits[0]."_id" + '/_update' ,query)
                if (updateStatus.result == 'updated')
                {
                    msg.append('{"status" : true}')
                }
                else
                {
                    msg.append('{"status" : false,')
                    msg.append('"reason" : "Password Update Failed"}')
                }
            }
            else
            {
                msg.append('{"status" : false,')
                msg.append('"reason" : "Incorrect Old Password"}')
            }
        }
        else
        {
            msg.append('{"status" : false,')
            msg.append('"reason" : "Incorrect Username"}')
        }
        return msg

    }

    static def ManageClientAccess(String esUrl,def json)
    {
        StringBuilder msg = new StringBuilder()
        StringBuilder query = new StringBuilder()
        boolean clientExists = false
        def updateStatus = null

        def jsonMap = JsonUtils.jsonToMap(json)
        def loginStatus = ElasticSearchUtils.IsUserExists(esUrl,jsonMap.UserName)
        println loginStatus
        log.debug "Login Status Exists : " + loginStatus
        if(loginStatus?.hits?.hits.size() > 0)
        {
            if(jsonMap?.ClientName == null || jsonMap?.ClientName == '')
            {
                query.append('{"doc":{"Clients":""}}')
                updateStatus = ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.USERTABLE_WS + loginStatus.hits.hits[0]."_id" + '/_update',query)
                if (updateStatus?.result  == 'updated' || updateStatus?.result  == 'noop')
                {
                    msg.append('{"status" : true}')
                }
                else
                {
                    msg.append('{"status" : false,')
                    msg.append('"reason" : "Client Update Failed"}')
                }
            }
            else
            {
                def isClientExists = ElasticSearchUtils.IsClientLicenseExists(esUrl,jsonMap?.ClientName)
                println isClientExists
                if(isClientExists?.hits?.hits?.size() > 0)
                {
                    def existingClient = loginStatus?.hits?.hits[0]?."_source"?.Clients
                    if(existingClient != '')
                    {
                        def clientList = existingClient.split(',')
                        if(clientList.contains(jsonMap.ClientName))
                        {
                            clientExists = true
                        }
                        else
                        {
                            query.append('{"doc":{"Clients":"').append(existingClient).append(',').append(jsonMap.ClientName).append('"}}')
                        }
                    }
                    else
                    {
                        query.append('{"doc":{"Clients":"').append(jsonMap.ClientName).append('"}}')
                    }

                    if(!clientExists)
                    {
                        updateStatus = ElasticSearchUtils.elasticSearchPOST(esUrl,GlobalConstants.USERTABLE_WS + loginStatus.hits.hits[0]."_id" + '/_update',query)
                        if (updateStatus?.result  == 'updated')
                        {
                            msg.append('{"status" : true}')
                        }
                        else
                        {
                            msg.append('{"status" : false,')
                            msg.append('"reason" : "Client Update Failed"}')
                        }

                    }
                    else
                    {
                        msg.append('{"status" : false,')
                        msg.append('"reason" : "ClientName already available to this user"}')
                    }

                }
                else
                {
                    msg.append('{"status" : false,')
                    msg.append('"reason" : "ClientName not available"}')

                }

            }

        }
        else
        {
            msg.append('{"status" : false,')
            msg.append('"reason" : "Incorrect Username"}')
        }
        return msg

    }


}