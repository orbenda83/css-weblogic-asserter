package com.oracle.il.css;

public interface CustomRealmIdentityAsserterMBean {
    String getHeaderName();
    void setHeaderName(String headerName);

    boolean isDebugEnabled();
    void setDebugEnabled(boolean debugEnabled);
} 