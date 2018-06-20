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

package com.cognizant.pace.CXOptimize.api.config;


import com.cognizant.pace.CXOptimize.api.security.AuthenticationProvider;
import com.cognizant.pace.CXOptimize.api.security.JWTAuthenticationFilter;
import com.cognizant.pace.CXOptimize.api.security.JWTLoginFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter{

    @Autowired
    private AuthenticationProvider authProvider;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authProvider);

    }


    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        http.csrf().disable().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                /* .antMatchers("/*").permitAll();*/
                .antMatchers(HttpMethod.GET,"/v2*").permitAll()
                //.antMatchers(HttpMethod.GET,"/swagger*").permitAll()
                //.antMatchers(HttpMethod.GET,"/webjars*").permitAll()
                .antMatchers(HttpMethod.OPTIONS,"/*").permitAll()
                .antMatchers(HttpMethod.POST,"/login*").permitAll()
                .antMatchers(HttpMethod.POST,"/authToken*").permitAll()
                .antMatchers(HttpMethod.POST,"/insertStats*").permitAll()
                .antMatchers(HttpMethod.POST,"/insertBoomerangStats*").permitAll()
                .antMatchers(HttpMethod.GET,"/checkHealth*").permitAll()
                .antMatchers(HttpMethod.GET,"/getConfig*").permitAll()
                .anyRequest().authenticated()
                .and()
                // We filter the api/login requests
                .addFilterBefore(new JWTLoginFilter("/authToken", authenticationManager()),UsernamePasswordAuthenticationFilter.class)
                // And filter other requests to check the presence of JWT in header
                .addFilterBefore(new JWTAuthenticationFilter(),UsernamePasswordAuthenticationFilter.class);

    }


}