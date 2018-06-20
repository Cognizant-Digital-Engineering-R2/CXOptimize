using System;
using com.cognizant.pace.CXOptimize.Collector.constant;
using com.cognizant.pace.CXOptimize.Collector;
using OpenQA.Selenium;
using OpenQA.Selenium.Chrome;
using OpenQA.Selenium.IE;
using OpenQA.Selenium.Firefox;
using com.cognizant.pace.CXOptimize.Collector.config;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace CXOptimizeCollectorTest
{
    [TestClass]
    public class CXOptimizeCollectorTest
    {

        [TestMethod]
        public void HardTransactionChrome()
        {
            CollectorConstants.setCollectorProperties("C:\\Work\\CXOptimize\\Collector\\SeleniumC#\\CXOptimizeCollectorTest");

            using (var driver = new ChromeDriver("C:\\Work\\CXOptimize\\Collector\\SeleniumC#\\CXOptimizeCollectorTest"))
            {
                CXOptimizeCollector.StartTransaction("Test", driver);
                driver.Navigate().GoToUrl("https://www.wikipedia.org/");
                var result = CXOptimizeCollector.EndTransaction("Test", driver);
                Assert.AreEqual("Success", result["UploadStatus"].ToString());

            }
        }

        [TestMethod]
        public void HardTransactionIE()
        {
            CollectorConstants.setCollectorProperties("C:\\Work\\CXOptimize\\Collector\\SeleniumC#\\CXOptimizeCollectorTest");
            var options = new InternetExplorerOptions();
            options.IntroduceInstabilityByIgnoringProtectedModeSettings = true;
            using (var driver = new InternetExplorerDriver("C:\\Work\\CXOptimize\\Collector\\SeleniumC#\\CXOptimizeCollectorTest", options))
            {
                
                CXOptimizeCollector.StartTransaction("Test", driver);
                driver.Navigate().GoToUrl("https://www.wikipedia.org/");
                var result = CXOptimizeCollector.EndTransaction("Test", driver);
                Assert.AreEqual("Success", result["UploadStatus"].ToString());

            }
        }


        [TestMethod]
        public void HardTransactionFirefox()
        {
            
            using (var driver = new FirefoxDriver())
            {
                CXOptimizeCollector.StartTransaction("Test", driver);
                driver.Navigate().GoToUrl("https://www.wikipedia.org/");
                var result = CXOptimizeCollector.EndTransaction("Test", driver);
                Assert.AreEqual("Success", result["UploadStatus"].ToString());

            }
        }
        
    }
}
