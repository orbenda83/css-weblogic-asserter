package com.oracle.il.css;

import java.security.Principal;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.http.HttpServletRequest;

// Assuming CustomRealmCallbackHandlerImpl and CustomRealmLoginModuleImpl exist in the same package
// import com.oracle.il.css.CustomRealmCallbackHandlerImpl; // Already in same package, no explicit import needed if in same dir
// import com.oracle.il.css.CustomRealmLoginModuleImpl; // Assuming this is also in the same package

import weblogic.logging.NonCatalogLogger;
import weblogic.management.security.ProviderMBean;
import weblogic.security.principal.WLSUserImpl;
import weblogic.security.provider.PrincipalValidatorImpl;
import weblogic.security.service.ContextHandler;
import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.IdentityAssertionException;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.spi.SecurityServices;

import java.util.HashMap;

public final class CustomRealmIdentityAsserterProviderImpl implements AuthenticationProviderV2, IdentityAsserterV2 {
    private String headerName;
    private boolean debugEnabled;
    private String description;
    private PrincipalValidator principalValidator;
    private NonCatalogLogger logger = new NonCatalogLogger("CustomRealmIdentityAsserterProviderImpl"); // Logger instance

    public CustomRealmIdentityAsserterProviderImpl() {
        // Constructor, no specific debug here as initialize is called after MBean is available
    }

    @Override
    public void initialize(ProviderMBean mbean, SecurityServices services) {
        CustomRealmIdentityAsserterMBean myMBean = (CustomRealmIdentityAsserterMBean) mbean;

        this.headerName = myMBean.getHeaderName();
        this.debugEnabled = myMBean.getDebugEnabled();
        this.description = myMBean.getDescription() + "\n" + myMBean.getVersion();
        this.principalValidator = new PrincipalValidatorImpl();

        if (this.debugEnabled) {
            logger.debug("CustomRealmIdentityAsserterProviderImpl initialized.");
            logger.debug("Configured Header Name: " + this.headerName);
            logger.debug("Debug Enabled: " + this.debugEnabled);
            logger.debug("Description: " + this.description);
        }
    }

    @Override
    public PrincipalValidator getPrincipalValidator() {
        return this.principalValidator;
    }

    @Override
    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    @Override
    public AppConfigurationEntry getAssertionModuleConfiguration() {
        if (debugEnabled) {
            logger.debug("getAssertionModuleConfiguration called. Returning null as per implementation.");
        }
        return null; // This method is often not used for simple header assertion
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void shutdown() {
        if (debugEnabled) {
            logger.debug("CustomRealmIdentityAsserterProviderImpl shutdown.");
        }
    }

    @Override
    public CallbackHandler assertIdentity(String type, Object token, ContextHandler contextHandler) throws IdentityAssertionException {
        if (debugEnabled) {
            logger.debug("assertIdentity method invoked. Type: " + type + ", Token Class: " + (token != null ? token.getClass().getName() : "null"));
        }

        if (!(token instanceof HttpServletRequest)) {
            if (debugEnabled) {
                logger.debug("Token is not an HttpServletRequest. Returning null.");
            }
            return null; // Token must be an HttpServletRequest for this asserter
        }

        HttpServletRequest request = (HttpServletRequest) token;
        final String username = request.getHeader(this.headerName);

        if (debugEnabled) {
            logger.debug("Checking for header '" + this.headerName + "'. Value found: '" + (username != null ? username : "null (or empty string)"));
        }

        if (username == null || username.isEmpty()) {
            if (debugEnabled) {
                logger.debug("Header '" + this.headerName + "' is null or empty. Returning null.");
            }
            return null; // No user identified from header
        }

        final Principal userPrincipal = new WLSUserImpl(username);
        if (debugEnabled) {
            logger.debug("Attempting to validate principal: " + userPrincipal.getName());
        }

        if (validateUserInRealm(userPrincipal)) {
            if (debugEnabled) {
                logger.debug("User '" + username + "' validated successfully by PrincipalValidator. Returning CustomRealmCallbackHandlerImpl.");
            }
            return new CustomRealmCallbackHandlerImpl(username);
        } else {
            if (debugEnabled) {
                logger.debug("User '" + username + "' validation failed by PrincipalValidator. Throwing IdentityAssertionException.");
            }
            throw new IdentityAssertionException("User '" + username + "' not found in security realm.");
        }
    }

    private boolean validateUserInRealm(Principal principal) {
        if (debugEnabled) {
            logger.debug("Invoking PrincipalValidator.validate() for principal: " + principal.getName());
        }
        try {
            boolean isValid = this.principalValidator.validate(principal);
            if (debugEnabled) {
                logger.debug("PrincipalValidator.validate() returned: " + isValid + " for " + principal.getName());
            }
            return isValid;
        } catch (Exception e) {
            if (debugEnabled) {
                logger.debug("Exception during PrincipalValidator.validate() for " + principal.getName() + ": " + e.getMessage(), e);
            }
            return false;
        }
    }

    @Deprecated // This method seems to be for AuthenticationProvider, not typically part of IdentityAsserterV2 flow for assertion
    public AppConfigurationEntry getLoginModuleConfiguration() {
        // This method signature is usually for AuthenticationProviderV2's own LoginModule setup,
        // not directly for the assertion process of IdentityAsserterV2.
        // The original code uses System.out.println, let's keep it but add a debug flag check.
        if (debugEnabled) {
             logger.debug("CustomRealmIdentityAsserterProviderImpl: getLoginModuleConfiguration called (non-assertion context).");
        }
        // System.out.println("CustomRealmIdentityAsserterProviderImpl: getConfiguration of non Assertion!!! "); // Removed direct System.out.println

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("IdentityAssertion", "false"); // Setting a property for the LoginModule
        return new AppConfigurationEntry(
            "com.oracle.il.css.CustomRealmLoginModuleImpl",
            AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
            paramHashMap
        );
    }

    // // Renamed for clarity, was 'getConfiguration' in original, but handles LoginModule config
    // private AppConfigurationEntry getLoginModuleAppConfiguration(HashMap<String, ?> paramHashMap) {
    //     if (debugEnabled) {
    //         logger.debug("CustomRealmIdentityAsserterProviderImpl: Creating AppConfigurationEntry for LoginModule. Parameters: " + paramHashMap);
    //     }
    //     // System.out.println("CustomRealmIdentityAsserterProviderImpl: getConfiguration"); // Removed direct System.out.println

    //     return new AppConfigurationEntry(
    //         "com.oracle.il.css.CustomRealmLoginModuleImpl",
    //         AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
    //         paramHashMap
    //     );
    // }
}