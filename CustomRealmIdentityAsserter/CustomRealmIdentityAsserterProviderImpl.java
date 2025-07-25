package com.oracle.il.css;

import java.security.Principal;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.http.HttpServletRequest;

import weblogic.logging.NonCatalogLogger;
import weblogic.management.security.ProviderMBean;
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

    public CustomRealmIdentityAsserterProviderImpl() {}

    @Override
    public void initialize(ProviderMBean mbean, SecurityServices services) {
        CustomRealmIdentityAsserterMBean myMBean = (CustomRealmIdentityAsserterMBean) mbean;
        
        this.headerName = myMBean.getHeaderName();
        this.debugEnabled = myMBean.getDebugEnabled();
        this.description = myMBean.getDescription() + "\n" + myMBean.getVersion();
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

    // FIX #1: Correct method name for this WLS version
    @Override
    public AppConfigurationEntry getLoginModuleConfiguration() {
        return null;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void shutdown() {}

    @Override
    public CallbackHandler assertIdentity(String type, Object token, ContextHandler contextHandler) throws IdentityAssertionException {
        if (!(token instanceof HttpServletRequest)) {
            return null;
        }

        HttpServletRequest request = (HttpServletRequest) token;
        final String username = request.getHeader(this.headerName);

        if (username == null || username.isEmpty()) {
            return null;
        }

        if (debugEnabled) {
            logger.debug("assertIdentity found user '" + username + "'");
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
                            // FIX #2: Pass the Principal object
                            iduc.setUser(userPrincipal);
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
            return this.principalValidator.validate(principal);
        } catch (Exception e) {
            if (debugEnabled) {
                logger.debug("Exception during principal validation: " + e.getMessage());
            }
            return false;
        }
    }
}