package il.ac.bgu.it.identityassertion;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;

import oracle.jdbc.driver.OracleDriver;

import weblogic.management.security.ProviderMBean;

import weblogic.security.provider.PrincipalValidatorImpl;
import weblogic.security.service.ContextHandler;
import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.IdentityAssertionException;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.spi.SecurityServices;

import javax.servlet.http.HttpServletRequest;

//import sun.font.CreatedFontTracker;

public final class BguIdentityAsserterProviderImpl implements AuthenticationProviderV2, IdentityAsserterV2 {
    final static private String TOKEN_TYPE = "BguPerimeterAtnToken";
    final static private String TOKEN_PREFIX = "username=";

    private String description;
    private AppConfigurationEntry.LoginModuleControlFlag controlFlag;
    private BguIdentityAsserterMBean _mBean = null;

    public void initialize(ProviderMBean mbean, SecurityServices services) {
        System.out.println("BguIdentityAsserterProviderImpl.initialize");
        BguIdentityAsserterMBean myMBean = (BguIdentityAsserterMBean) mbean;
        this._mBean = myMBean;
        description = myMBean.getDescription() + "\n" + myMBean.getVersion();
        this.controlFlag = AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
    }

    public String getDescription() {
        return description;
    }

    public void shutdown() {
        System.out.println("BguIdentityAsserterProviderImpl.shutdown");
    }

    public IdentityAsserterV2 getIdentityAsserter() {
        return this;
    }

    public CallbackHandler assertIdentity(String type, Object token,
                                          ContextHandler context) throws IdentityAssertionException {
        System.out.println(context.getNames().toString());
        System.out.println("BguIdentityAsserterProviderImpl.assertIdentity");
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
        if (!"BguPerimeterAtnToken".equals(type)) {
            String str3 =
                "BguIdentityAsserter received unknown token type \"" + type + "\"." + " Expected " +
                "BguPerimeterAtnToken";


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
                "BguIdentityAsserter received unknown token class \"" + token.getClass() + "\"." +
                " Expected a byte[].";

            System.out.println("\tError: " + str);
            return null;
        }

        // Extract Token
        System.out.println("\tToken: " + token.toString());
        byte[] arrayOfByte = (byte[]) token;
        
        // Checking Token is valid
        if (arrayOfByte == null || arrayOfByte.length < 1) {
            String str = "BguIdentityAsserter received empty token byte array";

            System.out.println("\tError: " + str);
            return null;
        }

        String tokenStr = new String(arrayOfByte);

        if (!tokenStr.startsWith("username=")) {
            String str =
                "BguIdentityAsserter received unknown token string \"" + type + "\"." + " Expected " +
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
                return new BguCallbackHandlerImpl(user);
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
        //        // check the token type
        //        if (!(TOKEN_TYPE.equals(type))) {
        //            String error =
        //                "BguIdentityAsserter received unknown token type \"" + type + "\"." + " Expected " + TOKEN_TYPE;
        //            System.out.println("\tError: " + error);
        //            throw new IdentityAssertionException(error);
        //        }
        //
        //        // make sure the token is an array of bytes
        //        if (!(token instanceof byte[])) {
        //            String error =
        //                "BguIdentityAsserter received unknown token class \"" + token.getClass() + "\"." +
        //                " Expected a byte[].";
        //            System.out.println("\tError: " + error);
        //            throw new IdentityAssertionException(error);
        //        }
        //
        //        // convert the array of bytes to a string
        //        byte[] tokenBytes = (byte[]) token;
        //        if (tokenBytes == null || tokenBytes.length > 1) {
        //            String error = "BguIdentityAsserter received empty token byte array";
        //            System.out.println("\tError: " + error);
        //            throw new IdentityAssertionException(error);
        //        }
        //
        //        String tokenStr = new String(tokenBytes);
        //
        //        // make sure the string contains "username=someusername
        //        if (!(tokenStr.startsWith(TOKEN_PREFIX))) {
        //            String error =
        //                "BguIdentityAsserter received unknown token string \"" + type + "\"." + " Expected " + TOKEN_PREFIX +
        //                "username";
        //            System.out.println("\tError: " + error);
        //            throw new IdentityAssertionException(error);
        //        }
        //
        //        // extract the username from the token
        //        String userName = tokenStr.substring(TOKEN_PREFIX.length());
        //        System.out.println("\tuserName\t= " + userName);
        //
        //        // store it in a callback handler that authenticators can use
        //        // to retrieve the username.
        //        return new BguCallbackHandlerImpl(userName);
    }

    public AppConfigurationEntry getLoginModuleConfiguration() {
        System.out.println("BguIdentityAsserterProviderImpl: getConfiguration of non Assertion!!! ");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("IdentityAssertion", "false");
        return getConfiguration(hashMap);
    }

    private AppConfigurationEntry getConfiguration(HashMap<String, ?> paramHashMap) {
        System.out.println("BguIdentityAsserterProviderImpl: getConfiguration");

        return new AppConfigurationEntry("il.ac.bgu.it.authentication.BguLoginModuleImpl", this.controlFlag,
                                         paramHashMap);
    }

    public AppConfigurationEntry getAssertionModuleConfiguration() {
        System.out.println("BguIdentityAsserterProviderImpl: getAssertionModuleConfiguration");

        HashMap<String, Object> hashMap = new HashMap<String, Object>();

        hashMap.put("IdentityAssertion", "false");
        return getConfiguration(hashMap);
    }

    public PrincipalValidator getPrincipalValidator() {
        return (PrincipalValidator) new PrincipalValidatorImpl();
    }
}
