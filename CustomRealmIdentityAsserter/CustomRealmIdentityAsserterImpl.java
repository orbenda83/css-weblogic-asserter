package com.oracle.il.css;

import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.IdentityAssertionException;
import weblogic.security.spi.SecurityServices;
import weblogic.security.service.SecurityServiceManager;
import weblogic.security.service.ContextHandler;
import weblogic.security.principal.WLSUserImpl;
import weblogic.logging.NonCatalogLogger;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import java.util.HashMap;

public class CustomRealmIdentityAsserterImpl implements AuthenticationProviderV2, IdentityAsserterV2, CustomRealmIdentityAsserterMBean {
    private String headerName = "X-User-Id";
    private boolean debugEnabled = false;
    private NonCatalogLogger logger;
    private String description = "Custom Identity Asserter for Realm Validation";

    public CustomRealmIdentityAsserterImpl() {}

    @Override
    public void initialize(SecurityServices services) {
        this.logger = new NonCatalogLogger("CustomRealmIdentityAsserterImpl");
        if (debugEnabled) {
            logger.debug("Initializing CustomRealmIdentityAsserterImpl with headerName: " + headerName);
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public AppConfigurationEntry getLoginModuleConfiguration() {
        return null;
    }

    @Override
    public AppConfigurationEntry getAssertionModuleConfiguration() {
        HashMap<String, Object> options = new HashMap<>();
        options.put("HeaderName", headerName);
        return new AppConfigurationEntry(
                "weblogic.security.providers.authentication.DefaultIdentityAsserterLoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                options
        );
    }

    @Override
    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    @Override
    public void shutdown() {
        if (debugEnabled) {
            logger.debug("Shutting down CustomRealmIdentityAsserterImpl");
        }
    }

    @Override
    public Subject assertIdentity(String type, Object token, ContextHandler contextHandler) throws IdentityAssertionException {
        if (debugEnabled) {
            logger.debug("assertIdentity called with type: " + type);
        }

        if (!headerName.equals(type)) {
            if (debugEnabled) {
                logger.debug("Token type " + type + " does not match configured header: " + headerName);
            }
            throw new IdentityAssertionException("Unsupported token type: " + type);
        }

        String username = extractTokenFromHeader(token, contextHandler);

        if (username == null || username.isEmpty()) {
            if (debugEnabled) {
                logger.debug("No username provided in header: " + headerName);
            }
            throw new IdentityAssertionException("No username provided in header");
        }

        if (debugEnabled) {
            logger.debug("Extracted username: " + username);
        }

        boolean isValid = validateUserInRealm(username);

        if (isValid) {
            if (debugEnabled) {
                logger.debug("User " + username + " validated successfully");
            }
            Subject subject = new Subject();
            subject.getPrincipals().add(new WLSUserImpl(username));
            return subject;
        } else {
            if (debugEnabled) {
                logger.debug("User " + username + " not found in security realm");
            }
            throw new IdentityAssertionException("User not found in security realm");
        }
    }

    private String extractTokenFromHeader(Object token, ContextHandler contextHandler) {
        String username = null;
        if (contextHandler != null && contextHandler.isKeyAvailable("com.bea.contextelement.servlet.HttpServletRequest")) {
            Object request = contextHandler.getValue("com.bea.contextelement.servlet.HttpServletRequest");
            if (request instanceof javax.servlet.http.HttpServletRequest) {
                username = ((javax.servlet.http.HttpServletRequest) request).getHeader(headerName);
            }
        }
        if (debugEnabled) {
            logger.debug("Header " + headerName + " value: " + username);
        }
        return username;
    }

    private boolean validateUserInRealm(String username) {
        try {
            SecurityServiceManager securityServiceManager = SecurityServiceManager.getInstance();
            boolean isValid = securityServiceManager.isUserValid("myrealm", username);
            if (debugEnabled) {
                logger.debug("Realm validation for user " + username + ": " + isValid);
            }
            return isValid;
        } catch (Exception e) {
            if (debugEnabled) {
                logger.debug("Error validating user " + username + ": " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public String getHeaderName() {
        return headerName;
    }

    @Override
    public void setHeaderName(String headerName) {
        this.headerName = headerName;
        if (debugEnabled) {
            logger.debug("Header name set to: " + headerName);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
        if (debugEnabled) {
            logger.debug("Debug flag set to: " + debugEnabled);
        }
    }
}