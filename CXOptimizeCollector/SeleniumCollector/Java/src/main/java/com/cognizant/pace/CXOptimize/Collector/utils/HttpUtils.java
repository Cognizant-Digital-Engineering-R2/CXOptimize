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

package com.cognizant.pace.CXOptimize.Collector.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.StringBuilderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
    private static Map<X509Certificate, Long> trusted = new HashMap();


    public static Map<String, Object> callService(String urlStr, String method, String body, String authToken)
    {
        LOGGER.debug("CXOP - Calling service:{} method:{} body:{} token : {}", urlStr,method,body,authToken);
        Map<String, Object> result = new HashMap<String, Object>();
        if (urlStr.toLowerCase().contains("http") || urlStr.toLowerCase().contains("https"))
        {
            HttpURLConnection conn;

            try
            {
                URL url = new URL(urlStr);

                if (urlStr.toLowerCase().contains("https"))
                {
                    // Handle https Certificate //
                    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                        public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {

                            //Checks if your certificate is provided to specific host
                            //Normally the certificate name and url that you use needs to be same for this to work
                            if (hostname == null) {
                                LOGGER.error("hostname is null");
                                return false;
                            }
                            return hostname.equalsIgnoreCase(sslSession.getPeerHost());

                            //Enable this section if your domain certificate name and url is not same
                            //But this will create security risk when
                            //return true;
                        }
                    });

                    // Create a trust manager that does not validate certificate chains
                    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                        public X509Certificate[] getAcceptedIssuers() {

                            //Comment below part in case if you get SSL exception
                            X509Certificate[] res =new X509Certificate[trusted.size()];
                            trusted.keySet().toArray(res);
                            return res;

                            //Enable this only if your are having SSL exception
                            //return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            if (certs != null) {
                                for (X509Certificate item : certs)
                                {
                                    if(!trusted.containsKey(item) || (System.currentTimeMillis() > (trusted.get(item) +3600*1000*24)))
                                    {
                                        trusted.put(item, System.currentTimeMillis());
                                    }
                                }
                            }
                        }
                    }};

                    // Install the all-trusting trust manager
                    try {
                        SSLContext sc = SSLContext.getInstance("TLSv1.2");
                        sc.init(null, trustAllCerts, new SecureRandom());
                        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                    } catch (Exception e) {
                    }
                    // Trying for HTTPS
                    conn = (HttpsURLConnection) url.openConnection();

                }
                else
                {
                    conn = (HttpURLConnection) url.openConnection();
                }


                conn.setRequestProperty("Content-Type", "application/json");

                if (authToken != null) {
                    conn.setRequestProperty("Authorization", authToken);
                }

                LOGGER.debug("CXOP - Calling service:{} with token:{}", urlStr, authToken);

                if (method == "POST")
                {
                    //LOGGER.debug("Posted Body:{}", body);
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF8"));
                    writer.write(body);
                    writer.close();
                }
                LOGGER.debug("CXOP - HTTP Status for url:{} is {}",urlStr,conn.getResponseCode());
                int code = conn.getResponseCode();

                if (code == 200)
                {
                    result.put("status", "pass");
                    if (authToken != null) {
                        result.put("response", getResponseStream(conn));
                    } else {
                        result.put("authToken", conn.getHeaderField("Authorization"));
                    }
                } else {
                    result.put("status", "fail");
                    LOGGER.error("CXOP - Failed to parse error response from url:{} due to status code:{}", urlStr, code);
                    result.put("reason", "Other");
                }


            } catch (Exception e)
            {
                LOGGER.debug("CXOP - Exception in call Service {}",e);
                result.put("reason", "Other");
            }

        }

        return result;

    }

    public static StringBuilder getResponseStream(HttpURLConnection conn) {

        try {


            BoundedInputStream boundedInput = new BoundedInputStream(conn.getInputStream(),20480);
            BufferedReader reader = new BufferedReader(new InputStreamReader(boundedInput), 2048);
            StringBuilder output = new StringBuilder();
            StringBuilderWriter writer = new StringBuilderWriter(output);
            IOUtils.copy(reader, writer); // copies data from "reader" => "writer"
            reader.close();
            return output;

            /*
            // Buffer the result into a string
            BufferedReader rd = new BufferedReader(new InputStreamReader(new BoundedInputStream(conn.getInputStream(),20480)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();
            return sb.toString();*/
        } catch (Exception e) {
            return null;
        }
        finally{

            conn.disconnect();
        }
    }

}
