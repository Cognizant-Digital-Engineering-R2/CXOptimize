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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);


    public static Map<String, Object> callService(String urlStr, String method, String body, String authToken)
    {
        LOGGER.debug("CXOP - Calling service:{} method:{} body:{} token : {}", urlStr,method,body,authToken);
        HttpURLConnection conn;
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> resultToken = new HashMap<String, Object>();
        try {
            URL url = new URL(urlStr);
            if (urlStr.toLowerCase().contains("http") && urlStr.toLowerCase().contains("https")) {
                // Handle https Certificate //
                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                        return true;

                    }
                });

                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }};

                // Install the all-trusting trust manager
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                } catch (Exception e) {
                }
                // Trying for HTTPS
                conn = (HttpsURLConnection) url.openConnection();

            } else {
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
				String response = getResponseStream(conn);
                LOGGER.debug("CXOP - HTTP Status failed {}",);
                try
                {
                    //JSONObject jsonObj = new JSONObject(getResponseStream(conn));
                    if (response.contains("io.jsonwebtoken.ExpiredJwtException")) {
                        LOGGER.error("CXOP - Unable to connect to url:{} due to expired Auth Token : {}", urlStr,response);
                        result.put("reason", "JWTExpiry");
                    } else {
                        LOGGER.error("CXOP - Unable to connect to url:{} due to status code:{} {}", urlStr, code,response);
                        result.put("reason", "Other");
                    }

                } catch (Exception e)
                {
                    LOGGER.error("CXOP - Failed to parse error response from url:{} due to status code:{} {}", urlStr, code,response);
                    result.put("reason", "Other");
                }


            }

        } catch (Exception e)
        {
            LOGGER.debug("CXOP - Exception in call Service {}",e);
            result.put("reason", "Other");
        }
        return result;

    }

    public static String getResponseStream(HttpURLConnection conn) {
        try {
            // Buffer the result into a string
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();

            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

}
