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

using log4net.Appender;
using log4net.Config;
using log4net.Core;
using log4net.Layout;
using log4net.Repository;
using log4net.Repository.Hierarchy;
using System;
using System.Reflection;
namespace com.cognizant.pace.CXOptimize.Collector.utils
{
    [AttributeUsage(AttributeTargets.Assembly)]
    public class Log4NetConfig : ConfiguratorAttribute
    {
        public Log4NetConfig() : base(0) { }

        public override void Configure(Assembly sourceAssembly, ILoggerRepository targetRepository)
        {
            try
            {
                var hierarchy = (Hierarchy)targetRepository;
                var patternLayout = new PatternLayout();
                patternLayout.ConversionPattern = "%date [%thread] %-5level %logger - %message%newline";
                patternLayout.ActivateOptions();

                var roller = new RollingFileAppender();
                roller.AppendToFile = false;
                roller.File = @"Logs\PerfInsightEventLog.log";
                roller.Layout = patternLayout;
                roller.MaxSizeRollBackups = 5;
                roller.MaximumFileSize = "100MB";
                roller.RollingStyle = RollingFileAppender.RollingMode.Size;
                roller.StaticLogFileName = true;
                roller.ActivateOptions();
                hierarchy.Root.AddAppender(roller);

                hierarchy.Root.Level = GetLoggerLevel();
                hierarchy.Configured = true;
            }
            catch (Exception ex)
            {
                LoggingMessage.DisplayMessage(ex.Message + " " + ex.StackTrace, LoggingMessage.MessageType.ERROR, null);
            }
        }

        private Level GetLoggerLevel()
        {
            string loggerLevel = System.Configuration.ConfigurationManager.AppSettings["CxOptimizeLoggerLevel"];
            if (loggerLevel == null || loggerLevel == string.Empty) loggerLevel = "INFO";
            switch (loggerLevel.ToUpper())
            {
                case "OFF":
                    return Level.Off;
                case "ALL":
                    return Level.All;
                case "DEBUG":
                    return Level.Debug;
                case "WARN":
                    return Level.Warn;
                case "FATAL":
                    return Level.Fatal;
                case "TRACE":
                    return Level.Trace;
                case "ERROR":
                    return Level.Error;
                default:
                    return Level.Info;

            }
        }

    }
}
