package examples.security.providers.identityassertion.simple;


import javax.management.*;
import weblogic.management.commo.RequiredModelMBeanWrapper;



/**
 * No description provided.
 * @root SimpleSampleIdentityAsserter
 * @customizer examples.security.providers.identityassertion.simple.SimpleSampleIdentityAsserterImpl(new RequiredModelMBeanWrapper(this))
 * @dynamic false

 */
public interface SimpleSampleIdentityAsserterMBean extends weblogic.management.commo.StandardInterface,weblogic.descriptor.DescriptorBean, weblogic.management.security.authentication.IdentityAsserterMBean {
                
        


        /**
         * No description provided.

         * @preprocessor weblogic.management.configuration.LegalHelper.checkClassName(value)
         * @default "examples.security.providers.identityassertion.simple.SimpleSampleIdentityAsserterProviderImpl"
         * @dynamic false
         * @non-configurable
         * @validatePropertyDeclaration false

         * @preserveWhiteSpace
         */
        public java.lang.String getProviderClassName ();


        
        


        /**
         * No description provided.

         * @default "WebLogic Simple Sample Identity Asserter Provider"
         * @dynamic false
         * @non-configurable
         * @validatePropertyDeclaration false

         * @preserveWhiteSpace
         */
        public java.lang.String getDescription ();


        
        


        /**
         * No description provided.

         * @default "1.0"
         * @dynamic false
         * @non-configurable
         * @validatePropertyDeclaration false

         * @preserveWhiteSpace
         */
        public java.lang.String getVersion ();


        
        


        /**
         * No description provided.

         * @default "SamplePerimeterAtnToken"
         * @dynamic false
         * @non-configurable
         * @validatePropertyDeclaration false

         * @preserveWhiteSpace
         */
        public java.lang.String[] getSupportedTypes ();


        
        


        /**
         * No description provided.

         * @default "SamplePerimeterAtnToken"
         * @dynamic false

         * @preserveWhiteSpace
         */
        public java.lang.String[] getActiveTypes ();


        /**
         * No description provided.

         * @default "SamplePerimeterAtnToken"
         * @dynamic false

         * @param newValue - new value for attribute ActiveTypes
         * @exception InvalidAttributeValueException
         * @preserveWhiteSpace
         */
        public void setActiveTypes (java.lang.String[] newValue)
                throws InvalidAttributeValueException;


        
        


        /**
         * No description provided.

         * @default false
         * @dynamic false

         * @preserveWhiteSpace
         */
        public boolean getBase64DecodingRequired ();


        /**
         * No description provided.

         * @default false
         * @dynamic false

         * @param newValue - new value for attribute Base64DecodingRequired
         * @exception InvalidAttributeValueException
         * @preserveWhiteSpace
         */
        public void setBase64DecodingRequired (boolean newValue)
                throws InvalidAttributeValueException;



        
        /**
         * @default "SimpleSampleIdentityAsserter"
         * @dynamic false
         * @owner RealmAdministrator
         * @VisibleToPartitions ALWAYS
         */
         public java.lang.String getName();

          

}
