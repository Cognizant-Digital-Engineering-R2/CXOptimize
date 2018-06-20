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

using System;
using System.Net;
using System.Text;
using System.IO;
using System.Collections.Generic;
using System.Net.Security;

namespace com.cognizant.pace.CXOptimize.Collector.utils
{
    public class HTTPUtils
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        public static String HttpGet(String urlStr)
        {
            return HttpRequest(urlStr, "", "", "GET");
        }

        public static String HttpPost(String urlStr, String contentType, String postData)
        {
            return HttpRequest(urlStr, contentType, postData, "POST");
        }

        private static string HttpRequest(String urlStr, String contentType, String postData, string method)
        {
            try
            {
                HttpWebRequest httpReq = (HttpWebRequest)WebRequest.Create(urlStr);

                if (urlStr.Contains("https://"))
                {
                    System.Net.ServicePointManager.ServerCertificateValidationCallback +=
                        (object sender, System.Security.Cryptography.X509Certificates.X509Certificate certificate,
                                                System.Security.Cryptography.X509Certificates.X509Chain chain,
                                                System.Net.Security.SslPolicyErrors sslPolicyErrors) =>
                        { return true; };


                }

                if (method == "POST")
                {
                    var data = Encoding.ASCII.GetBytes(postData);
                    httpReq.ContentType = contentType;
                    httpReq.Method = "POST";
                    httpReq.ContentLength = data.Length;

                    using (var stream = httpReq.GetRequestStream())
                    {
                        stream.Write(data, 0, data.Length);
                    }
                }

                using (HttpWebResponse httpRes = (HttpWebResponse)httpReq.GetResponse())
                {
                    if (httpRes.StatusCode == HttpStatusCode.OK)
                    {
                        Stream receiveStream = httpRes.GetResponseStream();

                        using (StreamReader readStream = new StreamReader(receiveStream, Encoding.UTF8))
                            return readStream.ReadToEnd();
                    }

                }
            }
            catch (Exception ex)
            {
                return ex.Message;
            }
            return null;
        }




        public static Dictionary<String, Object> callService(String urlStr, String method, String body, String authToken)
        {
            log.Debug("Calling service:" + urlStr + " method:" + method + " body:" + body + " token : " + authToken);
            Dictionary<String, Object> result = new Dictionary<String, Object>();
            Dictionary<String, Object> resultToken = new Dictionary<String, Object>();
            try
            {
                HttpWebRequest httpReq = (HttpWebRequest)WebRequest.Create(urlStr);
                if (urlStr.ToLower().Contains("https"))
                {
                    // Handle https Certificate //
                    System.Net.ServicePointManager.ServerCertificateValidationCallback +=
                        (object sender, System.Security.Cryptography.X509Certificates.X509Certificate certificate,
                                                System.Security.Cryptography.X509Certificates.X509Chain chain,
                                                System.Net.Security.SslPolicyErrors sslPolicyErrors) =>
                        {
                            return true;
                        };
                }

                httpReq.ContentType = "application/json";

                if (authToken != null)
                {
                    httpReq.Headers.Add("Authorization", authToken);
                }
                log.Debug("Calling service:" + urlStr + " with token:" + authToken);

                if (method == "POST")
                {
                    var data = Encoding.ASCII.GetBytes(body);
                    httpReq.Method = "POST";
                    httpReq.ContentLength = data.Length;

                    using (var stream = httpReq.GetRequestStream())
                    {
                        stream.Write(data, 0, data.Length);
                    }
                }
                using (HttpWebResponse httpRes = (HttpWebResponse)httpReq.GetResponse())
                {
                    Stream receiveStream = httpRes.GetResponseStream();
                    if (httpRes.StatusCode == HttpStatusCode.OK)
                    {
                        result.Add("status", "pass");

                        if (authToken != null)
                        {
                            using (StreamReader readStream = new StreamReader(receiveStream, Encoding.UTF8))
                                result.Add("response", readStream.ReadToEnd());
                        }
                        else
                        {
                            result.Add("authToken", httpRes.GetResponseHeader("Authorization"));
                        }
                    }
                    else {
                        result.Add("status", "fail");
                        string response;
                        using (StreamReader readStream = new StreamReader(receiveStream, Encoding.UTF8))
                            response = readStream.ReadToEnd();
                        if (response.Contains("io.jsonwebtoken.ExpiredJwtException"))
                        {
                            log.Info("Unable to connect to url:" + urlStr + " due to expired Auth Token");
                            result.Add("reason", "JWTExpiry");
                        }
                        else {
                            log.Info("Unable to connect to url:" + urlStr + " due to status code:" + httpRes.StatusCode);
                            result.Add("reason", "Other");
                        }
                    }
                }



            }
            catch (Exception e)
            {
                log.Debug("Exception in call Service " + e);
                result.Add("reason", "Other");
            }
            return result;

        }

        public static String getResponseStream(HttpWebRequest httpReq)
        {
            try
            {
                // Buffer the result into a string
                using (HttpWebResponse httpRes = (HttpWebResponse)httpReq.GetResponse())
                {
                    if (httpRes.StatusCode == HttpStatusCode.OK)
                    {
                        Stream receiveStream = httpRes.GetResponseStream();

                        using (StreamReader readStream = new StreamReader(receiveStream, Encoding.UTF8))
                            return readStream.ReadToEnd();
                    }

                }
                return "";
            }
            catch 
            {
                return null;
            }
        }

    }
}