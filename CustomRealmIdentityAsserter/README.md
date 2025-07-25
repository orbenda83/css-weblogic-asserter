# CustomRealmIdentityAsserter

## Overview
This project provides a custom Identity Asserter for Oracle WebLogic Server. It allows WebLogic to assert user identity based on a configurable HTTP header, typically set by a trusted proxy or authentication gateway. The asserter is configurable (header name, debug flag) and is designed to integrate with enterprise authentication (such as Active Directory) via WebLogic’s security realm.

**Key Features:**
- Extracts username from a configurable HTTP header (e.g., `X-User-Id`).
- Debug logging via a flag (now uses standard Java logging).
- No embedded LDAP logic; relies on WebLogic’s configured authenticators.
- Packaged as a JAR for easy deployment.

## Prerequisites
- Oracle WebLogic Server (12.2.1 or compatible)
- Java 8 (JDK 1.8)
- Maven
- Access to required WebLogic JARs (see below)

## Installation

### 1. Prepare Dependencies
Copy the following JARs from your WebLogic installation into the `lib/` directory of this project:
- `wls-api.jar`
- `weblogic.jar`

> **Note:** WebLogic logging dependencies are commented out in the `pom.xml` because the project now uses standard Java logging. If you want to use WebLogic logging, you must resolve all transitive dependencies.

### 2. Build the Project
From the project root:
```sh
mvn clean package
```
The output JAR will be in `target/CustomRealmIdentityAsserter-1.0-SNAPSHOT.jar`.

### 3. Deploy to WebLogic
- Copy the JAR to `$WL_HOME/server/lib/mbeantypes` on your WebLogic server.
- Restart WebLogic.

### 4. Configure in WebLogic Console
1. Go to **Security Realms** > **myrealm** > **Providers** > **Authentication** > **New**.
2. Select `CustomRealmIdentityAsserter` and give it a name (e.g., `CustomADAsserter`).
3. Set the **Control Flag** (e.g., `SUFFICIENT` or `REQUIRED`).
4. Set **HeaderName** (e.g., `X-User-Id`).
5. Set **DebugEnabled** as needed.
6. Ensure your Active Directory or other authenticator is configured and prioritized.

## Testing

### 1. Web Application Setup
- In your `web.xml`, set:
  ```xml
  <login-config>
      <auth-method>CLIENT-CERT</auth-method>
  </login-config>
  ```
- Protect a servlet/resource to trigger authentication.

### 2. Send Test Requests
- Use `curl` or Postman:
  ```sh
  curl -i -H "X-User-Id: valid_ad_user" http://your-weblogic-server:port/your-app/protected-resource
  ```
- Test with valid, invalid, and missing headers. Toggle debug flag and check logs.

### 3. Troubleshooting
- If you see missing class errors, ensure all required JARs are in `lib/` and referenced in `pom.xml`.
- If you want to use WebLogic logging, uncomment the dependencies in `pom.xml` and ensure all transitive dependencies are present.
- Check WebLogic logs for debug output if enabled.

## References
- [WebLogic Logger Class Reference](https://docs.oracle.com/en/middleware/fusion-middleware/weblogic-server/12.2.1.4/logsv/loggerclasses.html)
- [WebLogic Security Providers Documentation](https://docs.oracle.com/middleware/1221/wls/DEVSP/toc.htm)

---

**This solution provides a flexible, header-based identity assertion mechanism for WebLogic, suitable for integration with enterprise SSO and authentication proxies.** 