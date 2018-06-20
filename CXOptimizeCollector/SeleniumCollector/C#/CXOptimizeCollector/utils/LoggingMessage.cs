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
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace com.cognizant.pace.CXOptimize.Collector.utils
{
    public static class LoggingMessage
    {
        public enum MessageType
        {
            INFO,
            WARN,
            DEBUG,
            ERROR
        }


        public static void DisplayMessage(string message, MessageType type, log4net.ILog log)
        {
            if (log != null)
            {
                switch (type)
                {
                    case MessageType.INFO:
                        //Console.WriteLine("INFO! " + message);
                        log.Info(message);
                        break;
                    case MessageType.WARN:
                        //Console.WriteLine("WARN! " + message);
                        log.Warn(message);
                        break;
                    case MessageType.DEBUG:
                        //Console.WriteLine("DEBUG! " + message);
                        log.Debug(message);
                        break;
                    case MessageType.ERROR:
                        Console.WriteLine("ERROR! " + message);
                        log.Error(message);
                        break;
                }
            }
            else
            {
                Console.WriteLine(message);
            }
        }
    }
}
