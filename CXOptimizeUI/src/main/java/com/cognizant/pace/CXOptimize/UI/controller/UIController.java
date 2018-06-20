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

package com.cognizant.pace.CXOptimize.UI.controller;

import com.cognizant.pace.CXOptimize.UI.config.HTTPUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;


@Controller
public class UIController
{
    @Value("${login.url}")
    private String loginAPIUrl;

    @Value("${auth.request}")
    private String authRequest;

    @GetMapping("/login")
    public String loginpage()
    {
        return "login";
    }

    @PostMapping("/login")
    public String loginpagepost()
    {
        return "login";
    }


    @GetMapping("/Landing")
    public String landingPage(Model model,HttpServletResponse  response)
    {
        Cookie co = new Cookie("token",HTTPUtils.getHTTPHeader(loginAPIUrl + "authToken",authRequest).replace("Bearer ",""));
        response.addCookie(co);
        model.addAttribute("cxop", new CXOptimiseInput());
        return "Landing";
    }

    @PostMapping("/Dashboard")
    public String dashboardPage(@ModelAttribute CXOptimiseInput cxop)
    {
            //System.out.println(cxop.getClientName());
            return "Dashboard";
    }

    @GetMapping("/Settings")
    public String settingsPage(Model model)
    {
        model.addAttribute("cxop", new CXOptimiseInput());
        return "Settings";
    }

    @GetMapping("/TransactionSamples")
    public String samplesPage() {
        return "TransactionSamples";
    }

    @GetMapping("/ResourceComparison")
    public String resourceComparison() {
        return "ResourceComparison";
    }

    @GetMapping("/BrowserComparison")
    public String browserComparison() {
        return "BrowserComparison";
    }


    @GetMapping("/har/HarViewer")
    public String harViewer() {
        return "har/HarViewer";
    }

    @GetMapping("/403")
    public String error403() {
        return "/error/403";
    }

    @GetMapping("/error/403")
    public String error4031() {
        return "/error/403";
    }

}
