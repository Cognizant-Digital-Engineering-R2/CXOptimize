package com.cognizant.pace.CXOptimize.Collector.utils;

import com.cognizant.pace.CXOptimize.Collector.constant.CollectorConstants;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class ValidationUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtils.class);

    public static boolean validateAuthToken(String authToken) throws UnsupportedEncodingException, JSONException {
        Base64 base64Url = new Base64(true);
        String[] split_string = authToken.split("\\.");
        String base64EncodedBody = split_string[1];
        String body = new String(base64Url.decode(base64EncodedBody));
        JSONObject tokenDetails = new JSONObject(body);
        if(tokenDetails.getString("sub").equals(CollectorConstants.getUserName()))
        {
            LOGGER.debug("CXOP - Setting AuthToken {}",authToken);
            LOGGER.debug("CXOP - Setting AuthToken expiry {}",tokenDetails.getLong("exp"));
            long tokenStartTime = System.currentTimeMillis();
            //Set expiry duration less than 3 seconds
            long expiryDuration = ((tokenDetails.getLong("exp") * 1000) - tokenStartTime) - 3000;
            LOGGER.debug("CXOP - Setting AuthToken expiry duration {}",expiryDuration);
            CollectorConstants.setTokenStartTime(tokenStartTime);
            CollectorConstants.setApiToken(authToken);
            CollectorConstants.setTokenExpiry(expiryDuration);
            return true;
        }
        return false;
    }
}
