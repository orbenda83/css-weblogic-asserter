package il.ac.bgu.it.identityassertion;

import javax.management.InvalidAttributeValueException;
import weblogic.descriptor.DescriptorBean;
import weblogic.management.commo.StandardInterface;
import weblogic.management.security.authentication.IdentityAsserterMBean;

public interface BguIdentityAsserterMBean extends StandardInterface, DescriptorBean, IdentityAsserterMBean {
  String getProviderClassName();
  String getDescription();
  String getVersion();
  String[] getSupportedTypes();
  String[] getActiveTypes();
  void setActiveTypes(String[] paramArrayOfString) throws InvalidAttributeValueException;
  boolean getBase64DecodingRequired();
  String getDbURL();
  void setDbURL(String paramString) throws InvalidAttributeValueException;
  String getDbUser();
  void setDbUser(String paramString) throws InvalidAttributeValueException;
  String getDbPassword();
  void setDbPassword(String paramString) throws InvalidAttributeValueException;
  String getDbTokenSql();
  void setDbTokenSql(String paramString) throws InvalidAttributeValueException;
  String getName();
} 