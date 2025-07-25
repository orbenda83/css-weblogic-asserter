package com.oracle.il.css;

import java.security.Principal;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.servlet.http.HttpServletRequest;

import com.oracle.il.css.CustomRealmCallbackHandlerImpl;

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
    
    // FIX #1: Correct method signature for this WLS version
    @Override
    public AppConfigurationEntry getAssertionModuleConfiguration() {
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
        
        final Principal userPrincipal = new WLSUserImpl(username);
        
        if (validateUserInRealm(userPrincipal)) {
            if (debugEnabled) {
                logger.debug("User '" + username + "' validated. Returning UserCallback.");
            }
            
            return new CustomRealmCallbackHandlerImpl(username);
        } else {
            throw new IdentityAssertionException("User '" + username + "' not found in security realm.");
        }
    }

    private boolean validateUserInRealm(Principal principal) {
        try {
            return this.principalValidator.validate(principal);
        } catch (Exception e) {
            return false;
        }
    }

    public AppConfigurationEntry getLoginModuleConfiguration() {
        System.out.println("CustomRealmIdentityAsserterProviderImpl: getConfiguration of non Assertion!!! ");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("IdentityAssertion", "false");
        return getConfiguration(hashMap);
    }

    private AppConfigurationEntry getConfiguration(HashMap<String, ?> paramHashMap) {
        System.out.println("CustomRealmIdentityAsserterProviderImpl: getConfiguration");

        return new AppConfigurationEntry("com.oracle.il.css.CustomRealmLoginModuleImpl", AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT,
                                         paramHashMap);
    }
}