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

package com.cognizant.pace.CXOptimize.api;


import com.cognizant.pace.CXOptimize.api.controller.ApiController;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;


@RunWith(SpringRunner.class)
@WebMvcTest(value = ApiController.class, secure = false)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ApiControllerTest
{
    @Autowired
    private MockMvc mockMvc;

    String authToken = null;
    Lock sequential = new ReentrantLock();

    private void setAuthToken() throws Exception
    {
        if(authToken == null)
        {
            String authJson = "{\"username\":\"cxopuser\",\"password\":\"b02Tpjyh4eteEyCuLbJq8zrz2Gr+TnVL2mh4fOwh57s=\"}";
            RequestBuilder requestBuilder = MockMvcRequestBuilders
                    .post("/authToken")
                    .accept(MediaType.APPLICATION_JSON).content(authJson)
                    .contentType(MediaType.APPLICATION_JSON);

            MvcResult result = mockMvc.perform(requestBuilder).andReturn();

            authToken = result.getResponse().getHeader("Authorization");

        }
    }

    @BeforeClass
    public static void setUpEnv() throws Exception
    {


    }

    @Before
    public void setUp() throws Exception
    {

        sequential.lock();
        setAuthToken();
        System.out.println("---------------------------------------------------------------------------------");
        Thread.sleep(1000);

    }

    @After
    public void tearDown() throws Exception
    {
        sequential.unlock();
        System.out.println("---------------------------------------------------------------------------------");

    }



    @Test
    public void test_101_createuser_success() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"Password\":\"asdfghjkl\",\"Role\":\"USER\",\"SecurityQuestions\":{\"q1\":\"\",\"a1\":\"\",\"q2\":\"\",\"a2\":\"\",\"q3\":\"\",\"a3\":\"\"},\"Clients\":\"\",\"ContactEmail\":\"PerfInsightSupport@cognizant.com\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/createUser")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{status:true}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_102_createuser_existinguser_choose_different_name() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"Password\":\"asdfghjkl\",\"Role\":\"USER\",\"SecurityQuestions\":{\"q1\":\"\",\"a1\":\"\",\"q2\":\"\",\"a2\":\"\",\"q3\":\"\",\"a3\":\"\"},\"Clients\":\"\",\"ContactEmail\":\"PerfInsightSupport@cognizant.com\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/createUser")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:UserName already exists.Choose different name or add Client to existing user.}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_103_createuser_check_role() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser1\",\"Password\":\"asdfghjkl\",\"Role\":\"DB\",\"SecurityQuestions\":{\"q1\":\"\",\"a1\":\"\",\"q2\":\"\",\"a2\":\"\",\"q3\":\"\",\"a3\":\"\"},\"Clients\":\"\",\"ContactEmail\":\"PerfInsightSupport@cognizant.com\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/createUser")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Failed}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_104_createconfig_success() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/createConfig?ClientName=cxoptimisedemo&ProjectName=Test&Scenario=Test")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{ClientNameOrg:cxoptimisedemo}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_105_createconfig_existing() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/createConfig?ClientName=cxoptimisedemo&ProjectName=Test&Scenario=Test")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Reason:Already config exists for given combination}",result.getResponse().getContentAsString(),false);


    }

    @Test
    public void test_106_createconfig_validation() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/createConfig?ClientName=&ProjectName=&Scenario=")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Reason:ClientName or ProjectName or Scenario cannot be null or blank}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_111_manageclient_success() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"Password\":\"asdfghjkl\",\"ClientName\":\"cxoptimisedemo\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/manageClientAccess")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{status:true}",result.getResponse().getContentAsString(),false);
    }

    @Test
    public void test_112_manageclient_invaliduser() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser5\",\"Password\":\"asdfghjkl\",\"ClientName\":\"cxoptimisedemo\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/manageClientAccess")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:Incorrect Username}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_113_manageclient_existingclient() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"Password\":\"asdfghjkl\",\"ClientName\":\"cxoptimisedemo\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/manageClientAccess")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:ClientName already available to this user}",result.getResponse().getContentAsString(),false);


    }



    @Test
    public void test_114_manageclient_invalidclient() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"Password\":\"asdfghjkl\",\"ClientName\":\"cxoptimisedemo5\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/manageClientAccess")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:ClientName not available}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_115_manageclient_updateclient() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"Password\":\"asdfghjkl\",\"ClientName\":\"\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/manageClientAccess")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        System.out.println(result.getResponse().toString());
        JSONAssert.assertEquals("{status:true}",result.getResponse().getContentAsString(),false);

    }



    @Test
    public void test_116_updatepassword_success() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"OldPassword\":\"asdfghjkl\",\"NewPassword\":\"asdfghjkl123\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updatePassword")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{status:true}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_117_updatepassword_invaliduser() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser5\",\"OldPassword\":\"asdfghjkl\",\"NewPassword\":\"asdfghjkl123\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updatePassword")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:Incorrect Username}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_118_updatepassword_invalidpassword() throws Exception
    {
        String userJson = "{\"UserName\":\"demouser\",\"OldPassword\":\"asdfghjkl\",\"NewPassword\":\"asdfghjkl123\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updatePassword")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(userJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:Incorrect Old Password}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_119_login_success() throws Exception
    {
        String loginJson = "{\"UserName\":\"cxopuser\",\"Password\":\"b02Tpjyh4eteEyCuLbJq8zrz2Gr+TnVL2mh4fOwh57s=\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/login")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(loginJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{status:true}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_120_login_password_invalid() throws Exception
    {
        String loginJson = "{\"UserName\":\"cxopuser\",\"Password\":\"BtXoCC0oZCNnCbKd6J1MYGtj1I/iYIBxVOZnZLeT/2o=\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/login")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(loginJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:Incorrect Password}",result.getResponse().getContentAsString(),false);

     }

    @Test
    public void test_121_login_username_invalid() throws Exception
    {
        String loginJson = "{\"UserName\":\"cxopuser1\",\"Password\":\"b02Tpjyh4eteEyCuLbJq8zrz2Gr+TnVL2mh4fOwh57s=\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/login")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(loginJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{reason:Incorrect Username}",result.getResponse().getContentAsString(),false);

    }





    @Test
    public void test_126_getconfig_success() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getConfig?ClientName=cxoptimisedemo&ProjectName=Test&Scenario=Test")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{ClientNameOrg:cxoptimisedemo}",result.getResponse().getContentAsString(),false);


    }

    @Test
    public void test_127_getconfig_validation() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getConfig?ClientName=&ProjectName=&Scenario=")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Reason:ClientName or ProjectName or Scenario cannot be null or blank}",result.getResponse().getContentAsString(),false);


    }


    @Test
    public void test_129_getconfig_wrongconfig() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getConfig?ClientName=cxoptimisedemo&ProjectName=Test&Scenario=Test1")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_130_checkHealth() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/checkHealth")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{API:UP}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_131_viewLicenseCache() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/viewLicenseCache")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        assertThat(result.getResponse().getContentAsString(), containsString("cxoptimize"));


    }

    @Test
    public void test_132_insertStats_chrome_hard() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(IOUtils.toString(this.getClass().getResourceAsStream("chrome_hard.json"),"UTF-8"))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_133_insertStats_chrome_soft() throws Exception
    {
        String insertJson = "{\"resources\":[{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4401.86,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"fetchStart\":4401.86,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4520.785,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":118.92500000000018,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4627.285000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"fetchStart\":4627.285000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4675.93,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":48.64499999999953,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4694.395,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4694.395,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4788.490000000001,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":94.09500000000025,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4694.645,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4694.645,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4893.515,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":198.8699999999999,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4694.740000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4694.740000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":5409.635,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":714.8949999999995,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4694.85,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4694.85,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":5466.975,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":772.125,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4694.9800000000005,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4694.9800000000005,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4844.18,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":149.19999999999982,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4377.780000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s42162307108326?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2016%3A46%3A8%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=72A9413266883024-2F54CED8C44C6630&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.6&v=N&k=Y&bw=1040&bh=878&AQE=1\",\"fetchStart\":4377.780000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4432.59,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":54.80999999999949,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4694.050000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s43620667462828?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2016%3A46%3A8%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=72A9413266883024-2F54CED8C44C6630&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=3PM&v4=3PM&c5=Tuesday&v5=Tuesday&c6=weekday&v6=weekday&c7=New&v7=New&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-29%7C29&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=1&c36=First%20Visit&c37=10&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=1&v42=First%20Visit&v43=10&v47=Landscape%3A1280x918&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.6&v=N&k=Y&bw=1040&bh=878&AQE=1\",\"fetchStart\":4694.050000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4726.870000000001,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":32.81999999999971,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4755.6,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"fetchStart\":4755.6,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4816.895,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":61.29500000000007,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4339.005,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"fetchStart\":4339.005,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4380.540000000001,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":41.535000000000764,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"Content-Length\":\"1004\",\"requestStart\":4715.645,\"IsImage\":true,\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"nextHopProtocol\":\"http/1.1\",\"startTime\":4696.68,\"domainLookupEnd\":4696.68,\"ResourceType\":\".svg\",\"responseStart\":4730.790000000001,\"connectEnd\":4696.68,\"connectStart\":4696.68,\"decodedBodySize\":1004,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":4696.68,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":true,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"responseEnd\":4731.255,\"secureConnectionStart\":0,\"initiatorType\":\"css\",\"HostName\":\"www.comcastnow.com\",\"transferSize\":1286,\"Status\":200,\"encodedBodySize\":1004,\"duration\":34.57499999999982,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":4696.68}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.79 Safari/537.36\"},\"others\":{\"dom\":null,\"domElementCount\":1028,\"msFirstPaint\":1505249166758},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"1\",\"BuildNumber\":\"\",\"RunTime\":1505247206484,\"resourceLoadTime\":1003.3499999999985,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":1127.9700000000003,\"StartTime\":1505249169390,\"ScriptTime\":2000},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":23100000,\"totalJSHeapSize\":42100000,\"jsHeapSizeLimit\":2190000000}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_134_insertStats_firefox_hard() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(IOUtils.toString(this.getClass().getResourceAsStream("firefox_hard.json"),"UTF-8"))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_135_insertStats_firefox_soft() throws Exception
    {
        String insertJson = "{\"resources\":[{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6654.0614527272655,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s43454966550597?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2016%3A59%3A51%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=56E0CEA7102AAD36-346F45AB474CF867&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=3PM&v4=3PM&c5=Tuesday&v5=Tuesday&c6=weekday&v6=weekday&c7=New&v7=New&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-29%7C29&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=1&c36=First%20Visit&c37=27&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=1&v42=First%20Visit&v43=27&v47=Landscape%3A1280x913&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.8.5&v=N&k=Y&bw=1280&bh=913&AQE=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6654.0614527272655,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":6989.271657543689,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":335.21020481642336,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6255.285038823118,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6255.285038823118,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":6297.768158269737,\"initiatorType\":\"other\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":42.48311944661873,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6735.001073573225,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6735.001073573225,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7111.2960841057375,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":376.2950105325126,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6738.862652830231,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6738.862652830231,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7182.063239415861,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":443.20058658562994,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6109.307818281618,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s47235708107327?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2016%3A59%3A50%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=56E0CEA7102AAD36-346F45AB474CF867&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.8.5&v=N&k=Y&bw=1280&bh=913&AQE=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6109.307818281618,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":6269.085073390944,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":159.77725510932578,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6746.199899745094,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6746.199899745094,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7065.905900793623,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":319.7060010485293,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":5911.293292404808,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":5911.293292404808,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":6643.873386509762,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":732.5800941049538,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6743.092490283444,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6743.092490283444,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7096.529628363024,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":353.4371380795801,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6993.402964375844,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6993.402964375844,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7054.709537887282,\"initiatorType\":\"other\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":61.30657351143782,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6763.205053307119,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6763.205053307119,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7551.253781625764,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":788.0487283186449,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6695.11136218173,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6695.11136218173,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7049.432812585316,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":354.3214504035859,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":6995.915084668594,\"IsImage\":true,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6891.582652206203,\"domainLookupEnd\":6891.582652206203,\"ResourceType\":\".svg\",\"responseStart\":7073.020632722594,\"connectEnd\":6891.582652206203,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"decodedBodySize\":1004,\"connectStart\":6891.582652206203,\"fetchStart\":6891.582652206203,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":true,\"secureConnectionStart\":0,\"responseEnd\":7073.130658582777,\"initiatorType\":\"css\",\"HostName\":\"www.comcastnow.com\",\"transferSize\":1465,\"Status\":401,\"encodedBodySize\":1004,\"duration\":181.54800637657354,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":6891.582652206203}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:46.0) Gecko/20100101 Firefox/46.0\"},\"others\":{\"dom\":null,\"domElementCount\":1028,\"msFirstPaint\":1505249988015},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"1\",\"BuildNumber\":\"\",\"RunTime\":1505247206484,\"resourceLoadTime\":1629.7724230034519,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":1639.9604892209554,\"StartTime\":1505249992793,\"ScriptTime\":2000},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":0,\"totalJSHeapSize\":0,\"jsHeapSizeLimit\":0}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_136_insertStats_ie_hard() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(IOUtils.toString(this.getClass().getResourceAsStream("ie_hard.json"),"UTF-8"))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_137_insertStats_ie_soft() throws Exception
    {
        String insertJson = "{\"resources\":[{\"fetchStart\":8118.485535089012,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8119.274190601739,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":7949.296552947277,\"domainLookupEnd\":0,\"duration\":169.97763765446234,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8303.822864933814,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8365.543677597954,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8302.548946112373,\"domainLookupEnd\":0,\"duration\":62.994731485581724,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8328.035533426308,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8388.43357250273,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8326.416346886905,\"domainLookupEnd\":0,\"duration\":62.01722561582574,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8337.25225193787,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8532.69225479526,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8332.076109978238,\"domainLookupEnd\":0,\"duration\":200.61614481702236,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8341.772344178955,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8418.352806131561,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8340.236087578942,\"domainLookupEnd\":0,\"duration\":78.1167185526192,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8350.803907231779,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8444.91625102482,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8345.547709142533,\"domainLookupEnd\":0,\"duration\":99.3685418822879,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8273.035740751156,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8291.369415520461,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8267.93103349179,\"domainLookupEnd\":0,\"duration\":23.438382028671185,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s56788994646771?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2017%3A13%3A13%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=4PM&v4=4PM&c5=Tuesday&v5=Tuesday&c6=weekday&v6=weekday&c7=Repeat&v7=Repeat&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-28%7C28&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=5&c36=Less%20than%201%20day&c37=8&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=5&v42=Less%20than%201%20day&v43=8&v47=Landscape%3A1280x908&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8047.868638976004,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8064.981355132686,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8040.094572974446,\"domainLookupEnd\":0,\"duration\":24.88678215823984,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s51820629878260?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2017%3A13%3A13%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8666.881107615554,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":9227.693591116808,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8618.28950020753,\"domainLookupEnd\":0,\"duration\":609.4040909092782,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8145.350319465011,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8219.30083492385,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8119.277885500029,\"domainLookupEnd\":0,\"duration\":100.02294942382105,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8617.406419516288,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8618.286626397748,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8368.767271083603,\"domainLookupEnd\":0,\"duration\":249.5193553141453,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":8479.754215365769,\"redirectStart\":0,\"IsStaticResrc\":true,\"redirectEnd\":0,\"Content-Length\":\"1004\",\"requestStart\":8479.754215365769,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"responseEnd\":8488.672468204373,\"initiatorType\":\"css\",\"IsImage\":true,\"HostName\":\"www.comcastnow.com\",\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"startTime\":8477.669882186116,\"Status\":200,\"domainLookupEnd\":8479.754215365769,\"duration\":11.002586018257716,\"ResourceType\":\".svg\",\"entryType\":\"resource\",\"connectEnd\":8479.754215365769,\"responseStart\":8479.754215365769,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"connectStart\":8479.754215365769,\"domainLookupStart\":8479.754215365769}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; InfoPath.3; MS-RTC LM 8; wbx 1.0.0; rv:11.0) like Gecko\"},\"others\":{\"dom\":null,\"domElementCount\":1028},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"1\",\"BuildNumber\":\"\",\"RunTime\":1505247206484,\"resourceLoadTime\":900.7476421417114,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":1179.8249521408034,\"StartTime\":1505250795234,\"ScriptTime\":2000},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":0,\"totalJSHeapSize\":0,\"jsHeapSizeLimit\":0}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_138_getconfig_success() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getConfig?ClientName=cxoptimisedemo&ProjectName=Test&Scenario=Test&updateRunID=true")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{ClientName:cxoptimisedemo}",result.getResponse().getContentAsString(),false);


    }

    @Test
    public void test_139_insertStats_chrome_hard() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(IOUtils.toString(this.getClass().getResourceAsStream("chrome_hard_2.json"),"UTF-8"))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_140_insertStats_chrome_soft() throws Exception
    {
        String insertJson = "{\"resources\":[{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4402.22,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"fetchStart\":4402.22,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4567.900000000001,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":165.6800000000003,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4316.780000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"fetchStart\":4316.780000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4391.07,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":74.28999999999905,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4703.370000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s09067068335286?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A44%3A42%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=26FDF4259807E36B-2A08E92D7DCB5D9F&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=9AM&v4=9AM&c5=Wednesday&v5=Wednesday&c6=weekday&v6=weekday&c7=New&v7=New&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-29%7C29&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=1&c36=First%20Visit&c37=12&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=1&v42=First%20Visit&v43=12&v47=Landscape%3A1280x918&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.6&v=N&k=Y&bw=1280&bh=918&AQE=1\",\"fetchStart\":4703.370000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4767.06,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":63.6899999999996,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4362.885,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s08176233999131?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A44%3A42%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=26FDF4259807E36B-2A08E92D7DCB5D9F&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.6&v=N&k=Y&bw=1280&bh=918&AQE=1\",\"fetchStart\":4362.885,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4405.39,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":42.50500000000011,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4703.735,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4703.735,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":5024.920000000001,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":321.1850000000013,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4703.655000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4703.655000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4878.165,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":174.5099999999993,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4703.865000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4703.865000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4988.1900000000005,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":284.3249999999998,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4637.485000000001,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"fetchStart\":4637.485000000001,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4662.800000000001,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":25.31500000000051,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4772.615,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"fetchStart\":4772.615,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4929.610000000001,\"secureConnectionStart\":0,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":156.9950000000008,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4703.8,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4703.8,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4928.425000000001,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":224.6250000000009,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":4703.515,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"connectStart\":0,\"decodedBodySize\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"fetchStart\":4703.515,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"responseEnd\":4825.795,\"secureConnectionStart\":0,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":122.27999999999975,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":0},{\"workerStart\":0,\"Content-Length\":\"1004\",\"requestStart\":4769.3150000000005,\"IsImage\":true,\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"nextHopProtocol\":\"http/1.1\",\"startTime\":4705.61,\"domainLookupEnd\":4712.89,\"ResourceType\":\".svg\",\"responseStart\":4785.81,\"connectEnd\":4768.455,\"connectStart\":4712.89,\"decodedBodySize\":1004,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":4705.61,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":true,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"responseEnd\":4788.0650000000005,\"secureConnectionStart\":4757.335,\"initiatorType\":\"css\",\"HostName\":\"www.comcastnow.com\",\"transferSize\":1286,\"Status\":200,\"encodedBodySize\":1004,\"duration\":82.45500000000084,\"toJSON\":{},\"entryType\":\"resource\",\"domainLookupStart\":4712.885}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.79 Safari/537.36\"},\"others\":{\"dom\":null,\"domElementCount\":1028,\"msFirstPaint\":1505313881214},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"2\",\"BuildNumber\":\"\",\"RunTime\":1505313833097,\"resourceLoadTime\":597.9850000000006,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":708.1400000000003,\"StartTime\":1505313884617,\"ScriptTime\":2000},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":21700000,\"totalJSHeapSize\":42100000,\"jsHeapSizeLimit\":2190000000}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_141_insertStats_firefox_hard() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(IOUtils.toString(this.getClass().getResourceAsStream("firefox_hard_2.json"),"UTF-8"))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_142_insertStats_firefox_soft() throws Exception
    {
        String insertJson = "{\"resources\":[{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6170.446068648747,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6170.446068648747,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":6592.596409133625,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":422.15034048487723,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6407.511564005286,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6407.511564005286,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":6502.747156878402,\"initiatorType\":\"other\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":95.23559287311582,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6723.8199008042975,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6723.8199008042975,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7171.780748019843,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":447.9608472155451,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6755.436734925123,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6755.436734925123,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7271.067591595502,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":515.6308566703783,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6758.97562639816,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6758.97562639816,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7185.8435309104925,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":426.86790451233264,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6684.368240132261,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s09202034609738?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A44%3A1%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=7B9B03CD2A5A1310-1C893859341E9F25&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=9AM&v4=9AM&c5=Wednesday&v5=Wednesday&c6=weekday&v6=weekday&c7=New&v7=New&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-29%7C29&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=1&c36=First%20Visit&c37=31&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=1&v42=First%20Visit&v43=31&v47=Landscape%3A1280x913&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.8.5&v=N&k=Y&bw=1280&bh=913&AQE=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6684.368240132261,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7123.292187219676,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":438.9239470874145,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6752.404865606286,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6752.404865606286,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7276.716269991966,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":524.3114043856804,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6267.894495053147,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s04730670609721?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A44%3A0%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=7B9B03CD2A5A1310-1C893859341E9F25&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.8.5&v=N&k=Y&bw=1280&bh=913&AQE=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6267.894495053147,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":6438.969106955399,\"initiatorType\":\"img\",\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":171.0746119022524,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6775.068961171135,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6775.068961171135,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7216.386381261775,\"initiatorType\":\"xmlhttprequest\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":441.31742009063964,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":6761.745979026936,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":6761.745979026936,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7207.533404959621,\"initiatorType\":\"img\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":445.78742593268544,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"requestStart\":0,\"IsImage\":false,\"IsCached\":false,\"nextHopProtocol\":\"http/1.1\",\"startTime\":7098.953892185331,\"domainLookupEnd\":0,\"ResourceType\":\"others\",\"responseStart\":0,\"connectEnd\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"decodedBodySize\":0,\"connectStart\":0,\"fetchStart\":7098.953892185331,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":false,\"secureConnectionStart\":0,\"responseEnd\":7157.847697113587,\"initiatorType\":\"other\",\"HostName\":\"sitecore.comcastnow.com\",\"transferSize\":0,\"encodedBodySize\":0,\"duration\":58.89380492825603,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":0},{\"Content-Length\":\"1004\",\"requestStart\":7112.349130118307,\"IsImage\":true,\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"nextHopProtocol\":\"http/1.1\",\"startTime\":6859.788873511726,\"domainLookupEnd\":6859.788873511726,\"ResourceType\":\".svg\",\"responseStart\":7142.849694411384,\"connectEnd\":6859.788873511726,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"decodedBodySize\":1004,\"connectStart\":6859.788873511726,\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":6859.788873511726,\"redirectStart\":0,\"redirectEnd\":0,\"IsStaticResrc\":true,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"secureConnectionStart\":0,\"responseEnd\":7142.959309727313,\"initiatorType\":\"css\",\"HostName\":\"www.comcastnow.com\",\"transferSize\":1286,\"Status\":200,\"encodedBodySize\":1004,\"duration\":283.17043621558696,\"entryType\":\"resource\",\"toJSON\":\"function toJSON() {\\n    [native code]\\n}\",\"domainLookupStart\":6859.788873511726}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:46.0) Gecko/20100101 Firefox/46.0\"},\"others\":{\"dom\":null,\"domElementCount\":1028,\"msFirstPaint\":1505313838396},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"2\",\"BuildNumber\":\"\",\"RunTime\":1505313775753,\"resourceLoadTime\":1014.4983703445823,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":1106.270201343219,\"StartTime\":1505313842375,\"ScriptTime\":2000},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":0,\"totalJSHeapSize\":0,\"jsHeapSizeLimit\":0}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_143_insertStats_ie_hard() throws Exception
    {
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(IOUtils.toString(this.getClass().getResourceAsStream("ie_hard_2.json"),"UTF-8"))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_144_insertStats_ie_soft() throws Exception
    {
        String insertJson = "{\"resources\":[{\"fetchStart\":8027.5401296745085,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8028.502445406852,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":7839.134392072227,\"domainLookupEnd\":0,\"duration\":189.36805333462507,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8302.958669278276,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8332.610228053229,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8276.155466540438,\"domainLookupEnd\":0,\"duration\":56.454761512790355,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s08014819120170?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A42%3A59%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=9AM&v4=9AM&c5=Wednesday&v5=Wednesday&c6=weekday&v6=weekday&c7=Repeat&v7=Repeat&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-28%7C28&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=6&c36=Less%20than%201%20day&c37=7&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=6&v42=Less%20than%201%20day&v43=7&v47=Landscape%3A1280x908&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8063.601926437859,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8142.53111206996,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8028.506550849395,\"domainLookupEnd\":0,\"duration\":114.02456122056446,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8395.070020375311,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8492.762720611086,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8375.10935872577,\"domainLookupEnd\":0,\"duration\":117.65336188531728,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":7947.859237512578,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":7985.737692601705,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":7937.723720959639,\"domainLookupEnd\":0,\"duration\":48.01397164206628,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s0139445109917?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A42%3A59%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8418.101963592115,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8593.325946273715,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8394.00178422533,\"domainLookupEnd\":0,\"duration\":199.32416204838592,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8837.347292932767,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8838.507490995738,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8453.068428284692,\"domainLookupEnd\":0,\"duration\":385.43906271104606,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8345.80512039005,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8421.741027863229,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8308.555619098683,\"domainLookupEnd\":0,\"duration\":113.18540876454608,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8434.121400399295,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8491.259307551429,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8415.161235097758,\"domainLookupEnd\":0,\"duration\":76.0980724536712,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8871.851074250624,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8908.324236356895,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8838.512417526792,\"domainLookupEnd\":0,\"duration\":69.81181883010322,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8453.292174903348,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8498.459843229572,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8430.77792799136,\"domainLookupEnd\":0,\"duration\":67.68191523821224,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":8614.171330791518,\"redirectStart\":0,\"IsStaticResrc\":true,\"redirectEnd\":0,\"Content-Length\":\"1004\",\"requestStart\":8614.171330791518,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"responseEnd\":8631.97540347263,\"initiatorType\":\"css\",\"IsImage\":true,\"HostName\":\"www.comcastnow.com\",\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"startTime\":8606.509343371414,\"Status\":200,\"domainLookupEnd\":8614.171330791518,\"duration\":25.4660601012165,\"ResourceType\":\".svg\",\"entryType\":\"resource\",\"connectEnd\":8614.171330791518,\"responseStart\":8614.171330791518,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"connectStart\":8614.171330791518,\"domainLookupStart\":8614.171330791518}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; InfoPath.3; MS-RTC LM 8; wbx 1.0.0; rv:11.0) like Gecko\"},\"others\":{\"dom\":null,\"domElementCount\":1028},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"2\",\"BuildNumber\":\"\",\"RunTime\":1505313538300,\"resourceLoadTime\":450.37977396254246,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":960.464998844317,\"StartTime\":1505313781450},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":0,\"totalJSHeapSize\":0,\"jsHeapSizeLimit\":0}}{\"resources\":[{\"fetchStart\":8027.5401296745085,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8028.502445406852,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":7839.134392072227,\"domainLookupEnd\":0,\"duration\":189.36805333462507,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8302.958669278276,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8332.610228053229,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8276.155466540438,\"domainLookupEnd\":0,\"duration\":56.454761512790355,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s08014819120170?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A42%3A59%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=9AM&v4=9AM&c5=Wednesday&v5=Wednesday&c6=weekday&v6=weekday&c7=Repeat&v7=Repeat&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-28%7C28&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=6&c36=Less%20than%201%20day&c37=7&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=6&v42=Less%20than%201%20day&v43=7&v47=Landscape%3A1280x908&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8063.601926437859,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8142.53111206996,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8028.506550849395,\"domainLookupEnd\":0,\"duration\":114.02456122056446,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8395.070020375311,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8492.762720611086,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8375.10935872577,\"domainLookupEnd\":0,\"duration\":117.65336188531728,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":7947.859237512578,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":7985.737692601705,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":7937.723720959639,\"domainLookupEnd\":0,\"duration\":48.01397164206628,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s0139445109917?AQB=1&ndh=1&pf=1&t=13%2F8%2F2017%2010%3A42%3A59%203%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8418.101963592115,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8593.325946273715,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8394.00178422533,\"domainLookupEnd\":0,\"duration\":199.32416204838592,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8837.347292932767,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8838.507490995738,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8453.068428284692,\"domainLookupEnd\":0,\"duration\":385.43906271104606,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8345.80512039005,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8421.741027863229,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8308.555619098683,\"domainLookupEnd\":0,\"duration\":113.18540876454608,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8434.121400399295,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8491.259307551429,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8415.161235097758,\"domainLookupEnd\":0,\"duration\":76.0980724536712,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8871.851074250624,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8908.324236356895,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8838.512417526792,\"domainLookupEnd\":0,\"duration\":69.81181883010322,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8453.292174903348,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8498.459843229572,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8430.77792799136,\"domainLookupEnd\":0,\"duration\":67.68191523821224,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":8614.171330791518,\"redirectStart\":0,\"IsStaticResrc\":true,\"redirectEnd\":0,\"Content-Length\":\"1004\",\"requestStart\":8614.171330791518,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"responseEnd\":8631.97540347263,\"initiatorType\":\"css\",\"IsImage\":true,\"HostName\":\"www.comcastnow.com\",\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"startTime\":8606.509343371414,\"Status\":200,\"domainLookupEnd\":8614.171330791518,\"duration\":25.4660601012165,\"ResourceType\":\".svg\",\"entryType\":\"resource\",\"connectEnd\":8614.171330791518,\"responseStart\":8614.171330791518,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"connectStart\":8614.171330791518,\"domainLookupStart\":8614.171330791518}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; InfoPath.3; MS-RTC LM 8; wbx 1.0.0; rv:11.0) like Gecko\"},\"others\":{\"dom\":null,\"domElementCount\":1028},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"2\",\"BuildNumber\":\"\",\"RunTime\":1505313538300,\"resourceLoadTime\":450.37977396254246,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":960.464998844317,\"StartTime\":1505313781450,\"ScriptTime\":2000},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":0,\"totalJSHeapSize\":0,\"jsHeapSizeLimit\":0}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_145_insertStats_validation() throws Exception
    {
        String insertJson = "{\"resources\":[{\"fetchStart\":8118.485535089012,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8119.274190601739,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":7949.296552947277,\"domainLookupEnd\":0,\"duration\":169.97763765446234,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8303.822864933814,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8365.543677597954,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8302.548946112373,\"domainLookupEnd\":0,\"duration\":62.994731485581724,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8328.035533426308,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8388.43357250273,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8326.416346886905,\"domainLookupEnd\":0,\"duration\":62.01722561582574,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8337.25225193787,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8532.69225479526,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8332.076109978238,\"domainLookupEnd\":0,\"duration\":200.61614481702236,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8341.772344178955,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8418.352806131561,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8340.236087578942,\"domainLookupEnd\":0,\"duration\":78.1167185526192,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8350.803907231779,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8444.91625102482,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8345.547709142533,\"domainLookupEnd\":0,\"duration\":99.3685418822879,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8273.035740751156,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8291.369415520461,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8267.93103349179,\"domainLookupEnd\":0,\"duration\":23.438382028671185,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s56788994646771?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2017%3A13%3A13%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=4PM&v4=4PM&c5=Tuesday&v5=Tuesday&c6=weekday&v6=weekday&c7=Repeat&v7=Repeat&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-28%7C28&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=5&c36=Less%20than%201%20day&c37=8&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=5&v42=Less%20than%201%20day&v43=8&v47=Landscape%3A1280x908&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8047.868638976004,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8064.981355132686,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8040.094572974446,\"domainLookupEnd\":0,\"duration\":24.88678215823984,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s51820629878260?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2017%3A13%3A13%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8666.881107615554,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":9227.693591116808,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8618.28950020753,\"domainLookupEnd\":0,\"duration\":609.4040909092782,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8145.350319465011,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8219.30083492385,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8119.277885500029,\"domainLookupEnd\":0,\"duration\":100.02294942382105,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8617.406419516288,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8618.286626397748,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8368.767271083603,\"domainLookupEnd\":0,\"duration\":249.5193553141453,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":8479.754215365769,\"redirectStart\":0,\"IsStaticResrc\":true,\"redirectEnd\":0,\"Content-Length\":\"1004\",\"requestStart\":8479.754215365769,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"responseEnd\":8488.672468204373,\"initiatorType\":\"css\",\"IsImage\":true,\"HostName\":\"www.comcastnow.com\",\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"startTime\":8477.669882186116,\"Status\":200,\"domainLookupEnd\":8479.754215365769,\"duration\":11.002586018257716,\"ResourceType\":\".svg\",\"entryType\":\"resource\",\"connectEnd\":8479.754215365769,\"responseStart\":8479.754215365769,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"connectStart\":8479.754215365769,\"domainLookupStart\":8479.754215365769}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; InfoPath.3; MS-RTC LM 8; wbx 1.0.0; rv:11.0) like Gecko\"},\"others\":{\"dom\":null,\"domElementCount\":1028},\"host\":{\"name\":\"HQSWL-C008775\"},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":0,\"totalJSHeapSize\":0,\"jsHeapSizeLimit\":0}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Reason:Invalid request}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_146_insertStats_configcheck() throws Exception
    {
        String insertJson = "{\"resources\":[{\"fetchStart\":8118.485535089012,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8119.274190601739,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":7949.296552947277,\"domainLookupEnd\":0,\"duration\":169.97763765446234,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8303.822864933814,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8365.543677597954,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8302.548946112373,\"domainLookupEnd\":0,\"duration\":62.994731485581724,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/Benefits%20750x500/Benefits_LandingPage750/Benefits_LandingPage2_750/Benefits_LandingPage3_750.ashx?bc=White&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8328.035533426308,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8388.43357250273,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8326.416346886905,\"domainLookupEnd\":0,\"duration\":62.01722561582574,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/09_ELDER_WOMAN_B_0419.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8337.25225193787,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8532.69225479526,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8332.076109978238,\"domainLookupEnd\":0,\"duration\":200.61614481702236,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Evergreen%20Image%20Library/Working%20at%20Comcast/Benefits/TRS.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8341.772344178955,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8418.352806131561,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8340.236087578942,\"domainLookupEnd\":0,\"duration\":78.1167185526192,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/Accolade%20Hero%20Image%20002x3.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8350.803907231779,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8444.91625102482,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8345.547709142533,\"domainLookupEnd\":0,\"duration\":99.3685418822879,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/-/media/ComcastNow%20Feed%20Content%20Image%20Library/Benefits/Military%20Benefits%20Image.ashx?bc=White&as=1&h=500&mh=500&mw=750&w=750\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8273.035740751156,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8291.369415520461,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8267.93103349179,\"domainLookupEnd\":0,\"duration\":23.438382028671185,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s56788994646771?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2017%3A13%3A13%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Benefits%2C%20Perks%20%26%20Courtesy%20Services&g=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&ch=Working%20at%20Comcast&events=event3&c1=D%3Dv1&h1=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&c4=4PM&v4=4PM&c5=Tuesday&v5=Tuesday&c6=weekday&v6=weekday&c7=Repeat&v7=Repeat&c10=NA&c11=NA&c12=NA&v12=US&c13=NA&c14=NA&v14=NA&v15=NA&c16=US&v17=NA&v18=NA&v19=NA&c20=Homepage-28%7C28&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&c32=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&c34=D%3Dv39&c35=5&c36=Less%20than%201%20day&c37=8&v39=Desktop&v40=https%3A%2F%2Fwww.comcastnow.com%2Flanding%2F%257B7C851D3B-E512-4632-9FBC-9520D43E6AC2%257D&v41=5&v42=Less%20than%201%20day&v43=8&v47=Landscape%3A1280x908&v53=Updates&v60=Internal&v61=AS&c74=de4decdc-c6d9-4c61-a967-ab9e44565816&v74=de4decdc-c6d9-4c61-a967-ab9e44565816&c75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Working%20at%20Comcast%7CBenefits%2C%20Perks%20%26%20Courtesy%20Services&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8047.868638976004,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8064.981355132686,\"initiatorType\":\"img\",\"IsImage\":false,\"HostName\":\"teamcomcast-s.sc.omtrdc.net\",\"IsCached\":false,\"startTime\":8040.094572974446,\"domainLookupEnd\":0,\"duration\":24.88678215823984,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://teamcomcast-s.sc.omtrdc.net/b/ss/teamcomcastcomcastnowprod/1/JS-1.7.0-D7QN/s51820629878260?AQB=1&ndh=1&pf=1&t=12%2F8%2F2017%2017%3A13%3A13%202%20240&D=D%3D&vid=de4decdc-c6d9-4c61-a967-ab9e44565816&fid=09720FFAEDAAB28A-36D3F5F6571216DC&ce=UTF-8&pageName=Homepage&g=https%3A%2F%2Fwww.comcastnow.com%2F&events=event55&c24=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v26=Benefits%2C%20Perks%20%26%20Courtesy%20Services&v51=Navigation%20Click%3ABenefits%2C%20Perks%20%26%20Courtesy%20Services&v75=Homepage&pe=lnk_o&pev2=New%20Internal%20Link%20Click%20-%20Navigation&c.&a.&activitymap.&page=https%3A%2F%2Fwww.comcastnow.com%2F&link=Benefits%2C%20Perks%20%26%20Courtesy%20Services&region=BODY&.activitymap&.a&.c&s=1280x1024&c=24&j=1.6&v=Y&k=Y&bw=1280&bh=908&AQE=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8666.881107615554,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":9227.693591116808,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8618.28950020753,\"domainLookupEnd\":0,\"duration\":609.4040909092782,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8145.350319465011,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8219.30083492385,\"initiatorType\":\"xmlhttprequest\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8119.277885500029,\"domainLookupEnd\":0,\"duration\":100.02294942382105,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"connectStart\":0,\"domainLookupStart\":0},{\"fetchStart\":8617.406419516288,\"redirectStart\":0,\"IsStaticResrc\":false,\"redirectEnd\":0,\"requestStart\":0,\"responseEnd\":8618.286626397748,\"initiatorType\":\"preflight\",\"IsImage\":false,\"HostName\":\"sitecore.comcastnow.com\",\"IsCached\":false,\"startTime\":8368.767271083603,\"domainLookupEnd\":0,\"duration\":249.5193553141453,\"ResourceType\":\"others\",\"entryType\":\"resource\",\"connectEnd\":0,\"responseStart\":0,\"name\":\"https://sitecore.comcastnow.com/content_delivery/api/feed/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D?page=1\",\"connectStart\":0,\"domainLookupStart\":0},{\"ETag\":\"06dd470e31cd31:0\",\"fetchStart\":8479.754215365769,\"redirectStart\":0,\"IsStaticResrc\":true,\"redirectEnd\":0,\"Content-Length\":\"1004\",\"requestStart\":8479.754215365769,\"Last-Modified\":\"Thu 24 Aug 2017 14:15:30 GMT\",\"responseEnd\":8488.672468204373,\"initiatorType\":\"css\",\"IsImage\":true,\"HostName\":\"www.comcastnow.com\",\"IsCached\":false,\"Cache-Control\":\"max-age#691200\",\"startTime\":8477.669882186116,\"Status\":200,\"domainLookupEnd\":8479.754215365769,\"duration\":11.002586018257716,\"ResourceType\":\".svg\",\"entryType\":\"resource\",\"connectEnd\":8479.754215365769,\"responseStart\":8479.754215365769,\"name\":\"https://www.comcastnow.com/Content/images/icon-external-link.svg\",\"connectStart\":8479.754215365769,\"domainLookupStart\":8479.754215365769}],\"platform\":{\"UserAgent\":\"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; InfoPath.3; MS-RTC LM 8; wbx 1.0.0; rv:11.0) like Gecko\"},\"others\":{\"dom\":null,\"domElementCount\":1028},\"host\":{\"name\":\"HQSWL-C008775\"},\"details\":{\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"ProjectName\":\"test1\",\"Scenario\":\"test\",\"NavType\":\"Soft\",\"dataStoreUrl\":null,\"licenseKey\":null,\"txnStatus\":1,\"transactionName\":\"SoftNavigation\",\"url\":\"https://www.comcastnow.com/landing/%7B7C851D3B-E512-4632-9FBC-9520D43E6AC2%7D\",\"Release\":\"\",\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"source\":\"Selenium\",\"RunID\":\"1\",\"BuildNumber\":\"\",\"RunTime\":1505247206484,\"resourceLoadTime\":900.7476421417114,\"ClientName\":\"cxoptimisedemo\",\"resourceDurationThreshold\":5,\"visuallyComplete\":1179.8249521408034,\"StartTime\":1505250795234},\"memory\":{\"currentPageUsage\":0,\"usedJSHeapSize\":0,\"totalJSHeapSize\":0,\"jsHeapSizeLimit\":0}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/insertStats")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(insertJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        JSONAssert.assertEquals("{Reason:Configuration UnAvailable}",result.getResponse().getContentAsString(),false);

    }


    @Test
    public void test_148_getSummaryReport_success() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
         Assert.assertTrue(result.getResponse().getContentAsString().contains("header"));

    }

    @Test
    public void test_149_getSummaryReport_cached() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("header"));

    }

    @Test
    public void test_150_getSummaryReport_override() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1&Override=true")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("header"));

    }

    @Test
    public void test_151_getSummaryReport_default() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("header"));

    }

    @Test
    public void test_152_getSummaryReport_validation() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=&Scenario=test&AnalysisType=Run")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:ClientName or ProjectName or Scenario cannot be null or blank}",result.getResponse().getContentAsString(),false);

    }


    @Test
    public void test_154_getSummaryReport_analysistype() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=script")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid Analysis Type only Run or Transaction or Time allowed}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_155_getSummaryReport_runid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=s2&BaselineRunID=2")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid RunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_156_getSummaryReport_baselinerunid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=q2")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid BaselineRunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_157_getSummaryReport_config() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getSummaryReport?ClientName=cxoptimisedemo&ProjectName=test1&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_158_getSummaryReportWithStatus_success() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"AnalysisType\":\"Run\",\"RunID\":2,\"BaselineRunID\":1}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getSummaryReportWithStatus")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("compliance"));

    }

    @Test
    public void test_159_getSummaryReportWithStatus_validationfailure() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"\",\"Scenario\":\"test\",\"AnalysisType\":\"Run\",\"RunID\":2,\"BaselineRunID\":1}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getSummaryReportWithStatus")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("ClientName or ProjectName or Scenario cannot be null or blank"));

    }



    @Test
    public void test_161_getSummaryReportWithStatus_analysistypefailure() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"AnalysisType\":\"script\",\"RunID\":2,\"BaselineRunID\":1}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getSummaryReportWithStatus")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid Analysis Type only Run or Transaction or Time allowed}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_162_getSummaryReportWithStatus_runidfailure() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"AnalysisType\":\"Run\",\"RunID\":s2,\"BaselineRunID\":1}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getSummaryReportWithStatus")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid RunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_163_getSummaryReportWithStatus_baselinerunidfailure() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"AnalysisType\":\"Run\",\"RunID\":2,\"BaselineRunID\":s1}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getSummaryReportWithStatus")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid BaselineRunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_164_getSummaryReportWithStatus_configfailure() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test1\",\"Scenario\":\"test\",\"AnalysisType\":\"Run\",\"RunID\":2,\"BaselineRunID\":1}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getSummaryReportWithStatus")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_165_getSummaryReportWithStatus_modifythreshold() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"AnalysisType\":\"Run\",\"RunID\":2,\"BaselineRunID\":1,\"excludeTransactionsList\":\"\",\"totalScoreThreshold\":90,\"compareDeviationThreshold\":\"\",\"compareComplianceThreshold\":\"\",\"individualScoreThreshold\":\"\",\"indvScoreComplianceThreshold\":\"\",\"slaComplianceThreshold\":\"\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getSummaryReportWithStatus")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);


        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
        Assert.assertTrue(result.getResponse().getContentAsString().contains("Total Score :86.0 less than configured threshold 90"));

    }

    @Test
    public void test_166_getCBAnalysis_success() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getCBAnalysis?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertTrue(result.getResponse().getContentAsString().contains("UA"));

    }

    @Test
    public void test_167_getCBAnalysis_success_default() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getCBAnalysis?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("UA"));

    }

    @Test
    public void test_168_getCBAnalysis_validation() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getCBAnalysis?ClientName=cxoptimisedemo&ProjectName=&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:ClientName or ProjectName or Scenario cannot be null or blank}",result.getResponse().getContentAsString(),false);

    }


    @Test
    public void test_170_getCBAnalysis_analysistype() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getCBAnalysis?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=script&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid Analysis Type only Run or Transaction or Time allowed}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_171_getCBAnalysis_runid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getCBAnalysis?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation&RunID=s2")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid RunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_172_getCBAnalysis_transactionname() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getCBAnalysis?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid TransactionName}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_173_getCBAnalysis_config() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getCBAnalysis?ClientName=cxoptimisedemo&ProjectName=test1&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_174_getAllTransactionSamples_success() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getAllTransactionSamples?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertTrue(result.getResponse().getContentAsString().contains("HardNavigation"));

    }

    @Test
    public void test_175_getAllTransactionSamples_success_default() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getAllTransactionSamples?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("HardNavigation"));

    }

    @Test
    public void test_176_getAllTransactionSamples_validation() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getAllTransactionSamples?ClientName=cxoptimisedemo&ProjectName=&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:ClientName or ProjectName or Scenario cannot be null or blank}",result.getResponse().getContentAsString(),false);

    }


    @Test
    public void test_178_getAllTransactionSamples_analysistype() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getAllTransactionSamples?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=script&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid Analysis Type only Run or Transaction or Time allowed}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_179_getAllTransactionSamples_runid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getAllTransactionSamples?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation&RunID=s2")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid RunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_180_getAllTransactionSamples_transactionname() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getAllTransactionSamples?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid TransactionName}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_181_getAllTransactionSamples_config() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getAllTransactionSamples?ClientName=cxoptimisedemo&ProjectName=test1&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_182_compareResources_success() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1&TransactionName=HardNavigation&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertTrue(result.getResponse().getContentAsString().contains("duration"));

    }

    @Test
    public void test_183_compareResources_success_default() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("duration"));

    }

    @Test
    public void test_184_compareResources_validation() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1&TransactionName=HardNavigation&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:ClientName or ProjectName or Scenario cannot be null or blank}",result.getResponse().getContentAsString(),false);

    }


    @Test
    public void test_186_compareResources_analysistype() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=script&RunID=2&BaselineRunID=1&TransactionName=HardNavigation&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid Analysis Type only Run or Transaction or Time allowed}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_187_compareResources_runid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=s2&BaselineRunID=1&TransactionName=HardNavigation&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid RunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_188_compareResources_baselinerunid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=s1&TransactionName=HardNavigation&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid BaselineRunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_189_compareResources_transactionname() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1&TransactionName=&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid TransactionName}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_190_compareResources_config() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/compareResources?ClientName=cxoptimisedemo&ProjectName=test1&Scenario=test&AnalysisType=Run&RunID=2&BaselineRunID=1&TransactionName=HardNavigation&CurrentSampleValue=3747&BaselineSampleValue=3638")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_191_getHar_success() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&TransactionName=HardNavigation&SampleValue=3747")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertTrue(result.getResponse().getContentAsString().contains("cxoptimise"));

    }

    @Test
    public void test_192_getHar_success_default_sample() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&TransactionName=HardNavigation")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("cxoptimise"));

    }

    @Test
    public void test_193_getHar_success_defaultrunid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&TransactionName=HardNavigation&SampleValue=3747")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        Assert.assertTrue(result.getResponse().getContentAsString().contains("cxoptimise"));

    }

    @Test
    public void test_194_getHar_validation() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=&Scenario=test&AnalysisType=Run&RunID=2&TransactionName=HardNavigation&SampleValue=3747")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:ClientName or ProjectName or Scenario cannot be null or blank}",result.getResponse().getContentAsString(),false);

    }


    @Test
    public void test_196_getHar_analysistype() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=script&RunID=2&TransactionName=HardNavigation&SampleValue=3747")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid Analysis Type only Run or Transaction or Time allowed}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_197_getHar_runid() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=s2&TransactionName=HardNavigation&SampleValue=3747")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid RunID}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_198_getHar_transactionname() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=test&Scenario=test&AnalysisType=Run&RunID=2&TransactionName=&SampleValue=3747")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:Invalid TransactionName}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_199_getHar_config() throws Exception
    {

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/getHAR?ClientName=cxoptimisedemo&ProjectName=test1&Scenario=test&AnalysisType=Run&RunID=2&TransactionName=HardNavigation&SampleValue=3747")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_200_getClientDetails_success() throws Exception
    {

        String inputJson="{\"UserName\": \"cxopadmin\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getClientDetails")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("cxoptimisedemo"));

    }

    @Test
    public void test_201_getClientDetails_failure() throws Exception
    {

        String inputJson="{\"UserName\": \"cxopadmin1\"}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/getClientDetails")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(!result.getResponse().getContentAsString().contains("cxoptimisedemo"));

    }

    @Test
    public void test_202_updateConfig_success() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"Scenario\":\"test\",\"runIntervalInMinutes\":30}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Status:Success}",result.getResponse().getContentAsString(),false);

    }


    @Test
    public void test_204_updateConfig_config() throws Exception
    {

        String inputJson="{\"ClientName\":\"cxoptimisedemo\",\"ProjectName\":\"test1\",\"Scenario\":\"test\",\"runIntervalInMinutes\":30}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Reason:No configuration for the given input defined in the system}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_205_updateConfig_rule_weight_validation() throws Exception
    {

        String inputJson="{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimisedemo\",\"ClientNameOrg\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"ProjectNameOrg\":\"Test\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"High\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":2,\"Scenario\":\"test\",\"ScenarioOrg\":\"Test\",\"comments\":\"Config file was created by admin\",\"creationTimestamp\":1505424425873,\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimise/app/kibana#/dashboard/CXOptimise_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimisedemo%20AND%20ProjectName:test%20AND%20Scenario:test')),title:CXOptimise_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":30,\"runTimestamp\":1505424458420,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"transactionSLA\":{}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Comment:Ignored Generic Rules Update since sum of weight is not equal to 100}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_206_updateConfig_rule_priority_validation() throws Exception
    {

        String inputJson="{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimisedemo\",\"ClientNameOrg\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"ProjectNameOrg\":\"Test\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":2},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"Less\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":2,\"Scenario\":\"test\",\"ScenarioOrg\":\"Test\",\"comments\":\"Config file was created by admin\",\"creationTimestamp\":1505424425873,\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimise/app/kibana#/dashboard/CXOptimise_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimisedemo%20AND%20ProjectName:test%20AND%20Scenario:test')),title:CXOptimise_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":30,\"runTimestamp\":1505424458420,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"transactionSLA\":{}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        Assert.assertTrue(result.getResponse().getContentAsString().contains("Ignored Generic Rules Update since priority allowed for rules are only Low or Medium or High"));

    }

    @Test
    public void test_205_updateConfig_softrule_weight_validation() throws Exception
    {

        String inputJson="{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimisedemo\",\"ClientNameOrg\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"ProjectNameOrg\":\"Test\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":2},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"High\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":7},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":20},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":2,\"Scenario\":\"test\",\"ScenarioOrg\":\"Test\",\"comments\":\"Config file was created by admin\",\"creationTimestamp\":1505424425873,\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimise/app/kibana#/dashboard/CXOptimise_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimisedemo%20AND%20ProjectName:test%20AND%20Scenario:test')),title:CXOptimise_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":30,\"runTimestamp\":1505424458420,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"transactionSLA\":{}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Comment:Ignored Soft Navigation Rules Update since sum of weight is not equal to 100}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_207_updateConfig_softrule_priority_validation() throws Exception
    {

        String inputJson="{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimisedemo\",\"ClientNameOrg\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"ProjectNameOrg\":\"Test\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":2},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"High\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Less\",\"weight\":6},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":2,\"Scenario\":\"test\",\"ScenarioOrg\":\"Test\",\"comments\":\"Config file was created by admin\",\"creationTimestamp\":1505424425873,\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimise/app/kibana#/dashboard/CXOptimise_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimisedemo%20AND%20ProjectName:test%20AND%20Scenario:test')),title:CXOptimise_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":30,\"runTimestamp\":1505424458420,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"transactionSLA\":{}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Comment:Ignored Soft Navigation Rules Update since priority allowed for rules are only Low or Medium or High}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_208_updateConfig_multirule_weight_validation() throws Exception
    {

        String inputJson="{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimisedemo\",\"ClientNameOrg\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"ProjectNameOrg\":\"Test\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"High\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":10},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":2,\"Scenario\":\"test\",\"ScenarioOrg\":\"Test\",\"comments\":\"Config file was created by admin\",\"creationTimestamp\":1505424425873,\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimise/app/kibana#/dashboard/CXOptimise_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimisedemo%20AND%20ProjectName:test%20AND%20Scenario:test')),title:CXOptimise_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":30,\"runTimestamp\":1505424458420,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"transactionSLA\":{}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Comment:Ignored Generic Rules Update since sum of weight is not equal to 100 and Ignored Soft Navigation Rules Update since sum of weight is not equal to 100}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_209_updateConfig_multirule_multi_validation() throws Exception
    {

        String inputJson="{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimisedemo\",\"ClientNameOrg\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"ProjectNameOrg\":\"Test\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":2},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"High\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Avg\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":10},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":2,\"Scenario\":\"test\",\"ScenarioOrg\":\"Test\",\"comments\":\"Config file was created by admin\",\"creationTimestamp\":1505424425873,\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimise/app/kibana#/dashboard/CXOptimise_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimisedemo%20AND%20ProjectName:test%20AND%20Scenario:test')),title:CXOptimise_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":30,\"runTimestamp\":1505424458420,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"transactionSLA\":{}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Comment:Ignored Generic Rules Update since priority allowed for rules are only Low or Medium or High and Ignored Soft Navigation Rules Update since sum of weight is not equal to 100}",result.getResponse().getContentAsString(),false);

    }

    @Test
    public void test_210_updateConfig_multirule_multi_validation() throws Exception
    {

        String inputJson="{\"BaselineRunID\":1,\"BuildNumber\":\"\",\"ClientName\":\"cxoptimisedemo\",\"ClientNameOrg\":\"cxoptimisedemo\",\"ProjectName\":\"test\",\"ProjectNameOrg\":\"Test\",\"Release\":\"\",\"Rules\":{\"CBTRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"comparisonRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"genericRules\":[{\"description\":\"Avoid CSS @import\",\"id\":1,\"priority\":\"Medium\",\"weight\":2},{\"description\":\"Minify HTML Document\",\"id\":2,\"priority\":\"High\",\"weight\":5},{\"description\":\"Minimize iFrames Usage\",\"id\":3,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid empty SRC or HREF\",\"id\":4,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Specify image dimensions\",\"id\":5,\"priority\":\"High\",\"weight\":6},{\"description\":\"Do not scale images in HTML\",\"id\":6,\"priority\":\"Avg\",\"weight\":3},{\"description\":\"Remove unnecessary comments\",\"id\":7,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid a character set in the META tag\",\"id\":8,\"priority\":\"Medium\",\"weight\":5},{\"description\":\"Make CSS external\",\"id\":9,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put CSS at the top\",\"id\":10,\"priority\":\"High\",\"weight\":8},{\"description\":\"Make JS external\",\"id\":11,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Put JavaScript at bottom\",\"id\":12,\"priority\":\"High\",\"weight\":8},{\"description\":\"Prefer asynchronous resources\",\"id\":13,\"priority\":\"High\",\"weight\":8},{\"description\":\"Reduce the number of DOM elements\",\"id\":14,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":3},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"High\",\"weight\":1},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":1},{\"description\":\"Parallelize downloads across hostnames\",\"id\":19,\"priority\":\"High\",\"weight\":2},{\"description\":\"Avoid Landing Page redirects\",\"id\":20,\"priority\":\"High\",\"weight\":1},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":5},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":2},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":1},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":1},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":4},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":4},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":4}],\"mobileRules\":[{\"description\":\"Rule Description\",\"id\":\"a12\"},{\"description\":\"Rule Description\",\"id\":\"a13\"}],\"nativeAppRules\":[{\"description\":\"Minimize HTTP Calls\",\"id\":1,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Payload\",\"id\":2,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid Duplicate Resources\",\"id\":3,\"priority\":\"High\",\"weight\":25},{\"description\":\"Minimize Resource Payload\",\"id\":4,\"priority\":\"High\",\"weight\":15},{\"description\":\"Avoid Bad Requests\",\"id\":5,\"priority\":\"High\",\"weight\":10}],\"softNavigationRules\":[{\"description\":\"Avoid Bad requests\",\"id\":15,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Make fewer HTTP requests\",\"id\":16,\"priority\":\"High\",\"weight\":10},{\"description\":\"Optimize images PNG or JPG\",\"id\":17,\"priority\":\"Light\",\"weight\":6},{\"description\":\"Use PNG or JPG instead of GIF\",\"id\":18,\"priority\":\"High\",\"weight\":10},{\"description\":\"Avoid URL redirects resources\",\"id\":21,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Set Proper Cache Header\",\"id\":22,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a cache validator\",\"id\":23,\"priority\":\"High\",\"weight\":10},{\"description\":\"Specify a Vary: Accept-Encoding header\",\"id\":24,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Enable Keep Alive\",\"id\":25,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Duplicate Resources\",\"id\":26,\"priority\":\"Medium\",\"weight\":6},{\"description\":\"Avoid Resources with Query String\",\"id\":27,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Avoid Flash\",\"id\":28,\"priority\":\"Low\",\"weight\":2},{\"description\":\"Compress Static files\",\"id\":29,\"priority\":\"High\",\"weight\":10},{\"description\":\"Minify CSS and JS\",\"id\":30,\"priority\":\"High\",\"weight\":10},{\"description\":\"Increase caching duration\",\"id\":31,\"priority\":\"High\",\"weight\":8}]},\"RunID\":2,\"Scenario\":\"test\",\"ScenarioOrg\":\"Test\",\"comments\":\"Config file was created by admin\",\"creationTimestamp\":1505424425873,\"defaultTransactionThreshold\":5000,\"devThreshold\":5,\"domCountThreshold\":1500,\"excludedCBTRules\":\"\",\"excludedGeneriRules\":\"\",\"excludedMobileRules\":\"\",\"httpCallsThreshold\":50,\"imageResourceExtension\":\".png,.jpg,.jpeg,.gif,.swf,.ico,.svg,.svgz,.bmp,.tif,.tiff,.pict\",\"isDOMNeeded\":true,\"isLoadTest\":false,\"isMarkAPIEnabled\":false,\"isMemoryAPIEnabled\":true,\"isNativeApp\":false,\"isNavigationAPIEnabled\":true,\"isResourceAPIEnabled\":true,\"isResourceCrawlingEnabled\":true,\"kibanaURL\":\"/cxoptimise/app/kibana#/dashboard/CXOptimise_Dashboard?_g=(refreshInterval:(display:Off,pause:!f,value:0),time:(from:now-7d%2Fd,mode:relative,to:now))&_a=(filters:!(),options:(darkTheme:!f),panels:!((col:1,id:Test-Execution-Summary-Board,panelIndex:1,row:1,size_x:12,size_y:2,type:visualization),(col:1,id:Transaction-level-Response-times-Comparison,panelIndex:2,row:3,size_x:6,size_y:4,type:visualization),(col:7,id:Total-Page-Load-Time-Trending,panelIndex:3,row:3,size_x:6,size_y:4,type:visualization),(col:1,id:Client-Time-Transaction-BreakDown,panelIndex:4,row:7,size_x:4,size_y:4,type:visualization),(col:5,id:Server-Time-Transaction-BreakDown,panelIndex:5,row:7,size_x:4,size_y:4,type:visualization),(col:9,id:Transaction-Break-Down,panelIndex:6,row:7,size_x:4,size_y:4,type:visualization)),query:(query_string:(analyze_wildcard:!t,query:'ClientName:cxoptimisedemo%20AND%20ProjectName:test%20AND%20Scenario:test')),title:CXOptimise_Dashboard,uiState:(P-4:(vis:(legendOpen:!t))))\",\"resourceDurationThreshold\":5,\"runIntervalInMinutes\":30,\"runTimestamp\":1505424458420,\"samplePercentile\":\"Pcnt95\",\"samplesCount\":10,\"staticResourceExtension\":\".js,.css,.html,.ttf,.otf,.docx,.doc\",\"transactionSLA\":{}}";
        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/updateConfig")
                .header("Authorization",authToken)
                .accept(MediaType.APPLICATION_JSON).content(inputJson)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        JSONAssert.assertEquals("{Comment:Ignored Generic Rules Update since priority allowed for rules are only Low or Medium or High and priority allowed for Soft rules are only Low or Medium or High}",result.getResponse().getContentAsString(),false);

    }




    @Test
    public void z_deleteallindex() throws Exception
    {
/*
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200","masterconfig");
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200","statsindex");
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200","analysisindex");
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200","baselineindex");
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200","markindex");
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200","userdetails");
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200","licenseindex");
        ElasticSearchUtils.elasticSearchDELETE("http://localhost:9200",".kibana");*/
    }



}