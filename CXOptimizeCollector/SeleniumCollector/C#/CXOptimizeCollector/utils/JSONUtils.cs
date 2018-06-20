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
using System.Collections.Generic;
using System.Text;
using System.Web.Script.Serialization;


namespace com.cognizant.pace.CXOptimize.Collector.utils
{
    public class JSONUtils
    {
        public static Dictionary<String, Object> JsonStringToMap(string jsonString)
        {
            Dictionary<String, Object> retMap = new Dictionary<String, Object>();

            if (jsonString != string.Empty)
            {
                //retMap = new JavaScriptSerializer().Deserialize<Dictionary<String, Object>(jsonString);
                var serializer = new JavaScriptSerializer();
                retMap = serializer.Deserialize<Dictionary<string, object>>(jsonString);
            }
            return retMap;
        }

        public static string MapToJsonString(Dictionary<String, Object> jsonMap)
        {
            StringBuilder retString = new StringBuilder();

            if (jsonMap != null)
            {
                new JavaScriptSerializer().Serialize(jsonMap, retString);
            }
            return retString.ToString();
        }

        public static string ObjectToJsonString(Object jsonObject)
        {
            string retString = string.Empty;

            if (jsonObject != null)
            {
                retString = new JavaScriptSerializer().Serialize(jsonObject);
            }
            return retString;
        }
    }
}
