package com.oracle.il.css;

import weblogic.management.security.authentication.IdentityAsserterMBean;

public interface CustomRealmIdentityAsserterMBean extends IdentityAsserterMBean {
    // Existing methods
    String getHeaderName();
    boolean getDebugEnabled();
    String getDescription();
    String getVersion();

    // NEW: Supported Types (maps to "Active Types" in console)
    String[] getSupportedTypes(); // WebLogic expects this to show the "Active Types"

    // NEW: LoginModuleSufficient flag
    boolean getLoginModuleSufficient();
}