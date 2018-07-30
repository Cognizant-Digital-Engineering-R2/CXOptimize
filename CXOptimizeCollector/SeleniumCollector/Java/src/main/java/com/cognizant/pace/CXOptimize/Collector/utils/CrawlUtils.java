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

import com.cognizant.pace.CXOptimize.Collector.constant.CollectorConstants;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import org.apache.commons.validator.routines.UrlValidator;

public class CrawlUtils
{
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CrawlUtils.class);
    private static UrlValidator urlValidator = new UrlValidator();

    private static String getHostName(String url)
    {
        if (urlValidator.isValid(url))
        {
            try
            {
                return new URL(url).getHost();
            }
            catch(Exception e)
            {
                return (url.split("//")[1]).split("/")[0];
            }
        }
        else
        {
            return (url.split("//")[1]).split("/")[0];
        }
    }

    public static ArrayList<Map<String, Object>> getResourceDetails(ArrayList<Map<String, Object>> resTiming) throws InterruptedException, ExecutionException {
        ArrayList<Map<String, Object>> modifiedResTiming = new ArrayList<>();
        int numThreads = Runtime.getRuntime().availableProcessors(); //max 4 threads
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CompletionService<Map<String, Object>> compService = new ExecutorCompletionService<>(executor);
        int resourceCnt = resTiming.size();
        try {
            for (int i = 0; i < resourceCnt; i++) {
                Task task = new Task(resTiming.get(i));
                compService.submit(task);

            }

            for (int i = 0; i < resourceCnt; i++) {
                Future<Map<String, Object>> future = compService.take();
                modifiedResTiming.add(future.get());
            }
            executor.shutdown(); //always reclaim resources
            LOGGER.debug("CXOP - Crawled Resource Details : {}",modifiedResTiming.toString());
            LOGGER.debug("CXOP - Complete getResourceDetails");
        } catch (Exception e) {
            LOGGER.debug("CXOP - Exception in getResourceDetails : {}",e);
            executor.shutdown(); //always reclaim resources
            return resTiming;
        }

        return modifiedResTiming;
    }


    // PRIVATE

    private static Map<String, Object> callResource(Map<String, Object> resData) throws MalformedURLException {
        String resUrl = resData.get("name").toString();
        String truncUrl = resUrl.toLowerCase().split("\\?")[0];

        Iterator iterator;
        String key;
        Map<String, Object> rs = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        boolean staticResrcStatus = false;
        boolean isImage = false;
        boolean flagSet = false;
        String resourceType = "others";

        iterator = resData.keySet().iterator();

        while (iterator.hasNext())
        {
            key = (String) iterator.next();
            rs.put(key, resData.get(key));
        }

        try {
            if (Double.parseDouble(resData.get("duration").toString()) <= CollectorConstants.getResDurThreshold() || Double.parseDouble(resData.get("duration").toString()) <= 0.0) {
                rs.put("IsCached", true);
            } else {
                rs.put("IsCached", false);
            }

            if(Double.parseDouble(resData.get("responseStart").toString()) != 0.0)
            {
                if(resData.containsKey("encodedBodySize") && Double.parseDouble(resData.get("encodedBodySize").toString()) == 0.0 && resData.containsKey("transferSize") && Double.parseDouble(resData.get("transferSize").toString()) == 0.0)
                {
                    rs.put("IsCached", true);
                }
            }
        }
        catch(Exception e)
        {

        }

        for (String img : CollectorConstants.getImageList())
        {
            if (truncUrl.contains(img.toLowerCase().trim()))
            {
                isImage = true;
                staticResrcStatus = true;
                flagSet = true;
                resourceType = img.trim();
                break;
            }
        }

        if (!flagSet)
        {
            for (String stat : CollectorConstants.getStaticList())
            {
                if (truncUrl.contains(stat.toLowerCase().trim()))
                {
                    staticResrcStatus = true;
                    isImage = false;
                    resourceType = stat.trim();
                    break;
                }
            }
        }

        rs.put("IsStaticResrc", staticResrcStatus);
        rs.put("IsImage", isImage);
        rs.put("ResourceType", resourceType);
        rs.put("HostName",getHostName(truncUrl));

        if (CollectorUtils.stringContainsItemFromList(resUrl, CollectorConstants.getStaticExt() + "," + CollectorConstants.getImages())) {
            try {
                LOGGER.debug("CXOP - Resource URL : {}",resUrl);
                URL url = new URL(resUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setRequestProperty("User-Agent", CollectorConstants.getUserAgent());
                if (CollectorUtils.stringContainsItemFromList(resUrl, CollectorConstants.getStaticExt() + ",.svg")) {
                    conn.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
                }
                LOGGER.debug("CXOP - Status code for : {} is {}" ,resUrl,conn.getResponseCode());
                rs.put("Status", conn.getResponseCode());
                if (conn.getResponseCode() == 200) {
                    Map<String, List<String>> header = conn.getHeaderFields();
                    if(header.containsKey("Last-Modified"))
                    {
                        rs.put("Last-Modified",header.get("Last-Modified").get(0).replaceAll(",",""));
                    }
                    if(header.containsKey("Content-Length"))
                    {
                        rs.put("Content-Length",header.get("Content-Length").get(0));
                    }
                    if(header.containsKey("Connection"))
                    {
                        rs.put("Connection",header.get("Connection").get(0));
                    }
                    if(header.containsKey("Cache-Control"))
                    {
                        rs.put("Cache-Control",header.get("Cache-Control").get(0).replaceAll(",", "#").replaceAll("=", "#"));
                    }
                    if(header.containsKey("ETag"))
                    {
                        rs.put("ETag",header.get("ETag").get(0).replaceAll("\"", ""));
                    }
                    if(header.containsKey("Expires"))
                    {
                        rs.put("Expires",header.get("Expires").get(0).replaceAll(",", ""));
                    }


                    // Buffer the result into a string
                    if (CollectorUtils.stringContainsItemFromList(resUrl, CollectorConstants.getImages())) {
                        if (CollectorUtils.stringContainsItemFromList(resUrl, ".svg"))
                        {
                            if(header.containsKey("Content-Encoding"))
                            {
                                rs.put("Content-Encoding",header.get("Content-Encoding").get(0).replaceAll(",", ""));
                            }
                        }
                        try (ImageInputStream in = ImageIO.createImageInputStream(conn.getInputStream())) {
                            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                            if (readers.hasNext()) {
                                ImageReader reader = readers.next();
                                try {
                                    reader.setInput(in);
                                    rs.put("Height", reader.getHeight(0));
                                    rs.put("Width", reader.getWidth(0));
                                } finally {
                                    reader.dispose();
                                }
                            }
                        }
                        conn.disconnect();
                    } else {
                        if (header.containsKey("Content-Encoding") && CollectorUtils.stringContainsItemFromList(header.get("Content-Encoding").get(0), "gzip")) {
                            String charset = "UTF-8"; // You should determine it based on response header.
                            try (
                                    InputStream gzippedResponse = conn.getInputStream();
                                    InputStream ungzippedResponse = new GZIPInputStream(gzippedResponse);
                                    Reader reader = new InputStreamReader(ungzippedResponse, charset);
                                    Writer writer = new StringWriter()
                            ) {
                                char[] buffer = new char[10240];
                                for (int length = 0; (length = reader.read(buffer)) > 0; ) {
                                    writer.write(buffer, 0, length);
                                }
                                if(header.containsKey("Content-Encoding"))
                                {
                                    rs.put("Content-Encoding",header.get("Content-Encoding").get(0).replaceAll(",", ""));
                                }
                                //striplen = writer.toString().replace("\\n| {2}|\\t|\\r","").length();
                                rs.put("OrgSize", writer.toString().length());
                                if (CollectorUtils.stringContainsItemFromList(resUrl, ".js,.css")) {
                                    rs.put("MinfSize", CollectorUtils.compress(writer.toString(), resUrl));
                                }
                            }
                        } else {
                            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                                String line;
                                while ((line = rd.readLine()) != null) {
                                    sb.append(line);
                                }
                                if(header.containsKey("Content-Encoding"))
                                {
                                    rs.put("Content-Encoding",header.get("Content-Encoding").get(0).replaceAll(",", ""));
                                }
                                rs.put("OrgSize", sb.toString().length());
                                if (CollectorUtils.stringContainsItemFromList(resUrl, ".js,.css")) {
                                    rs.put("MinfSize", CollectorUtils.compress(sb.toString(), resUrl));
                                }
                            }

                        }

                        conn.disconnect();
                    }

                }

            } catch (Exception e) {
                LOGGER.error("CXOP - Exception in callResource for : {} at {}",resUrl,e);
            }
        }
        LOGGER.debug("CXOP - Data for {} is {}",resUrl,rs.toString());
        return rs;
    }

    /**
     * Call URL. Return true only if successful.
     */
    private static final class Task implements Callable {
        private final Map<String, Object> resDetails;

        Task(Map<String, Object> resData) {
            resDetails = resData;
        }

        /**
         * Access a URL, and see if you get a healthy response.
         */
        @Override
        public Map<String, Object> call() throws Exception {
            return callResource(resDetails);
        }

    }
}
