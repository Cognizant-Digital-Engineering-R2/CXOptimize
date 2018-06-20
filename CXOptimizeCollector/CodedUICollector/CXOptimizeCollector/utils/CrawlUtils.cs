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

using com.cognizant.pace.CXOptimize.Collector.constant;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Yahoo.Yui.Compressor;

namespace com.cognizant.pace.CXOptimize.Collector.utils
{
    public class CrawlUtils
    {
        private static readonly log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        private static String getHostName(String url)
        {
            if (Uri.IsWellFormedUriString(url, UriKind.RelativeOrAbsolute))
            {
                try
                {
                    return new Uri(url).Host;
                }
                catch 
                {
                    return "NA";
                }
            }
            else
            {
                return "NA";
            }
        }
        private static Boolean stringContainsItemFromList(String inputString, String match)
        {
            String[] items = match.Split(',');
            for (int i = 0; i < items.Length; i++)
            {
                if (inputString.ToLower().Contains(items[i]))
                {
                    return true;
                }
            }
            return false;
        }
        
        public static List<Dictionary<String, Object>> getResourceDetails(List<Dictionary<String, Object>> resTiming)
        {
            List<Dictionary<String, Object>> modifiedResTiming = new List<Dictionary<String, Object>>();
            int numThreads = Environment.ProcessorCount;
            System.Threading.ThreadPool.SetMaxThreads(numThreads, numThreads);

            Task<Dictionary<String, Object>>[] taskList = new Task<Dictionary<String, Object>>[resTiming.Count()];
            int i = 0;
            for (i = 0; i < resTiming.Count(); i++)
            {
                int index = i;
                taskList[i] = Task.Factory.StartNew(() => callResource(resTiming[index]));
            }

            Task.WaitAll(taskList);

            for (int j = 0; j < resTiming.Count(); j++)
            {
                modifiedResTiming.Add(taskList[j].Result);
            }

            return modifiedResTiming;

        }

        private static Dictionary<String, Object> callResource(Dictionary<String, Object> resData)
        {
            String resUrl = resData["name"].ToString();
            String truncUrl = resUrl.ToLower().Split(new string[] { "\\?" }, StringSplitOptions.None)[0];
            
            Dictionary<String, Object> rs = new Dictionary<string, object>();
            StringBuilder sb = new StringBuilder();
            Boolean staticResrcStatus = false;
            Boolean isImage = false;
            Boolean flagSet = false;
            String resourceType = "others";

            //LoggingMessage.DisplayMessage("Started Thread to get ResourceTiming : " + resUrl, LoggingMessage.MessageType.DEBUG, log);
            
            rs = (from x in resData select x).ToDictionary(x => x.Key, x => x.Value);

            try
            {
                if (Convert.ToDouble(resData["duration"]) <= CollectorConstants.getResDurThreshold() || Convert.ToDouble(resData["duration"]) <= 0.0)
                    rs.Add("IsCached", true);
                else 
                    rs.Add("IsCached", false);
            }
            catch (Exception e)
            {            }

            foreach (String img in CollectorConstants.getImageList())
            {
                if (truncUrl.Contains(img.ToLower().Trim()))
                {
                    isImage = true;
                    staticResrcStatus = true;
                    flagSet = true;
                    resourceType = img.Trim();
                    break;
                }
            }

            if (!flagSet)
            {
                foreach (String stat in CollectorConstants.getStaticList())
                {
                    if (truncUrl.Contains(stat.ToLower().Trim()))
                    {
                        staticResrcStatus = true;
                        isImage = false;
                        resourceType = stat.Trim();
                        break;
                    }
                }
            }

            rs.Add("IsStaticResrc", staticResrcStatus);
            rs.Add("IsImage", isImage);
            rs.Add("ResourceType", resourceType);
            rs.Add("HostName", getHostName(truncUrl));

            //if(resUrl.contains(".js") || resUrl.contains(".css") || resUrl.contains(".png") || resUrl.contains(".jpg") || resUrl.contains(".jpeg") || resUrl.contains(".gif") || resUrl.contains(".svg") || resUrl.contains(".html"))
            if (stringContainsItemFromList(resUrl, CollectorConstants.getStaticExt() + "," + CollectorConstants.getImages()))
            {

                try
                {
                    HttpWebRequest httpReq = (HttpWebRequest)WebRequest.Create(resUrl);
                    httpReq.Timeout = 5000;
                    httpReq.UserAgent = CollectorConstants.getUserAgent();

                    if (stringContainsItemFromList(resUrl, CollectorConstants.getStaticExt() + ",.svg"))
                    {
                        httpReq.Headers["Accept-Encoding"] = "gzip,deflate,sdch";
                    }

                    using (HttpWebResponse httpRes = (HttpWebResponse)httpReq.GetResponse())
                    {
                        int statusCode = (int)httpRes.StatusCode;
                        rs.Add("Status", statusCode);
                        if (statusCode == 200)
                        {
                            string[] header = httpRes.Headers.AllKeys;

                            if (httpRes.GetResponseHeader("Last-Modified") != null)
                                rs.Add("Last-Modified", httpRes.GetResponseHeader("Last-Modified").Replace(",", ""));
                            if (httpRes.GetResponseHeader("Content-Length") != null)
                                rs.Add("Content-Length", httpRes.GetResponseHeader("Content-Length"));
                            if (httpRes.GetResponseHeader("Connection") != null)
                                rs.Add("Connection", httpRes.GetResponseHeader("Connection"));
                            if (httpRes.GetResponseHeader("Cache-Control") != null)
                                rs.Add("Cache-Control", httpRes.GetResponseHeader("Cache-Control").Replace(",", "#").Replace("=", "#"));
                            if (httpRes.GetResponseHeader("ETag") != null)
                                rs.Add("ETag", httpRes.GetResponseHeader("ETag").Replace("\"", ""));
                            if (httpRes.GetResponseHeader("Expires") != null)
                                rs.Add("Expires", httpRes.GetResponseHeader("Expires").Replace(",", ""));

                            if (stringContainsItemFromList(resUrl, CollectorConstants.getImages()))
                            {
                                if (stringContainsItemFromList(resUrl, ".svg"))
                                {
                                    if (httpRes.GetResponseHeader("Content-Encoding") != null)
                                        rs.Add("Content-Encoding", httpRes.GetResponseHeader("Content-Encoding").Replace(",", ""));
                                }
                                Image image = Image.FromStream(httpRes.GetResponseStream());
                                rs.Add("Height", image.Height);
                                rs.Add("Width", image.Width);
                            }
                            else
                            {
                                if (header.Contains("Content-Encoding") && stringContainsItemFromList(header[Array.IndexOf(header, "Content-Encoding")], "gzip"))
                                {
                                    try
                                    {
                                        using (GZipStream ungzippedResponse = new GZipStream(httpRes.GetResponseStream(), CompressionMode.Decompress))
                                        {
                                            using (StreamReader file = new StreamReader(ungzippedResponse, Encoding.UTF8))
                                            {
                                                String line;
                                                while ((line = file.ReadLine()) != null)
                                                {
                                                    sb.Append(line);
                                                }
                                                if (httpRes.GetResponseHeader("Content-Encoding") != null)
                                                    rs.Add("Content-Encoding", httpRes.GetResponseHeader("Content-Encoding").Replace(",", ""));
                                                rs.Add("OrgSize", sb.ToString().Length);
                                                if (stringContainsItemFromList(resUrl, ".js,.css"))
                                                {
                                                    rs.Add("MinfSize", compress(sb.ToString(), ref resUrl));
                                                }
                                            }
                                        }
                                    }
                                    catch { }
                                }
                                else
                                {

                                    using (StreamReader file = new StreamReader(httpRes.GetResponseStream(), Encoding.UTF8))
                                    {
                                        String line;
                                        while ((line = file.ReadLine()) != null)
                                        {
                                            sb.Append(line);
                                        }
                                    }

                                    rs.Add("OrgSize", sb.ToString().Length);
                                    if (stringContainsItemFromList(resUrl, ".js,.css"))
                                    {
                                        if (httpRes.GetResponseHeader("Content-Encoding") != null)
                                            rs.Add("Content-Encoding", httpRes.GetResponseHeader("Content-Encoding").Replace(",", ""));
                                        //striplen = sb.ToString().Replace("\\n| {2}|\\t|\\r", "").Length;
                                        rs.Add("MinfSize", compress(sb.ToString(), ref resUrl));
                                    }
                                }

                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    LoggingMessage.DisplayMessage("PerfInsight - CallResource Error: " + ex.Message, LoggingMessage.MessageType.ERROR, log);
                }
            }

            return rs;
        }
        
        public static long compress(String input, ref String filename)
        {
            long compressed = 0;
            try
            {
                String output = string.Empty;

                if (stringContainsItemFromList(filename, ".js"))
                {
                    JavaScriptCompressor compressor = new JavaScriptCompressor();
                    output = compressor.Compress(input);
                    compressed = output.Length;
                }
                else
                {
                    CssCompressor compressor = new CssCompressor();
                    output = compressor.Compress(input);
                    compressed = output.Length;
                }
            }
            catch
            {
                compressed = input.Replace("\\n", "").Replace("\\t", "").Replace("\\r", "").Length;
            }
            return compressed;
        }
    }
}
