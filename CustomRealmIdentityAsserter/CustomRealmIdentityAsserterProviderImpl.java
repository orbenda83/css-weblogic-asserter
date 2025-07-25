package com.oracle.il.css;

import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.http.HttpServletRequest;

import weblogic.logging.NonCatalogLogger;
import weblogic.management.security.ProviderMBean;
import weblogic.security.auth.callback.IdentityDomainUserCallback;
import weblogic.security.principal.WLSUserImpl;
import weblogic.security.service.ContextHandler;
import weblogic.security.service.SecurityServiceManager;
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
        
        // Get configuration from the MBean
        this.headerName = myMBean.getHeaderName();
        this.debugEnabled = myMBean.isDebugEnabled(); // This now works after fixing the XML
        this.description = myMBean.getDescription() + "\n" + myMBean.getVersion();
        
        // FIX #3: Get the PrincipalValidator from the SecurityServiceManager for the realm
        this.principalValidator = SecurityServiceManager.getPrincipalValidator(myMBean.getRealm());
    }

    @Override
    public PrincipalValidator getPrincipalValidator() {
        // This is still required, but we will use the one from SecurityServiceManager
        return this.principalValidator;
    }

    @Override
    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    // FIX #1: Implement the required getAssertionModuleConfiguration method
    @Override
    public AppConfigurationEntry[] getAssertionModuleConfiguration() {
        // Return null if no special JAAS module configuration is needed
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
        // The token "type" should be the name of the header
        if (!type.equalsIgnoreCase(this.headerName)) {
            if (debugEnabled) {
                logger.debug("Token type " + type + " does not match configured header " + this.headerName);
            }
            return null; // Not our token type, let other asserters try
        }
        
        String username = extractTokenFromHeader(token, contextHandler);
        if (username == null || username.isEmpty()) {
            throw new IdentityAssertionException("No username provided in header: " + this.headerName);
        }

        if (debugEnabled) {
            logger.debug("Extracted username: " + username);
        }
        
        // Create the principal object for validation
        final Principal userPrincipal = new WLSUserImpl(username);
        
        if (validateUserInRealm(userPrincipal)) {
            if (debugEnabled) {
                logger.debug("User " + username + " validated successfully. Returning callback handler.");
            }
            
            return new CallbackHandler() {
                public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
                    for (Callback callback : callbacks) {
                        if (callback instanceof IdentityDomainUserCallback) {
                            IdentityDomainUserCallback iduc = (IdentityDomainUserCallback) callback;
                            // FIX #4: Pass the Principal to the callback
                            iduc.setUser(userPrincipal);
                        } else {
                            throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
                        }
                    }
                }
            };
        } else {
            if (debugEnabled) {
                logger.debug("User " + username + " not found in security realm or is invalid.");
            }
            throw new IdentityAssertionException("User '" + username + "' not found in security realm or is invalid.");
        }
    }

    private String extractTokenFromHeader(Object token, ContextHandler contextHandler) {
        if (token instanceof HttpServletRequest) {
            return ((HttpServletRequest) token).getHeader(this.headerName);
        }
        return null;
    }

    private boolean validateUserInRealm(Principal principal) {
        try {
            if (debugEnabled) {
                logger.debug("Validating user '" + principal.getName() + "' using PrincipalValidator.");
            }
            // FIX #5: Use the correct 'validate' method signature
            if (this.principalValidator.validate(principal)) {
                return true; // The user is valid
            }
            return false;
        } catch (Exception e) {
            if (debugEnabled) {
                logger.debug("Error validating user '" + principal.getName() + "': " + e.getMessage());
            }
            return false;
        }
    }
}