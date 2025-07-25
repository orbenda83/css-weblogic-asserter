package com.oracle.il.css;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.http.HttpServletRequest;

import weblogic.logging.NonCatalogLogger;
import weblogic.management.security.ProviderMBean;
import weblogic.security.auth.callback.IdentityDomainUserCallback;
import weblogic.security.auth.callback.ImpersonateCallbackHandler;
import weblogic.security.principal.WLSUserImpl;
import weblogic.security.provider.PrincipalValidatorImpl;
import weblogic.security.service.ContextHandler;
import weblogic.security.service.SecurityServiceManager;
import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.IdentityAssertionException;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.spi.SecurityServices;

public final class CustomRealmIdentityAsserterProviderImpl implements AuthenticationProviderV2, IdentityAsserterV2 {
    private String headerName = "X-User-Id";
    private boolean debugEnabled = false;
    private String description = "Custom Identity Asserter for Realm Validation";
    private AppConfigurationEntry.LoginModuleControlFlag controlFlag;
    private CustomRealmIdentityAsserterMBean _mBean;
    private NonCatalogLogger logger = new NonCatalogLogger("CustomRealmIdentityAsserterProviderImpl");
    private PrincipalValidator principalValidator; // To store the validator

    public CustomRealmIdentityAsserterProviderImpl() {
        logger.debug("Initializing CustomRealmIdentityAsserterProviderImpl");
    }

    @Override
    public void initialize(ProviderMBean mbean, SecurityServices services) {
        logger.debug("CustomRealmIdentityAsserterProviderImpl.initialize");
        this._mBean = (CustomRealmIdentityAsserterMBean) mbean;
        
        // Use the MBean to get configuration
        this.headerName = this._mBean.getHeaderName();
        this.debugEnabled = this._mBean.isDebugEnabled();
        this.description = this._mBean.getDescription() + "\n" + this._mBean.getVersion();
        
        this.controlFlag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
        // Get the principal validator from the security services
        this.principalValidator = services.getPrincipalValidator();
    }

    // --- FIX #1: Implement the required getPrincipalValidator method ---
    @Override
    public PrincipalValidator getPrincipalValidator() {
        // A simple PrincipalValidator is usually sufficient.
        return new PrincipalValidatorImpl();
    }

    @Override
    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void shutdown() {
        logger.debug("CustomRealmIdentityAsserterProviderImpl.shutdown");
    }

    @Override
    public CallbackHandler assertIdentity(String type, Object token, ContextHandler contextHandler) throws IdentityAssertionException {
        if (debugEnabled) {
            logger.debug("assertIdentity called with type: " + type);
        }
        
        // The "type" for an Identity Asserter is the token name from the provider configuration
        // In this case, it should match the active token type in the WebLogic console.
        // We will assume it's configured to be the header name for simplicity.
        if (!type.equals(this.headerName)) {
            if (debugEnabled) {
                logger.debug("Token type " + type + " is not the configured active token type for this provider.");
            }
            // Return null to indicate we are not handling this token type
            return null;
        }

        String username = extractTokenFromHeader(token, contextHandler);

        if (username == null || username.isEmpty()) {
            // No user in the header, so we don't assert an identity.
            // Throwing an exception might be too aggressive if other asserters should try.
            // Returning null is often better. For this example, we'll stick to the exception.
            throw new IdentityAssertionException("No username provided in header: " + headerName);
        }

        if (debugEnabled) {
            logger.debug("Extracted username: " + username);
        }
        
        // --- FIX #4 & #5: Use the PrincipalValidator ---
        boolean isValid = validateUserInRealm(username);

        if (isValid) {
            if (debugEnabled) {
                logger.debug("User " + username + " validated successfully. Returning callback handler.");
            }
            // --- FIX #2: Return a CallbackHandler, not a Subject ---
            // This handler tells WebLogic to impersonate the specified user.
            return new ImpersonateCallbackHandler(new IdentityDomainUserCallback(null, new WLSUserImpl(username)));
        } else {
            if (debugEnabled) {
                logger.debug("User " + username + " not found in security realm or is invalid.");
            }
            throw new IdentityAssertionException("User '" + username + "' not found in security realm or is invalid.");
        }
    }

    private String extractTokenFromHeader(Object token, ContextHandler contextHandler) {
        String username = null;
        // --- FIX #3: Check for the key correctly ---
        if (contextHandler != null) {
            Object requestObj = contextHandler.getValue("com.bea.contextelement.servlet.HttpServletRequest");
            if (requestObj instanceof HttpServletRequest) {
                username = ((HttpServletRequest) requestObj).getHeader(headerName);
            }
        }
        if (debugEnabled) {
            logger.debug("Header " + headerName + " value: " + username);
        }
        return username;
    }

    private boolean validateUserInRealm(String username) {
        try {
            if (debugEnabled) {
                logger.debug("Validating user '" + username + "' using PrincipalValidator.");
            }
            Subject userSubject = new Subject();
            userSubject.getPrincipals().add(new WLSUserImpl(username));

            // Use the PrincipalValidator to check if the user is valid in the realm
            this.principalValidator.validate(userSubject, null);
            
            // If validate() does not throw an exception, the user is considered valid.
            return true;
        } catch (Exception e) {
            if (debugEnabled) {
                logger.debug("Error validating user '" + username + "': " + e.getMessage());
            }
            return false;
        }
    }

    // --- FIX #6: Remove @Override from custom methods not in the interfaces ---
    
    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }
}