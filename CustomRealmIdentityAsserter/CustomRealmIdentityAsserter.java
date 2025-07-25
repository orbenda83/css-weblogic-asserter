package com.oracle.il.css;

import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.IdentityAssertionException;
import weblogic.security.spi.SecurityServices;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.service.ContextHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.callback.CallbackHandler;
import java.util.HashMap;
import java.util.logging.Logger;
import weblogic.security.services.Authentication;

public class CustomRealmIdentityAsserter implements AuthenticationProviderV2, IdentityAsserterV2, CustomRealmIdentityAsserterMBean {
    private String headerName = "X-User-Id";
    private boolean debugEnabled = false;
    private static final Logger logger = Logger.getLogger(CustomRealmIdentityAsserter.class.getName());
    private String description = "Custom Identity Asserter for Realm Validation";
    private PrincipalValidator principalValidator;

    public void initialize(SecurityServices services) {
        if (debugEnabled) {
            System.out.println("Initializing CustomRealmIdentityAsserter with headerName: " + headerName);
        }
    }

    public void initialize(weblogic.management.security.ProviderMBean mbean, weblogic.security.spi.SecurityServices services) {
        initialize(services);
    }

    public String getName() {
        return "CustomRealmIdentityAsserter";
    }

    public boolean getDebugEnabled() {
        return debugEnabled;
    }

    public String getDescription() {
        return description;
    }

    public AppConfigurationEntry getLoginModuleConfiguration() {
        return null; // No login module required for identity assertion
    }

    public AppConfigurationEntry getAssertionModuleConfiguration() {
        HashMap<String, Object> options = new HashMap<>();
        options.put("HeaderName", headerName);
        return new AppConfigurationEntry(
                "weblogic.security.providers.authentication.DefaultIdentityAsserterLoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                options
        );
    }

    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    public PrincipalValidator getPrincipalValidator() {
        return principalValidator;
    }

    public void shutdown() {
        if (debugEnabled) {
            System.out.println("Shutting down CustomRealmIdentityAsserter");
        }
    }

    public CallbackHandler assertIdentity(String type, Object token, ContextHandler contextHandler) throws IdentityAssertionException {
        if (debugEnabled) {
            System.out.println("assertIdentity called with type: " + type);
        }
        if (!headerName.equals(type)) {
            if (debugEnabled) {
                System.out.println("Token type " + type + " does not match configured header: " + headerName);
            }
            throw new IdentityAssertionException("Unsupported token type: " + type);
        }
        String username = null;
        if (contextHandler != null) {
            Object request = contextHandler.getValue("com.bea.contextelement.servlet.HttpServletRequest");
            if (request != null) {
                try {
                    javax.servlet.http.HttpServletRequest httpRequest = (javax.servlet.http.HttpServletRequest) request;
                    username = httpRequest.getHeader(headerName);
                } catch (ClassCastException e) {
                    if (debugEnabled) {
                        System.out.println("Request is not an HttpServletRequest: " + e.getMessage());
                    }
                }
            }
        }
        if (debugEnabled) {
            System.out.println("Header " + headerName + " value: " + username);
        }
        if (username == null || username.isEmpty()) {
            if (debugEnabled) {
                System.out.println("No username provided in header: " + headerName);
            }
            throw new IdentityAssertionException("No username provided in header");
        }

        // Let WebLogic realm handle user validation
        final String user = username;
        return callbacks -> {
            javax.security.auth.callback.NameCallback nameCallback = null;
            for (javax.security.auth.callback.Callback callback : callbacks) {
                if (callback instanceof javax.security.auth.callback.NameCallback) {
                    nameCallback = (javax.security.auth.callback.NameCallback) callback;
                    nameCallback.setName(user);
                }
            }
        };
    }

    // MBean attribute getters and setters
    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
        if (debugEnabled) {
            System.out.println("Header name set to: " + headerName);
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
        if (debugEnabled) {
            System.out.println("Debug flag set to: " + debugEnabled);
        }
    }
}