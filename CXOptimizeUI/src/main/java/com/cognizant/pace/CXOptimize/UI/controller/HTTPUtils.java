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

package com.cognizant.pace.CXOptimize.UI.controller;


import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.StringBuilderWriter;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HTTPUtils {


    @Value("${login.username}")
    private String user;

    @Value("${authToken.request}")
    private String authRequest;

    @Value("${login.url}")
    private String loginAPIUrl;

    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(HTTPUtils.class);
    private static Map<X509Certificate, Long> trusted = new HashMap();


    public String getLoginStatus(String body) {
        StringBuilder output = new StringBuilder();
        HttpURLConnection conn;
        int code = 0;
        String urlStr = loginAPIUrl + "/login";
        if (urlStr.toLowerCase().contains("http") || urlStr.toLowerCase().contains("https")) {
            try
            {
                URL url = new URL(urlStr);

                if (urlStr.toLowerCase().contains("https")) {
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
                            X509Certificate[] res = new X509Certificate[trusted.size()];
                            trusted.keySet().toArray(res);
                            return res;

                            //Enable this only if your are having SSL exception
                            //return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            if (certs != null) {
                                for (X509Certificate item : certs) {
                                    if (!trusted.containsKey(item) || (System.currentTimeMillis() > (trusted.get(item) + 3600 * 1000 * 24))) {
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

                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF8"));
                writer.write(body);
                writer.close();
                code = conn.getResponseCode();

                if (code == 200)
                {
                    BoundedInputStream boundedInput = new BoundedInputStream(conn.getInputStream(),20480);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(boundedInput), 2048);

                    StringBuilderWriter swriter = new StringBuilderWriter(output);
                    IOUtils.copy(reader, swriter); // copies data from "reader" => "writer"
                    reader.close();
                }
                else {
                    LOGGER.debug("Exception in accessing Login API {}",code);
                }

            } catch (Exception e)
            {
                LOGGER.debug("Exception in accessing Login API {}",e);
            }
        }
        return output.toString();
    }


    public String getAuthToken()
    {
        String output = null;
        HttpURLConnection conn;
        int code = 0;

        String urlStr = loginAPIUrl + "/authToken";

        if (urlStr.toLowerCase().contains("http") || urlStr.toLowerCase().contains("https")) {
            try
            {
                URL url = new URL(urlStr);

                if (urlStr.toLowerCase().contains("https")) {
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
                            X509Certificate[] res = new X509Certificate[trusted.size()];
                            trusted.keySet().toArray(res);
                            return res;

                            //Enable this only if your are having SSL exception
                            //return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            if (certs != null) {
                                for (X509Certificate item : certs) {
                                    if (!trusted.containsKey(item) || (System.currentTimeMillis() > (trusted.get(item) + 3600 * 1000 * 24))) {
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

                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF8"));
                writer.write(authRequest);
                writer.close();
                code = conn.getResponseCode();

                if (code == 200)
                {
                    Base64 base64Url = new Base64(true);
                    String[] split_string = conn.getHeaderField("Authorization").split("\\.");
                    String base64EncodedBody = split_string[1];
                    String tokenbody = new String(base64Url.decode(base64EncodedBody));
                    JSONObject tokenDetails = new JSONObject(tokenbody);
                    if(tokenDetails.getString("sub").equals(user)){
                        output = base64EncodedBody;
                    }

                    conn.disconnect();
                }
                else {
                    LOGGER.debug("Exception in accessing AuthToken API {}",code);
                }

            } catch (Exception e)
            {
                LOGGER.debug("Exception in accessing AuthToken API {}",e);
            }
        }
        return output;

    }
}