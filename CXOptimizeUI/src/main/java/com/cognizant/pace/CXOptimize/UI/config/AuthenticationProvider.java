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

package com.cognizant.pace.CXOptimize.UI.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cognizant.pace.CXOptimize.UI.controller.HTTPUtils;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.cognizant.pace.CXOptimize.UI.EncryptionUtils;
import com.cognizant.pace.CXOptimize.UI.JsonUtils;


@Component
public class AuthenticationProvider implements org.springframework.security.authentication.AuthenticationProvider
{
    @Autowired
    private HTTPUtils httpUtils;

    @Value("${login.url}")
    private String loginAPIUrl;

    @Value("${secret.key}")
    private String secretKey;

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(AuthenticationProvider.class);

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {

        List<GrantedAuthority> grantedAuths = new ArrayList<GrantedAuthority>();

        StringBuilder loginReq = new StringBuilder();
        Map<String,Object> loginStatus = new HashMap<>();
        try
        {
            loginReq.append("{\"UserName\" : \"").append(authentication.getName()).append("\",");
            loginReq.append("\"Password\" : \"").append(EncryptionUtils.getEncryptedPassword(authentication.getCredentials(),secretKey)).append("\"}");
            loginStatus = JsonUtils.jsonToMap(new JSONObject(httpUtils.getLoginStatus(loginReq.toString())));
        }
        catch (Exception ex)
        {
            logger.error("Error to authenticate user");
        }

        if (Boolean.parseBoolean(loginStatus.get("status").toString()))
        {
            if(loginStatus.containsKey("role"))
            {
                if(loginStatus.get("role").toString().contains("USER")) {
                    grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
                }
                if(loginStatus.get("role").toString().contains("ADMIN")) {
                    grantedAuths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }
            }
            else
            {
                grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
            }

            return new UsernamePasswordAuthenticationToken(authentication.getName(),authentication.getCredentials(),grantedAuths);
        }
        else
        {
            return null;
        }

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
