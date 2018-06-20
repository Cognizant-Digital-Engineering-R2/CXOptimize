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

import com.cognizant.pace.CXOptimize.AnalysisEngine.GlobalConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


class TokenAuthenticationService
{


    //static final long EXPIRATIONTIME = GlobalConstants.getExpirationTime(); // 10 minutes
    //static final String SECRET = GlobalConstants.getJWTKey();
    static final String TOKEN_PREFIX = "Bearer";
    static final String HEADER_STRING = "Authorization";

    private TokenAuthenticationService()
    {
        throw new IllegalStateException("Utility Class");
    }

    static void addAuthentication(HttpServletResponse res, String username)
    {
        String JWT = Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + GlobalConstants.getExpirationTime()))
                .signWith(SignatureAlgorithm.HS512, GlobalConstants.getJWTKey())
                .compact();
        res.addHeader(HEADER_STRING, TOKEN_PREFIX + " " + JWT);
    }

    static Authentication getAuthentication(HttpServletRequest request)
    {
        String token = request.getHeader(HEADER_STRING);
        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("USER"));
        if (token != null)
        {
            // parse the token.
            String user = Jwts.parser()
                    .setSigningKey( GlobalConstants.getJWTKey())
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody()
                    .getSubject();

            return user != null ? new UsernamePasswordAuthenticationToken(user, null,grantedAuths) : null;
        }
        else
        {
            return null;
        }
    }
}