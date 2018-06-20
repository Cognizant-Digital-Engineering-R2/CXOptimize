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

package com.cognizant.pace.CXOptimize.UI.config;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.LoggerFactory;

public class HTTPUtils
{
    private static org.slf4j.Logger log = LoggerFactory.getLogger(HTTPUtils.class);

    public static String httpPOST(String urlStr,String body)
    {
        String response = null;
        HttpURLConnection conn;
        int code = 0;
        try
        {
            URL url = new URL(urlStr);
            if (urlStr.toLowerCase().contains("http") && urlStr.toLowerCase().contains("https"))
            {
                /* Handle https Certificate */

                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                        return hostname.equals("localhost");
                    }
                });

                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } };

                // Install the all-trusting trust manager
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                } catch (Exception e) {
                }

                /* Handle https Certificate */
                // Trying for HTTPS
                conn = (HttpsURLConnection) url.openConnection();

            }
            else
            {
                log.debug("http in the url string");
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setRequestProperty("Content-Type", "application/json");
            //conn.setRequestProperty("Authorization", "Basic " + authToken);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            log.debug("connection set");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF8"));
            writer.write(body);
            writer.close();
            code = conn.getResponseCode();
            log.debug("HTTP POST status for " + urlStr + " is " + code);
            if (conn.getResponseCode() != 200)
            {
                response = null;
                log.error("Unable access login API. Response Code to " + urlStr + " was: " + code);
            }
            else
            {
                // Buffer the result into a string
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();

                conn.disconnect();
                response = sb.toString();
            }
        } catch (Exception e)
        {
            log.error("Exception in accessing Login API");
        }
        return response;
    }

    public static String getHTTPHeader(String urlStr,String body)
    {
        String response = null;
        HttpURLConnection conn;
        int code = 0;
        try
        {
            URL url = new URL(urlStr);
            if (urlStr.toLowerCase().contains("http") && urlStr.toLowerCase().contains("https"))
            {
                /* Handle https Certificate */

                javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                        return hostname.equals("localhost");
                    }
                });

                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } };

                // Install the all-trusting trust manager
                try {
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                } catch (Exception e) {
                }

                /* Handle https Certificate */
                // Trying for HTTPS
                conn = (HttpsURLConnection) url.openConnection();

            }
            else
            {
                log.debug("http in the url string");
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setRequestProperty("Content-Type", "application/json");
            //conn.setRequestProperty("Authorization", "Basic " + authToken);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            log.debug("connection set");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF8"));
            writer.write(body);
            writer.close();
            code = conn.getResponseCode();
            log.debug("HTTP POST status for " + urlStr + " is " + code);
            if (conn.getResponseCode() != 200)
            {
                response = null;
                log.error("Unable access login API. Response Code to " + urlStr + " was: " + code);
            }
            else
            {

                response = conn.getHeaderField("Authorization");
                conn.disconnect();
            }
        } catch (Exception e)
        {
            log.error("Exception in accessing Login API");
        }
        return response;
    }
}

