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

package com.cognizant.pace.CXOptimize.api.security;

import com.cognizant.pace.CXOptimize.AnalysisEngine.EncryptionUtils;
import com.cognizant.pace.CXOptimize.AnalysisEngine.GlobalConstants;
import com.cognizant.pace.CXOptimize.AnalysisEngine.JsonUtils;
import com.cognizant.pace.CXOptimize.AnalysisEngine.LoginUtils;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuthenticationProvider implements org.springframework.security.authentication.AuthenticationProvider
{

    private static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AuthenticationProvider.class);

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {

        List<GrantedAuthority> grantedAuths = new ArrayList<>();

        Map<String,Object> loginStatus = new HashMap<>();

        try
        {
            loginStatus = JsonUtils.jsonToMap(new JSONObject(LoginUtils.Login(GlobalConstants.getESUrl(),authentication.getName(),authentication.getCredentials()).toString()));
        }
        catch (Exception ex)
        {
            LOGGER.error("Error in authenticating user");
        }

        LOGGER.debug("Login Status : {}",loginStatus);

        if (Boolean.parseBoolean(loginStatus.get("status").toString()))
        {
            if(loginStatus.containsKey("role"))
            {
                grantedAuths.add(new SimpleGrantedAuthority(loginStatus.get("role").toString()));
            }
            else
            {
                grantedAuths.add(new SimpleGrantedAuthority("USER"));
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

