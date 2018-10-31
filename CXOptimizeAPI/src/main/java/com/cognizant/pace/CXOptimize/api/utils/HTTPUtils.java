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

package com.cognizant.pace.CXOptimize.api.utils;

import com.cognizant.pace.CXOptimize.AnalysisEngine.GlobalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.StringBuilderWriter;


@Component
public class HTTPUtils {
    private Logger logger = LoggerFactory.getLogger(HTTPUtils.class);
    private String USER_AGENT = "Mozilla/5.0";

    public StringBuilder httpPost(String query, String esUrl) throws Exception {
        try {

            StringBuilder response = new StringBuilder();
            URL obj = new URL(esUrl);
            if (GlobalConstants.getESUrl().contains(obj.getAuthority())) {
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setDoOutput(true);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
                writer.write(query);
                writer.close();

                int responseCode = con.getResponseCode();

                if (responseCode == 200 || responseCode == 201) {

                    BoundedInputStream boundedInput = new BoundedInputStream(con.getInputStream(), 20480);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(boundedInput), 2048);
                    StringBuilderWriter swriter = new StringBuilderWriter(response);
                    IOUtils.copy(reader, swriter); // copies data from "reader" => "writer"
                    reader.close();

                } else {

                    response.append("NOT_FOUND400");
                }

                con.disconnect();

            } else {
                response.append("NOT_FOUND400");
            }


            return response;
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return new StringBuilder("NOT_FOUND400");

        }

    }

    public StringBuilder httpGet(String esUrl) {

        try {

            //
            StringBuilder response = new StringBuilder();
            URL obj = new URL(esUrl);
            if (GlobalConstants.getESUrl().contains(obj.getAuthority())) {
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", USER_AGENT);
                int responseCode = con.getResponseCode();

                if (responseCode == 200) {

                    BoundedInputStream boundedInput = new BoundedInputStream(con.getInputStream(), 20480);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(boundedInput), 2048);
                    StringBuilderWriter swriter = new StringBuilderWriter(response);
                    IOUtils.copy(reader, swriter); // copies data from "reader" => "writer"
                    reader.close();

                } else {
                    response.append("NOT_FOUND400");
                }

                con.disconnect();


            } else {
                response.append("NOT_FOUND400");
            }
            return response;

        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return new StringBuilder("NOT_FOUND400");

        }

    }
}
