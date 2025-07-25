package com.oracle.il.css;

import java.security.Principal;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.http.HttpServletRequest;

import weblogic.logging.NonCatalogLogger;
import weblogic.management.security.ProviderMBean;
import weblogic.management.security.RealmMBean;
import weblogic.security.auth.callback.IdentityDomainUserCallback;
import weblogic.security.principal.WLSUserImpl;
import weblogic.security.provider.PrincipalValidatorImpl;
import weblogic.security.service.ContextHandler;
import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.IdentityAssertionException;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.spi.SecurityServices;

public final class CustomRealmIdentityAsserterProviderImpl implements AuthenticationProviderV2, IdentityAsserterV2 {
    private String headerName;
    private boolean debugEnabled;
    private String description;
    private PrincipalValidator principalValidator;
    private NonCatalogLogger logger = new NonCatalogLogger("CustomRealmIdentityAsserterProviderImpl");

    public CustomRealmIdentityAsserterProviderImpl() {
        logger.debug("Initializing CustomRealmIdentityAsserterProviderImpl");
    }

    @Override
    public void initialize(ProviderMBean mbean, SecurityServices services) {
        logger.debug("CustomRealmIdentityAsserterProviderImpl.initialize");
        CustomRealmIdentityAsserterMBean myMBean = (CustomRealmIdentityAsserterMBean) mbean;
        
        this.headerName = myMBean.getHeaderName();
        this.debugEnabled = myMBean.getDebugEnabled(); // Use getDebugEnabled()
        this.description = myMBean.getDescription() + "\n" + myMBean.getVersion();
        
        // FIX #3 & #4: For older WLS, just instantiate the default validator
        this.principalValidator = new PrincipalValidatorImpl();
    }

    @Override
    public PrincipalValidator getPrincipalValidator() {
        return this.principalValidator;
    }

    @Override
    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    // FIX #1 & #2: Fix the method signature for older WLS
    @Override
    public AppConfigurationEntry getAssertionModuleConfiguration() {
        return null;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void shutdown() {
        logger.debug("CustomRealmIdentityAsserterProviderImpl.shutdown");
    }

    @Override
    public CallbackHandler assertIdentity(String type, Object token, ContextHandler contextHandler) throws IdentityAssertionException {
        // In older WLS, the token is often the HttpServletRequest itself
        if (!(token instanceof HttpServletRequest)) {
            return null; // Cannot handle this token type
        }

        HttpServletRequest request = (HttpServletRequest) token;
        final String username = request.getHeader(this.headerName);

        if (username == null || username.isEmpty()) {
             // It's better to return null to allow other providers to try
            return null;
        }

        if (debugEnabled) {
            logger.debug("assertIdentity found user '" + username + "' in header '" + this.headerName + "'");
        }
        
        final Principal userPrincipal = new WLSUserImpl(username);
        
        if (validateUserInRealm(userPrincipal)) {
            if (debugEnabled) {
                logger.debug("User '" + username + "' validated successfully.");
            }
            
            return new CallbackHandler() {
                public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
                    for (Callback callback : callbacks) {
                        if (callback instanceof IdentityDomainUserCallback) {
                            IdentityDomainUserCallback iduc = (IdentityDomainUserCallback) callback;
                            // FIX #6: Pass the username String to the callback
                            iduc.setUser(username);
                        } else {
                            throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
                        }
                    }
                }
            };
        } else {
            if (debugEnabled) {
                logger.debug("User '" + username + "' was not found in the security realm.");
            }
            throw new IdentityAssertionException("User '" + username + "' not found in security realm.");
        }
    }

    private boolean validateUserInRealm(Principal principal) {
        try {
            // The default validator's validate() returns boolean
            return this.principalValidator.validate(principal);
        } catch (Exception e) {
            if (debugEnabled) {
                logger.debug("Exception during principal validation for " + principal.getName() + ": " + e.getMessage());
            }
            return false;
        }
    }
}