# BGU Identity Asserter (SSO)
------

Includes Asserter code with Oracle DB + Web app to connect to the asserter  

## Table of Contents  
* [ Project Description ](#description)  
* [ BPM SSO USER_TOKEN Table ](#table) 
* [ WEB Module ](#web)  
* [ Custom Identity Asserter ](#asserter)  


### <a name="description">Project Description:</a> 
---
The project is built from 2 modules and a table in the DB:  
1. Custom Identity Asserter (Built for BGU based on **USER_TOKEN table in BPMSSO schema**).
2. WEB Application to connect to the asserter and assert the login token to Weblogic JSESSION.

The purpose of this project is to facilitate the users need to have an Single-Sign-On (SSO) between Aristo login and BPM workspace. 
Also, there is the operations need for impersionating a user in troubleshooting user's issues on the system.


### <a name="table">BPM SSO USER_TOKEN Table</a>
---
The SSO table is constructed of 2 columns in the following structure (Script to create is in CreateTokenTable.sql file):

**Column Name** | **Column Type** | **Description**  
--------------- | --------------- | ---------------  
U_NAME | VARCHAR2(200 BYTE) | The logged in user name from Aristo and OID (uid)  
USER_NAME_TOKEN | VARCHAR2(200 BYTE) | The user token inserted from Aristo, used for the assertion and navigation to BPM Workspace  


### <a name="web">WEB Module</a>
---
The project name is BguAsserterWeb and it consists of a web appliction with 2 html pages:
* **hi.html** file for debug
* **html_file.html** for asserter connection

The URL for connecting to the app is:  
```
http://<server address>:<server port (8001)>/html/html_file.html?username=<user_token from table>
```
For example in Dev 12c:  
```
http://testlinbpm1.bgu.ac.il:8001/html/html_file.html?username=12345
```

#### hi.html
This file is deployed on the system in order to debug the SSO system.

The web page invokes is built from the following body:
```html
<body>
        <h1>BGU Asserter Tester</h1>
        <p id="params"></p>
        <script language="Javascript">
        ....
        </script>
    </body>
```

In the script section we have the following functions:
```javascript
function init() {
    var location_st = window.location.href.toString();
    var fullUrl = location.protocol+'//'+location.hostname+(location.port ? ':'+location.port: '');

    if (location_st.indexOf("username=") >  - 1) {
        var username = location_st.substring(location_st.indexOf("username=") + 9);
    }
    else {
        return '<h2>No parameter recieved</h2>';
    }

    return "<h2>BguPerimeterAtnToken</h2><p><span>Recieved username: " + username + "</span><br /><span>Server URL: " + fullUrl + "</span></p>";
}
```
**Notes:** 
* The `location_st` variable consists of the full URL called by the user (with querystring included).
* The `fullUrl` variable is set to have the server connected to from the url (i.e. http://testlinbpm1.bgu.ac.il:8001)
* The *if* statement checks if a 'username' variable was recieved via the querystring.
    * If exists, the username sent (i.e. the user token from the USER_TOKEN table) is extracted from the querystring.
    * Otherwise there is an error
* After parameter extraction the embedded html with the details is built and returned.

The part that invokes the function and shows the html fragment is:
```javascript
document.getElementById("params").innerHTML = init();
```
**Notes:** 
* 'document.getElementById("params")' finds the html element with id params that we have created.
* 'innerHTML = init()' invokes the init() method and puts the embedded html value into the innerHTML of the "params" element.

#### html_file.html
This file is deployed on the system in order to make the SSO connection on the Weblogic system.

The web page invokes upon web page load the init script:
```html
<body onload="init()"></body>
```

In the script section we have the following functions:
```javascript
function init() {
    var location_st = window.location.href.toString();
    var date = new Date();
    var fullUrl = location.protocol+'//'+location.hostname+(location.port ? ':'+location.port: '');
    var bpmWorklist = "/bpm/workspace/faces/jsf/worklist/worklist.jspx";

    if (location_st.indexOf("username=") >  - 1) {
        var username = location_st.substring(location_st.indexOf("username=") + 9);
    }
    else {
        return;
    }

    var req = new XMLHttpRequest();
    req.open("GET", bpmWorklist, false);
    req.setRequestHeader("BguPerimeterAtnToken", "username=" + username);
    req.setRequestHeader("RequestTimestamp", date);

    req.responseType = "Document";

    req.send();
    navigateMe(fullUrl, bpmWorklist);
}
```
**Notes:** 
* The `location_st` variable consists of the full URL called by the user (with querystring included).
* The `fullUrl` variable is set to have the server connected to from the url (i.e. http://testlinbpm1.bgu.ac.il:8001)
* The `date` variable is a timestamp for the asserter logs to print (in order to identify the time of the assert - Debugging) 
* The `bpmWorklist` variable has a static URI string of the relative path for connection to the *BPM Workspace*.  
* The *if* statement checks if a 'username' variable was recieved via the querystring.
    * If exists, the username sent (i.e. the user token from the USER_TOKEN table) is extracted from the querystring.
    * Otherwise there is an error and the script does nothing.
    TODO: Change the script to write some error to the screen instead of doing nothing.
* After parameter extraction the scripts navigates to the BPM workspace.
    * An HTTP GET Request is constructed with `BguPerimeterAtnToken` and `RequestTimestamp` headers added.
    * The HTTP GET is sent to the server for the login.
    * The user browser is redirected to the BPM workspace main page.

The part that invokes navigates the user to the BPM is:
```javascript
function navigateMe(fullUrl, bpmWorklist) {
    window.location.href = fullUrl + bpmWorklist;
}
```

### <a name="asserter" href="#asserter">Custom Identity Asserter</a>
---
The project is based on the following resources:
- https://docs.oracle.com/middleware/1221/wls/DEVSP/ia.htm#DEVSP244
- https://docs.oracle.com/middleware/1221/wls/DEVSP/design.htm#DEVSP124
- http://weblogic-wonders.com/weblogic/2014/01/13/simple-sample-custom-identity-asserter-weblogic-server-12c/
- https://danielveselka.blogspot.com/2011/10/weblogic-custom-identity-asserter.html
- https://gist.github.com/kares/356576

The project name is BguIdentityAsserter and it consists of the following resources:
* **pom.xml** _Maven_ descriptor file for building the project
* Under **src/il/ac/bgu/it/identityassertion**: 
    * **BguIdentityAsserter.xml** xml file that constructs the annotations for building the MBean java class (MBean definition).
    * **BguIdentityAsserterProviderImpl.java** Java class implementation for the provider that implements AuthenticationProviderV2 and IdentityAsserterV2 of Weblogic.
    * **BguCallbackHandlerImpl.java** Java class implementation for the provider that implements a callback handler.
* Under **src/il/ac/bgu/it/authentication**: 
    * **BguLoginModuleImpl.java** Java class implementation for the provider that implements Java LoginModule and security principals of Weblogic.

#### BguIdentityAsserter.xml
Inherits the schema from commo.dtd of weblogic and sets the prameters of the MBean.
In the xml file there is an inline documentaion of the structure.

Declaration of the MBean:
```xml
<MBeanType
 Name          = "BguIdentityAsserter"
 DisplayName   = "BguIdentityAsserter"
 Package       = "il.ac.bgu.it.identityassertion"
 Extends       = "weblogic.management.security.authentication.IdentityAsserter"
 PersistPolicy = "OnUpdate"
>
```

Refer to the provider implementation class:
```xml
<MBeanAttribute
  Name          = "ProviderClassName"
  Type          = "java.lang.String"
  Writeable     = "false"
  Preprocessor = "weblogic.management.configuration.LegalHelper.checkClassName(value)"
  Default       = "&quot;il.ac.bgu.it.identityassertion.BguIdentityAsserterProviderImpl&quot;"
 />
 ```

Set description and version of the provider:
```xml
<MBeanAttribute
  Name          = "Description"
  Type          = "java.lang.String"
  Writeable     = "false"
  Default       = "&quot;WebLogic BGU Custom Identity Asserter Provider&quot;"
 />

 <MBeanAttribute
  Name          = "Version"
  Type          = "java.lang.String"
  Writeable     = "false"
  Default       = "&quot;1.0&quot;"
 />
```

Setting the Supported types (HTTP headers) and the Active types for the current work (a subset of the supported types array):
```xml
<MBeanAttribute  
  Name         = "SupportedTypes"
  Type         = "java.lang.String[]"
  Writeable     = "false"
  Default     = "new String[] { &quot;GET&quot;, &quot;Content-Type&quot;, &quot;Accept&quot;, &quot;BguPerimeterAtnToken&quot;, &quot;Host&quot;, &quot;Location&quot;, &quot;Cookie&quot;, &quot;Set-Cookie&quot;  }"
 />

 <MBeanAttribute  
  Name         = "ActiveTypes"
  Type         = "java.lang.String[]"
  Default     = "new String[] { &quot;GET&quot;, &quot;BguPerimeterAtnToken&quot;, &quot;Cookie&quot;, &quot;Set-Cookie&quot; }"
 />
 ```

 Other custom attributes to use (in our case the fields of the DB connection):
 ```xml
 <MBeanAttribute
  Name         = "Base64DecodingRequired"
  Type         = "boolean"
  Writeable    = "false"
  Default      = "false"
  Description  = "See MyIdentityAsserter-doc.xml."
/>
<MBeanAttribute
  Name          = "DbURL"
  Type          = "java.lang.String"
  Writeable     = "true"
  Default       = "&quot;jdbc:oracle:thin:@localhost:1521:XE&quot;"
  DisplayName   = "&quot;Indicate the JDBC URL for connecting and authenticating database users.&quot;"
 />
 <MBeanAttribute
  Name          = "DbUser"
  Type          = "java.lang.String"
  Writeable     = "true"
  Default       = "&quot;bpm_sso&quot;"
  DisplayName   = "&quot;Indicate the JDBC URL for connecting and authenticating database users.&quot;"
 />
 <MBeanAttribute
  Name          = "DbPassword"
  Type          = "java.lang.String"
  Writeable     = "true"
  Default       = "&quot;bpm_sso&quot;"
  DisplayName   = "&quot;Indicate the JDBC URL for connecting and authenticating database users.&quot;"
 />
 <MBeanAttribute
  Name          = "DbTokenSql"
  Type          = "java.lang.String"
  Writeable     = "true"
  Default       = "&quot;select U_NAME from USER_TOKEN where USER_NAME_TOKEN = ?&quot;"
  DisplayName   = "&quot;Indicate the JDBC URL for connecting and authenticating database users.&quot;"
 />
 ```

#### BguIdentityAsserterProviderImpl.java
The provider java class that implements AuthenticationProviderV2, IdentityAsserterV2 classes for identity assertion to weblogic.

The class declares the values to use and internal use variables:
```java
final static private String TOKEN_TYPE = "BguPerimeterAtnToken";
final static private String TOKEN_PREFIX = "username=";

private String description;
private AppConfigurationEntry.LoginModuleControlFlag controlFlag;
private BguIdentityAsserterMBean _mBean = null;
```

Methods high level explanation:
* The `public void initialize(ProviderMBean mbean, SecurityServices services)` method initializes the assertion provider mbean and sets the control flag to *_SUFFICIENT_*.
* `public String getDescription()` getter function for the description value.
* `public void shutdown()` prints a shutdown message.
* `public IdentityAsserterV2 getIdentityAsserter()` getter function for returning the mbean instance.
* `public CallbackHandler assertIdentity(String type, Object token, ContextHandler context) throws IdentityAssertionException` The main asserter function that validates the token and asserts the user in the JAAS identity assertion mechanism and returns the callback function to the application.
* `public AppConfigurationEntry getLoginModuleConfiguration()` returns the login module to the user for completing the user login.
* `private AppConfigurationEntry getConfiguration(HashMap<String, ?> paramHashMap)` returns current app configuration.
* `public AppConfigurationEntry getAssertionModuleConfiguration()` getter function for the assertion module configuration.
* `public PrincipalValidator getPrincipalValidator()` returns the principal validator instance.

#### BguCallbackHandlerImpl.java
The provider java class that implements CallbackHandler interface for identity assertion to weblogic.

The class declares the values to use and internal use variables:
```java
private String userName;
```

Methods high level explanation:
* `BguCallbackHandlerImpl(String user)` constructor that sets the username of the user to the instance.
* `public void handle(Callback[] callbacks) throws UnsupportedCallbackException` handles the callback to the user with the authenticated user.


#### BguLoginModuleImpl.java 
Java class implementation for the provider that implements Java LoginModule and security principals of Weblogic.

The class declares the values to use and internal use variables:
```java
private Subject subject;
private CallbackHandler callbackHandler;
private boolean isIdentityAssertion;
private boolean loginSucceeded;
private boolean principalsInSubject;
private Vector principalsForSubject = new Vector();
```

Methods high level explanation:
* `public void initialize(Subject paramSubject, CallbackHandler paramCallbackHandler, Map paramMap1, Map paramMap2)` Initializes the login module.
* `public boolean login() throws LoginExceptionn` Performs the login in the weblogic server by creating and adding `WLSUserImpl` object.
* `public boolean commit() throws LoginException` Commits the succesfull logins to Weblogic.
* `public boolean abort() throws LoginException` Clears all logins from the server.
* `public boolean logout() throws LoginException` Logsout (I don't think it is implemented)
* `private void throwLoginException(String paramString) throws LoginException` and `private void throwFailedLoginException(String paramString) throws FailedLoginException` Throws login exceptions.
* `private Callback[] getCallbacks() throws LoginException` returns the callback handler for the login.
* `private String getUserName(Callback[] paramArrayOfCallback) throws LoginException` returns the username stored in the callback handler.
* `private void addGroupsForSubject(String paramString)` returns the user groups (for the assigend roles).
* `private String getPasswordHave(String paramString, Callback[] paramArrayOfCallback) throws LoginException` returns the password provided by the login module.
