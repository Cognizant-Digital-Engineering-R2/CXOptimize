package com.cognizant.pace.CXOptimize.Collector;


import com.cognizant.pace.CXOptimize.Collector.constant.CollectorConstants;
import io.specto.hoverfly.junit.rule.HoverflyRule;

import static io.specto.hoverfly.junit.core.HoverflyConfig.configs;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import org.junit.ClassRule;
import org.junit.Test;

import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;
import static org.junit.Assert.assertEquals;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.File;


public class CXOptimizeCollectorTest {


    @ClassRule
    public static HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(
            dsl(
                    service("http://test").get("/getConfig").anyQueryParams().willReturn(success("{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimize\",\"ClientNameOrg\":\"cxoptimize\",\"ProjectName\":\"demo\",\"ProjectNameOrg\":\"demo\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":2},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"High\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":1,\"Scenario\":\"test\",\"ScenarioOrg\":\"test\",\"beaconUrl\":\"\",\"comments\":\"Config file was created by admin\",\"compareComplianceThreshold\":80,\"compareComplianceWeight\":25,\"configUpdated\":false,\"creationTimestamp\":1533063480519,\"dataStoreUrl\":\"\",\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"excludedSoftNavigationRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"individualScoreThreshold\":80,\"indvScoreComplianceThreshold\":80,\"indvScoreComplianceWeight\":25,\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimize/app/kibana#/dashboard/CXOptimize_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimize%20AND%20ProjectName:demo%20AND%20Scenario:test')),title:CXOptimize_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":60,\"runTimestamp\":1533063480519,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"slaComplianceThreshold\":80,\"slaComplianceWeight\":25,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"totalScoreComplianceWeight\":25,\"totalScoreThreshold\":80,\"transactionSLA\":{}}", "application/json")),
                    service("http://test").post("/insertStats").willReturn(success("{\"Status\":\"Success\",\"StatsIndex\":\"Success\"}", "application/json")),
                    service("http://test").post("/authToken").body("{\"username\":\"cxopadmin\",\"password\":\"qwertyuiop\"}").willReturn(success())
            ));


/*
    private final RestTemplate restTemplate = new RestTemplate();
    @Test
    public void testASecondStub() {
        final ResponseEntity<String> courseResponse = restTemplate.postForEntity("http://localhost/insertStats",null, String.class);
        assertEquals("200", courseResponse.getStatusCode().toString());
        assertEquals("{\"Status\":\"Success\",\"StatsIndex\":\"Success\"}", courseResponse.getBody());
    }*/

    @Test
    public void startTransaction(){


    }

    @Test
    public void endTransaction() {
    }

    @Test
    public void chromeTest() throws InterruptedException {
        String resourcePath = System.getProperty("user.dir") + "/src/test/resources";
        CollectorConstants.setCollectorProperties(resourcePath);
        System.setProperty("webdriver.chrome.driver", resourcePath + File.separator + "chromedriver.exe");
        WebDriver driver = new ChromeDriver();
        CXOptimizeCollector.StartTransaction("Home", driver);
        driver.get("https://www.wikipedia.org/");
        CXOptimizeCollector.EndTransaction("Home", driver);
        Thread.sleep(2000);
        driver.quit();

    }
}