package il.ac.bgu.it.authentication;

import java.io.IOException;

import java.util.Map;
import java.util.Vector;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import weblogic.security.principal.WLSGroupImpl;
import weblogic.security.principal.WLSUserImpl;

public final class BguLoginModuleImpl implements LoginModule {
    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean isIdentityAssertion;
    private boolean loginSucceeded;
    private boolean principalsInSubject;
    private Vector principalsForSubject = new Vector();

    public void initialize(Subject paramSubject, CallbackHandler paramCallbackHandler, Map paramMap1, Map paramMap2) {
        System.out.println("BguLoginModuleImpl.initialize");
        this.subject = paramSubject;
        this.callbackHandler = paramCallbackHandler;

        String str = (String) paramMap2.get("IdentityAssertion");
        System.out.println("BguLoginModuleImpl.initialize IA : " + str);

        this.isIdentityAssertion = "true".equalsIgnoreCase((String) paramMap2.get("IdentityAssertion"));
    }


    public boolean login() throws LoginException {
        System.out.println("BguLoginModuleImpl.login");


        Callback[] arrayOfCallback = getCallbacks();


        String str = getUserName(arrayOfCallback);

        if (str.length() > 0) {

            System.out.println("\tuserName=" + str);
        } else {

            System.out.println("\tempty userName");
        }

        this.loginSucceeded = false;


        this.principalsForSubject.add(new WLSUserImpl(str));
        addGroupsForSubject(str);

        return this.loginSucceeded;
    }


    public boolean commit() throws LoginException {
        System.out.println("BguLoginModule.commit");
        if (this.loginSucceeded) {


            this.subject
                .getPrincipals()
                .addAll(this.principalsForSubject);
            this.principalsInSubject = true;
            return true;
        }
        /* 182 */return false;
    }


    public boolean abort() throws LoginException {
        System.out.println("BguLoginModule.abort");
        if (this.principalsInSubject) {
            this.subject
                .getPrincipals()
                .removeAll(this.principalsForSubject);
            this.principalsInSubject = false;
        }
        return true;
    }


    public boolean logout() throws LoginException {
        System.out.println("BguLoginModule.logout");
        return true;
    }


    private void throwLoginException(String paramString) throws LoginException {
        System.out.println("Throwing LoginException(" + paramString + ")");
        throw new LoginException(paramString);
    }


    private void throwFailedLoginException(String paramString) throws FailedLoginException {
        System.out.println("Throwing FailedLoginException(" + paramString + ")");
        throw new FailedLoginException(paramString);
    }


    private Callback[] getCallbacks() throws LoginException {
        if (this.callbackHandler == null) {
            throwLoginException("No CallbackHandler Specified");
        }


        Callback[] arrayOfCallback = new Callback[2];

        arrayOfCallback[1] = new PasswordCallback("password: ", false);


        arrayOfCallback[0] = new NameCallback("username: ");


        try {
            this.callbackHandler.handle(arrayOfCallback);

        } catch (IOException iOException) {
            throw new LoginException(iOException.toString());

        } catch (UnsupportedCallbackException unsupportedCallbackException) {
            throwLoginException(unsupportedCallbackException.toString() + " " +
                                unsupportedCallbackException.getCallback().toString());
        }

        return arrayOfCallback;
    }


    private String getUserName(Callback[] paramArrayOfCallback) throws LoginException {
        String str = ((NameCallback) paramArrayOfCallback[0]).getName();
        if (str == null) {
            throwLoginException("Username not supplied.");
        }
        System.out.println("\tuserName\t= " + str);
        return str;
    }


    private void addGroupsForSubject(String paramString) {
        String str = "BguPerimeterAtnUsers";
        System.out.println("\tgroupName\t= " + str);
        this.principalsForSubject.add(new WLSGroupImpl(str));
    }


    private String getPasswordHave(String paramString, Callback[] paramArrayOfCallback) throws LoginException {
        PasswordCallback passwordCallback = (PasswordCallback) paramArrayOfCallback[1];
        char[] arrayOfChar = passwordCallback.getPassword();
        passwordCallback.clearPassword();
        if (arrayOfChar == null || arrayOfChar.length < 1) {
            throwLoginException("Authentication Failed: User " + paramString + ".  Password not supplied");
        }
        String str = new String(arrayOfChar);
        System.out.println("\tpasswordHave\t= " + str);
        return str;
    }
}
