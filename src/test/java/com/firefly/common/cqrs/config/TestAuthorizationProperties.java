/*
 * Copyright 2025 Firefly Software Solutions Inc
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

package com.firefly.common.cqrs.config;

/**
 * Test utility class for creating AuthorizationProperties instances for testing.
 * 
 * @since 1.0.0
 */
public class TestAuthorizationProperties {

    /**
     * Creates default authorization properties for testing.
     * 
     * @return AuthorizationProperties with default test configuration
     */
    public static AuthorizationProperties createDefault() {
        AuthorizationProperties properties = new AuthorizationProperties();
        properties.setEnabled(true);
        
        // Configure lib-common-auth
        AuthorizationProperties.LibCommonAuth libCommonAuth = new AuthorizationProperties.LibCommonAuth();
        libCommonAuth.setEnabled(true);
        libCommonAuth.setFailFast(false);
        libCommonAuth.setRequireBoth(false);
        properties.setLibCommonAuth(libCommonAuth);
        
        // Configure custom authorization
        AuthorizationProperties.Custom custom = new AuthorizationProperties.Custom();
        custom.setEnabled(true);
        custom.setAllowOverride(true);
        custom.setTimeoutMs(5000);
        properties.setCustom(custom);
        
        // Configure logging
        AuthorizationProperties.Logging logging = new AuthorizationProperties.Logging();
        logging.setEnabled(true);
        logging.setLogSuccessful(false);
        logging.setLogPerformance(true);
        logging.setLevel("INFO");
        properties.setLogging(logging);
        
        // Configure performance
        AuthorizationProperties.Performance performance = new AuthorizationProperties.Performance();
        performance.setCacheEnabled(false);
        performance.setCacheTtlSeconds(300);
        performance.setCacheMaxSize(1000);
        performance.setAsyncEnabled(false);
        properties.setPerformance(performance);
        
        return properties;
    }

    /**
     * Creates authorization properties with authorization disabled.
     * 
     * @return AuthorizationProperties with authorization disabled
     */
    public static AuthorizationProperties createDisabled() {
        AuthorizationProperties properties = createDefault();
        properties.setEnabled(false);
        return properties;
    }

    /**
     * Creates authorization properties with only lib-common-auth enabled.
     * 
     * @return AuthorizationProperties with only lib-common-auth enabled
     */
    public static AuthorizationProperties createLibCommonAuthOnly() {
        AuthorizationProperties properties = createDefault();
        properties.getCustom().setEnabled(false);
        return properties;
    }

    /**
     * Creates authorization properties with only custom authorization enabled.
     * 
     * @return AuthorizationProperties with only custom authorization enabled
     */
    public static AuthorizationProperties createCustomOnly() {
        AuthorizationProperties properties = createDefault();
        properties.getLibCommonAuth().setEnabled(false);
        return properties;
    }

    /**
     * Creates authorization properties with strict security settings.
     * 
     * @return AuthorizationProperties with strict security configuration
     */
    public static AuthorizationProperties createStrict() {
        AuthorizationProperties properties = createDefault();
        properties.getLibCommonAuth().setFailFast(true);
        properties.getLibCommonAuth().setRequireBoth(true);
        properties.getCustom().setAllowOverride(false);
        properties.getCustom().setTimeoutMs(2000);
        return properties;
    }

    /**
     * Creates authorization properties with verbose logging enabled.
     * 
     * @return AuthorizationProperties with verbose logging
     */
    public static AuthorizationProperties createVerbose() {
        AuthorizationProperties properties = createDefault();
        properties.getLogging().setLogSuccessful(true);
        properties.getLogging().setLevel("DEBUG");
        return properties;
    }
}
