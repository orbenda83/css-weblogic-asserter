package examples.security.providers.identityassertion.simple;


import javax.management.*;
import javax.management.modelmbean.ModelMBean;
import weblogic.management.commo.CommoMBeanInstance;
import java.lang.reflect.*;


public  class SimpleSampleIdentityAsserterImpl extends weblogic.management.security.authentication.IdentityAsserterImpl
  implements java.io.Serializable
{
   static final long serialVersionUID = 1L;

   
   /**
    * @deprecated Replaced by SimpleSampleIdentityAsserterImpl (ModelMBean base).
    */
   public SimpleSampleIdentityAsserterImpl (CommoMBeanInstance base)
        throws MBeanException
   { super(base); }




   //****************************************************************************************************
   //***************************************** GENERATED METHODS ****************************************
   //****************************************************************************************************


   //****************************************************************************************************
   //******************************************* METHODS STUBS ******************************************
   //****************************************************************************************************
//@constructorMethods


}

