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

namespace com.cognizant.pace.CXOptimize.Collector.utils
{
    public static class Extensions
    {

        public static Dictionary<string, object> ToDictionary(this List<object> list)
        {
            Dictionary<string, object> dict = new Dictionary<string, object>();
            KeyValuePair<String, Object> kvp = new KeyValuePair<string, object>();
            foreach (object obj in list)
            {
                kvp = (KeyValuePair<String, Object>)obj;
                dict.Add(kvp.Key, kvp.Value);
            }
            return dict;
        }

        public static List<Dictionary<string, object>> ToDictionaries(this IReadOnlyCollection<object> list)
        {
            List<Dictionary<string, object>> lstDic = new List<Dictionary<string, object>>();
            //List<object> lst = null;
            //KeyValuePair<String, Object> kvp = new KeyValuePair<string, object>();

            foreach (object obj in list)
            {
                Dictionary<String, Object> dict = (Dictionary<string, object>)obj;
                /*Dictionary<String, Object> dict = new Dictionary<string, object>();

                foreach (object obj1 in lst)
                {
                    kvp = (KeyValuePair<String, Object>)obj1;
                    dict.Add(kvp.Key, kvp.Value);
                }*/

                lstDic.Add(dict);
            }
            return lstDic;
        }

    }
}
