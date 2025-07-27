package com.oracle.il.css;

import java.security.Principal;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.http.HttpServletRequest;

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
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag; // Import this

import java.util.HashMap;

public final class CustomRealmIdentityAsserterProviderImpl implements AuthenticationProviderV2, IdentityAsserterV2 {
    private String headerName;
    private boolean debugEnabled;
    private String description;
    private PrincipalValidator principalValidator;
    private NonCatalogLogger logger = new NonCatalogLogger("CustomRealmIdentityAsserterProviderImpl");
    private boolean loginControlFlag; // NEW FIELD for MBean property

    public CustomRealmIdentityAsserterProviderImpl() {
        printMessage("constructor initialized"); // Corrected typo here
    }

    @Override
    public void initialize(ProviderMBean mbean, SecurityServices services) {
        CustomRealmIdentityAsserterMBean myMBean = (CustomRealmIdentityAsserterMBean) mbean;

        this.headerName = myMBean.getHeaderName();
        this.debugEnabled = myMBean.getDebugEnabled();
        this.description = myMBean.getDescription() + "\n" + myMBean.getVersion();
        this.principalValidator = new PrincipalValidatorImpl();
        this.loginControlFlag = myMBean.getControlFlag(); // NEW: Read the MBean property

        if (this.debugEnabled) {
            printMessage("initialized");
            printMessage("Configured Header Name: " + this.headerName);
            printMessage("Debug Enabled: " + this.debugEnabled);
            printMessage("Description: " + this.description);
            printMessage("Login Module Sufficient Flag: " + this.loginControlFlag); // NEW LOG
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
            printMessage("getAssertionModuleConfiguration called.");
        }
        // This method should return the AppConfigurationEntry for the LoginModule
        // that performs the actual assertion.
        HashMap<String, Object> paramHashMap = new HashMap<String, Object>();
        paramHashMap.put("IdentityAssertion", "true"); // This should be "true" for assertion modules

        // Use the configured loginModuleSufficient flag
        LoginModuleControlFlag controlFlag = this.loginControlFlag ?
            LoginModuleControlFlag.SUFFICIENT :
            LoginModuleControlFlag.REQUIRED;

        return new AppConfigurationEntry(
            "com.oracle.il.css.CustomRealmLoginModuleImpl",
            controlFlag,
            paramHashMap
        );
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void shutdown() {
        if (debugEnabled) {
            printMessage("shutdown");
        }
    }

    @Override
    public CallbackHandler assertIdentity(String type, Object token, ContextHandler contextHandler) throws IdentityAssertionException {
        if (debugEnabled) {
            printMessage("assertIdentity method invoked. Type: " + type + ", Token Class: " + (token != null ? token.getClass().getName() : "null"));
        }

        // IMPORTANT: The 'type' parameter here will be whatever you configured in the WebLogic Console
        // under your asserter's "Active Types" (e.g., "OAS_USER").
        // If the 'type' doesn't match what you expect, you might filter it out here.
        // For a simple header, you might or might not need an explicit type check here,
        // but it's good practice if your asserter handles multiple types.
        // Example check (uncomment if you add "OAS_USER_HEADER" as Active Type):
        /*
        if (!"OAS_USER".equals(type)) { // Match this to your exact "Active Type" in console
            if (debugEnabled) {
                printMessage("Unsupported token type: '" + type + "'. This asserter expects 'OAS_USER'. Returning null.");
            }
            return null;
        }
        */

        // Checking we got the right kind of token
        if (debugEnabled) {
            printMessage("Before checking the header name");
        }
        
        if (!this.headerName.equals(type)) {
            if (debugEnabled) {
                printMessage("Unsupported token type: '" + type + "'. This asserter expects '" + this.headerName + "'. Returning null.");
            }
            return null;
        }

        if (debugEnabled) {
            printMessage("After checking the header name");
        }

        if (!(token instanceof HttpServletRequest)) {
            if (debugEnabled) {
                printMessage("Token is not an HttpServletRequest. Returning null.");
            }
            return null; // Token must be an HttpServletRequest for this asserter
        }

        HttpServletRequest request = (HttpServletRequest) token;
        final String username = request.getHeader(this.headerName);

        if (debugEnabled) {
            printMessage("Checking for header '" + this.headerName + "'. Value found: '" + (username != null ? username : "null (or empty string)"));
        }

        if (username == null || username.isEmpty()) {
            if (debugEnabled) {
                printMessage("Header '" + this.headerName + "' is null or empty. Returning null.");
            }
            return null; // No user identified from header
        }

        final Principal userPrincipal = new WLSUserImpl(username);
        if (debugEnabled) {
            printMessage("Attempting to validate principal: " + userPrincipal.getName());
        }

        if (validateUserInRealm(userPrincipal)) {
            if (debugEnabled) {
                printMessage("User '" + username + "' validated successfully by PrincipalValidator. Returning CustomRealmCallbackHandlerImpl.");
            }
            return new CustomRealmCallbackHandlerImpl(username);
        } else {
            if (debugEnabled) {
                printMessage("User '" + username + "' validation failed by PrincipalValidator. Throwing IdentityAssertionException.");
            }
            throw new IdentityAssertionException("User '" + username + "' not found in security realm.");
        }
    }

    private boolean validateUserInRealm(Principal principal) {
        if (debugEnabled) {
            printMessage("Invoking PrincipalValidator.validate() for principal: " + principal.getName());
        }
        try {
            boolean isValid = this.principalValidator.validate(principal);
            if (debugEnabled) {
                printMessage("PrincipalValidator.validate() returned: " + isValid + " for " + principal.getName());
            }
            return isValid;
        } catch (Exception e) {
            if (debugEnabled) {
                printMessage("Exception during PrincipalValidator.validate() for " + principal.getName() + ": " + e.getMessage());
            }
            return false;
        }
    }

    @Deprecated
    public AppConfigurationEntry getLoginModuleConfiguration() {
        // This method is part of AuthenticationProviderV2, but for an Identity Asserter,
        // getAssertionModuleConfiguration() is typically used for the assertion flow.
        // This method might be called for traditional authentication flows.
        if (debugEnabled) {
             printMessage("getLoginModuleConfiguration called (non-assertion context).");
        }
        HashMap<String, Object> paramHashMap = new HashMap<String, Object>();
        paramHashMap.put("IdentityAssertion", "false"); // Indicate it's not for assertion flow here

        // Use the configured loginModuleSufficient flag
        LoginModuleControlFlag controlFlag = this.loginControlFlag ?
            LoginModuleControlFlag.SUFFICIENT :
            LoginModuleControlFlag.REQUIRED;

        return new AppConfigurationEntry(
            "com.oracle.il.css.CustomRealmLoginModuleImpl",
            controlFlag,
            paramHashMap
        );
    }

    // Corrected method name from printMesaage to printMessage
    private void printMessage(String message) {
        // Using logger.debug for actual logging, instead of System.out.println directly
        // This ensures the messages go to the configured WebLogic log files
        // and respect the server's logging level.
        // logger.debug(message);
        // If you still want to see it in standard out, you can keep:
        System.out.println("CustomRealmIdentityAsserterProviderImpl: " + message + ".");
    }
}