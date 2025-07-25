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

import javax.servlet.http.HttpServletRequest;


public final class CustomRealmIdentityAsserterImpl implements AuthenticationProviderV2, IdentityAsserterV2 {
    private String headerName = "X-User-Id";
    private boolean debugEnabled = false;
    private String description = "Custom Identity Asserter for Realm Validation";
    private AppConfigurationEntry.LoginModuleControlFlag controlFlag;
    private CustomRealmIdentityAsserterMBean _mBean = null;

    public void initialize(ProviderMBean mbean, SecurityServices services) {
        System.out.println("CustomRealmIdentityAsserterImpl.initialize");
        CustomRealmIdentityAsserterMBean myMBean = (CustomRealmIdentityAsserterMBean) mbean;
        this._mBean = myMBean;
        description = myMBean.getDescription() + "\n" + myMBean.getVersion();
        this.controlFlag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
    }

    public String getDescription() {
        return description;
    }

    public void shutdown() {
        System.out.println("CustomRealmIdentityAsserterImpl.shutdown");
    }

    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    public CallbackHandler assertIdentity(String type, Object token,
                                          ContextHandler context) throws IdentityAssertionException {
        System.out.println(context.getNames().toString());
        System.out.println("CustomRealmIdentityAsserterImpl.assertIdentity");
        System.out.println("\tType\t\t= " + type);
        System.out.println("\tToken\t\t= " + token);

        Object requestValue = context.getValue("com.bea.contextelement.servlet.HttpServletRequest");
        if ((requestValue == null) || (!(requestValue instanceof HttpServletRequest))) {
            System.out.println("do nothing");
        } else {
            HttpServletRequest request = (HttpServletRequest) requestValue;
            java.util.Enumeration names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                System.out.println(name + ":" + request.getHeader(name));
            }
        }

        // Checking we got the right kind of token
        if (!"CustomRealmToken".equals(type)) {
            String str3 =
                "CustomRealmIdentityAsserterImpl received unknown token type \"" + type + "\"." + " Expected " +
                "CustomRealmToken";


            System.out.println("\tError: " + str3);
            String str4 = new String((byte[]) token);
            System.out.println("\tError: " + str4);

            return null;
        }

        // Setting Assertion HashMap
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("IdentityAssertion", "true");
        getConfiguration(hashMap);

        // Checking Token exists
        if (!(token instanceof byte[])) {
            String str =
                "CustomRealmIdentityAsserterImpl received unknown token class \"" + token.getClass() + "\"." +
                " Expected a byte[].";

            System.out.println("\tError: " + str);
            return null;
        }

        // Extract Token
        System.out.println("\tToken: " + token.toString());
        byte[] arrayOfByte = (byte[]) token;
        
        // Checking Token is valid
        if (arrayOfByte == null || arrayOfByte.length < 1) {
            String str = "CustomRealmIdentityAsserterImpl received empty token byte array";

            System.out.println("\tError: " + str);
            return null;
        }

        String tokenStr = new String(arrayOfByte);

        if (!tokenStr.startsWith("username=")) {
            String str =
                "CustomRealmIdentityAsserterImpl received unknown token string \"" + type + "\"." + " Expected " +
                "username=" + "username";

            System.out.println("\tError: " + str);
            return null;
        }


        String tokenValue = tokenStr.substring("username=".length());
        System.out.println("\tuserName\t= " + tokenValue);

        // Trying to fetch user from the DB
        try {
            // CreatedFontTracker a driver
            DriverManager.registerDriver((Driver) new OracleDriver());

            // Get DB connection
            Connection connection =
                DriverManager.getConnection(this._mBean.getDbURL(), this._mBean.getDbUser(),
                                            this._mBean.getDbPassword());
            // Getting SQL query
            String queryString = this._mBean.getDbTokenSql();

            PreparedStatement preparedStatement = connection.prepareStatement(queryString);
            preparedStatement.setString(1, tokenValue);
            
            // Querying the DB
            ResultSet resultSet = preparedStatement.executeQuery();

            String user = "";
            boolean bool = false;
            while (resultSet.next()) {

                user = resultSet.getString(1);

                System.out.println("username : " + user);
                bool = true;
            }

            if (bool) {
                return new CustomRealmCallbackHandlerImpl(user);
            }

            String str5 = "Invalid username token, token not found in users database ";

            System.out.println("\tError: " + str5);
            return null;
        } catch (Exception exception) {

            exception.printStackTrace();
            String str = "database error ";

            System.out.println("\tError: " + str);
            return null;
        }
    }

    public AppConfigurationEntry getLoginModuleConfiguration() {
        System.out.println("CustomRealmIdentityAsserterImpl: getConfiguration of non Assertion!!! ");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("IdentityAssertion", "false");
        return getConfiguration(hashMap);
    }

    private AppConfigurationEntry getConfiguration(HashMap<String, ?> paramHashMap) {
        System.out.println("CustomRealmIdentityAsserterImpl: getConfiguration");

        return new AppConfigurationEntry("com.oracle.il.css.CustomRealmLoginModuleImpl", this.controlFlag,
                                         paramHashMap);
    }

    public AppConfigurationEntry getAssertionModuleConfiguration() {
        System.out.println("CustomRealmIdentityAsserterImpl: getAssertionModuleConfiguration");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();

        hashMap.put("IdentityAssertion", "false");
        return getConfiguration(hashMap);
    }

    public PrincipalValidator getPrincipalValidator() {
        return (PrincipalValidator) new PrincipalValidatorImpl();
    }
}
