package weblogic.management.security;


import  java.util.ArrayList;
import  java.util.Map;
import  java.util.HashMap;
import  java.util.Set;
import  weblogic.utils.codegen.ImplementationFactory;
import  weblogic.utils.codegen.RoleInfoImplementationFactory;




/**
 * This is a generated class that provides a mapping from 
 * interface classes to implementation classes
 */
public class SAMPLEIDENTITYASSERTERBeanInfoFactory implements RoleInfoImplementationFactory {
  private static final Map interfaceMap;
  private static final ArrayList roleInfoList;
  private static final SAMPLEIDENTITYASSERTERBeanInfoFactory SINGLETON;
  static {
    interfaceMap = new HashMap(1);
    interfaceMap.put("examples.security.providers.identityassertion.simple.SimpleSampleIdentityAsserterMBean","examples.security.providers.identityassertion.simple.SimpleSampleIdentityAsserterMBeanImplBeanInfo");
    roleInfoList = new ArrayList(1);
    roleInfoList.add("examples.security.providers.identityassertion.simple.SimpleSampleIdentityAsserterMBean");
    SINGLETON = new SAMPLEIDENTITYASSERTERBeanInfoFactory();
  }


  public static final ImplementationFactory getInstance() {
    return SINGLETON; 
  }


  public String getImplementationClassName( String interfaceName ) {
    return (String)interfaceMap.get(interfaceName);
  }


  public String[] getInterfaces() {
    Set keySet = interfaceMap.keySet();
    return (String[])keySet.toArray(new String[keySet.size()]);
  }


  public String[] getInterfacesWithRoleInfo() {
    return (String[])roleInfoList.toArray(new String[roleInfoList.size()]);
  }

  public String getRoleInfoImplementationFactoryTimestamp() {
    return "1753438296542";
  }


}
