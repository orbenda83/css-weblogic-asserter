package com.oracle.il.css;

import java.util.HashMap;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;

import weblogic.management.security.ProviderMBean;

import weblogic.security.provider.PrincipalValidatorImpl;
import weblogic.security.service.ContextHandler;
import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.IdentityAssertionException;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.spi.SecurityServices;

import weblogic.logging.NonCatalogLogger;

import javax.servlet.http.HttpServletRequest;


public final class CustomRealmIdentityAsserterProviderImpl implements AuthenticationProviderV2, IdentityAsserterV2 {
    private String headerName = "X-User-Id";
    private boolean debugEnabled = false;
    private String description = "Custom Identity Asserter for Realm Validation";
    private AppConfigurationEntry.LoginModuleControlFlag controlFlag;
    private CustomRealmIdentityAsserterMBean _mBean = null;

    public CustomRealmIdentityAsserterProviderImpl() {
        this.logger = new NonCatalogLogger("CustomRealmIdentityAsserterProviderImpl");
        if (debugEnabled) {
            logger.debug("Initializing CustomRealmIdentityAsserterProviderImpl with headerName: " + headerName);
        }
    }

    public void initialize(ProviderMBean mbean, SecurityServices services) {
        System.out.println("CustomRealmIdentityAsserterProviderImpl.initialize");
        CustomRealmIdentityAsserterMBean myMBean = (CustomRealmIdentityAsserterMBean) mbean;
        this._mBean = myMBean;
        description = myMBean.getDescription() + "\n" + myMBean.getVersion();
        this.controlFlag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
    }

    public String getDescription() {
        return description;
    }

    public void shutdown() {
        System.out.println("CustomRealmIdentityAsserterProviderImpl.shutdown");
    }

    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
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
