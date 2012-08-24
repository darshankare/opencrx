/*
 * ====================================================================
 * Project:     openCRX/Core, http://www.opencrx.org/
 * Description: Utils
 * Owner:       CRIXP AG, Switzerland, http://www.crixp.com
 * ====================================================================
 *
 * This software is published under the BSD license
 * as listed below.
 * 
 * Copyright (c) 2004-2008, CRIXP Corp., Switzerland
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 * 
 * * Neither the name of CRIXP Corp. nor the names of the contributors
 * to openCRX may be used to endorse or promote products derived
 * from this software without specific prior written permission
 * 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * ------------------
 * 
 * This product includes software developed by the Apache Software
 * Foundation (http://www.apache.org/).
 * 
 * This product includes software developed by contributors to
 * openMDX (http://www.openmdx.org/)
 */
package org.opencrx.kernel.utils;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.Constants;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.resource.cci.ConnectionFactory;

import org.oasisopen.jmi1.RefContainer;
import org.opencrx.kernel.account1.jmi1.Account1Package;
import org.opencrx.kernel.activity1.jmi1.Activity1Package;
import org.opencrx.kernel.admin1.jmi1.Admin1Package;
import org.opencrx.kernel.building1.jmi1.Building1Package;
import org.opencrx.kernel.contract1.jmi1.Contract1Package;
import org.opencrx.kernel.depot1.jmi1.Depot1Package;
import org.opencrx.kernel.forecast1.jmi1.Forecast1Package;
import org.opencrx.kernel.generic.jmi1.GenericPackage;
import org.opencrx.kernel.home1.jmi1.Home1Package;
import org.opencrx.kernel.home1.jmi1.UserHome;
import org.opencrx.kernel.product1.jmi1.Product1Package;
import org.opencrx.kernel.uom1.jmi1.Uom1Package;
import org.opencrx.security.realm1.jmi1.Realm1Package;
import org.openmdx.application.rest.ejb.DataManager_2ProxyFactory;
import org.openmdx.application.rest.http.SimplePort;
import org.openmdx.application.rest.spi.EntityManagerProxyFactory_2;
import org.openmdx.base.accessor.jmi.cci.RefObject_1_0;
import org.openmdx.base.accessor.jmi.spi.EntityManagerFactory_1;
import org.openmdx.base.exception.ServiceException;
import org.openmdx.base.jmi1.Authority;
import org.openmdx.base.mof.cci.AggregationKind;
import org.openmdx.base.mof.cci.ModelElement_1_0;
import org.openmdx.base.mof.cci.Model_1_0;
import org.openmdx.base.mof.spi.Model_1Factory;
import org.openmdx.base.naming.Path;
import org.openmdx.base.persistence.cci.ConfigurableProperty;
import org.openmdx.base.persistence.cci.UserObjects;
import org.openmdx.base.rest.spi.ConnectionFactoryAdapter;
import org.openmdx.base.text.conversion.UUIDConversion;
import org.openmdx.base.transaction.TransactionAttributeType;
import org.openmdx.kernel.id.UUIDs;
import org.openmdx.kernel.lightweight.naming.NonManagedInitialContextFactoryBuilder;
import org.openmdx.portal.servlet.UserSettings;
import org.openmdx.portal.servlet.attribute.DateValue;
import org.openmdx.security.realm1.jmi1.Permission;
import org.openmdx.security.realm1.jmi1.Role;

public abstract class Utils {

    //-----------------------------------------------------------------------
    public static Model_1_0 getModel(
    ) {
        Model_1_0 model = null;
        try {
            model = Model_1Factory.getModel();
        }
        catch(Exception e) {
            System.out.println("Can not initialize model repository " + e.getMessage());
            System.out.println(new ServiceException(e).getCause());
        }
        return model;
    }
        
    //-----------------------------------------------------------------------
    public static PersistenceManagerFactory getPersistenceManagerFactory(
    ) throws NamingException, ServiceException {
        return JDOHelper.getPersistenceManagerFactory("EntityManagerFactory");
    }

    //-----------------------------------------------------------------------
    public static PersistenceManagerFactory getPersistenceManagerFactoryProxy(
    	String url,
    	String userName,
    	String password,
    	String mimeType
    ) throws NamingException, ServiceException {
        if(!NamingManager.hasInitialContextFactoryBuilder()) {
            NonManagedInitialContextFactoryBuilder.install(null);
        }
    	SimplePort port = new SimplePort();
    	port.setMimeType(mimeType == null ? "application/vnd.openmdx.wbxml" : mimeType);
    	port.setUserName(userName);
    	port.setPassword(password);
    	port.setConnectionURL(url);
        ConnectionFactory connectionFactory = new ConnectionFactoryAdapter(
        	port,
            true, // supportsLocalTransactionDemarcation
            TransactionAttributeType.NEVER
        );
        Map<String,Object> dataManagerProxyConfiguration = new HashMap<String,Object>();
        dataManagerProxyConfiguration.put(
            ConfigurableProperty.ConnectionFactory.qualifiedName(),
            connectionFactory
        );
        dataManagerProxyConfiguration.put(
            ConfigurableProperty.PersistenceManagerFactoryClass.qualifiedName(),
            EntityManagerProxyFactory_2.class.getName()
        );    
        PersistenceManagerFactory outboundConnectionFactory = JDOHelper.getPersistenceManagerFactory(
            dataManagerProxyConfiguration
        );

        Map<String,Object> entityManagerConfiguration = new HashMap<String,Object>();
        entityManagerConfiguration.put(
            ConfigurableProperty.ConnectionFactory.qualifiedName(),
            outboundConnectionFactory
        );
        entityManagerConfiguration.put(
            ConfigurableProperty.PersistenceManagerFactoryClass.qualifiedName(),
            EntityManagerFactory_1.class.getName()
        );    
        return JDOHelper.getPersistenceManagerFactory(entityManagerConfiguration);        
    }
    
    //-----------------------------------------------------------------------
    public static PersistenceManagerFactory getPersistenceManagerFactoryProxy(
    	String contextName
    ) throws NamingException, ServiceException {    	
    	// Data manager
        Map<String,Object> dataManagerConfiguration = new HashMap<String,Object>();
        dataManagerConfiguration.put(
            Constants.PROPERTY_CONNECTION_FACTORY_NAME,
            contextName == null ? "java:comp/env/ejb/EntityManagerFactory" : contextName
        );
        dataManagerConfiguration.put(
            Constants.PROPERTY_PERSISTENCE_MANAGER_FACTORY_CLASS,
            DataManager_2ProxyFactory.class.getName()
        );
        PersistenceManagerFactory dataManagerFactory = JDOHelper.getPersistenceManagerFactory(
        	dataManagerConfiguration
        );        
        // Entity manager
    	Map<String,Object> entityManagerConfiguration = new HashMap<String,Object>();
    	entityManagerConfiguration.put(
        	ConfigurableProperty.ConnectionFactory.qualifiedName(),
        	dataManagerFactory
        );
        entityManagerConfiguration.put(
        	ConfigurableProperty.PersistenceManagerFactoryClass.qualifiedName(),
        	EntityManagerFactory_1.class.getName()
        );        
        return JDOHelper.getPersistenceManagerFactory(entityManagerConfiguration);
    }

    //-----------------------------------------------------------------------
    public static javax.jmi.reflect.RefPackage getJmiPackage(
        PersistenceManager pm,
        String authorityXri
    ) {
    	Authority obj = pm.getObjectById(
            Authority.class,
            authorityXri
        );  
    	return obj.refOutermostPackage().refPackage(obj.refGetPath().getBase());
    }

    //-----------------------------------------------------------------------
    public static org.opencrx.kernel.workflow1.jmi1.Workflow1Package getWorkflowPackage(
        PersistenceManager pm
    ) {
        return (org.opencrx.kernel.workflow1.jmi1.Workflow1Package)getJmiPackage(
        	pm,
            org.opencrx.kernel.workflow1.jmi1.Workflow1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static org.opencrx.kernel.code1.jmi1.Code1Package getCodePackage(
        PersistenceManager pm
    ) {
    	return (org.opencrx.kernel.code1.jmi1.Code1Package)getJmiPackage(
    		pm,
            org.opencrx.kernel.code1.jmi1.Code1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static org.opencrx.kernel.document1.jmi1.Document1Package getDocumentPackage(
        PersistenceManager pm
    ) {
        return (org.opencrx.kernel.document1.jmi1.Document1Package)getJmiPackage(
        	pm,
            org.opencrx.kernel.document1.jmi1.Document1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static Admin1Package getAdminPackage(
        PersistenceManager pm
    ) {
        return (Admin1Package)getJmiPackage(
        	pm,
            Admin1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static org.openmdx.base.jmi1.BasePackage getOpenMdxBasePackage(
        PersistenceManager pm
    ) {
        return (org.openmdx.base.jmi1.BasePackage)getJmiPackage(
        	pm,
        	org.openmdx.base.jmi1.BasePackage .AUTHORITY_XRI
         );
    }

    //-----------------------------------------------------------------------
    public static Home1Package getHomePackage(
        PersistenceManager pm
    ) {
        return (Home1Package)getJmiPackage(
        	pm,
        	Home1Package.AUTHORITY_XRI
         );            
    }

    //-----------------------------------------------------------------------
    public static Contract1Package getContractPackage(
        PersistenceManager pm
    ) {
    	return (Contract1Package)getJmiPackage(
    		pm,
    		Contract1Package.AUTHORITY_XRI
    	);            
    }

    //-----------------------------------------------------------------------
    public static Depot1Package getDepotPackage(
        PersistenceManager pm
    ) {
        return (Depot1Package)getJmiPackage(
        	pm,
        	Depot1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static Building1Package getBuildingPackage(
        PersistenceManager pm
    ) {
        return (Building1Package)getJmiPackage(
        	pm,
        	Building1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static Product1Package getProductPackage(
        PersistenceManager pm
    ) {
        return (Product1Package)getJmiPackage(
        	pm,
        	Product1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static Uom1Package getUomPackage(
        PersistenceManager pm
    ) {
    	return (Uom1Package)getJmiPackage(
    		pm,
    		Uom1Package.AUTHORITY_XRI
    	);
    }

    //-----------------------------------------------------------------------
    public static Realm1Package getRealmPackage(
        PersistenceManager pm
    ) {
        return (Realm1Package)getJmiPackage(
        	pm,
        	Realm1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static org.opencrx.kernel.base.jmi1.BasePackage getBasePackage(
        PersistenceManager pm
    ) {
        return (org.opencrx.kernel.base.jmi1.BasePackage)getJmiPackage(
        	pm,
        	org.opencrx.kernel.base.jmi1.BasePackage.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static Activity1Package getActivityPackage(
        PersistenceManager pm
    ) {
        return (Activity1Package)getJmiPackage(
        	pm,
        	Activity1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static Account1Package getAccountPackage(
        PersistenceManager pm
    ) {
        return (Account1Package)getJmiPackage(
        	pm,
        	Account1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static Forecast1Package getForecastPackage(
        PersistenceManager pm
    ) {
        return (Forecast1Package)getJmiPackage(
        	pm,
        	Forecast1Package.AUTHORITY_XRI
        );
    }

    //-----------------------------------------------------------------------
    public static GenericPackage getGenericPackage(
        PersistenceManager pm
    ) {
        return (GenericPackage)getJmiPackage(
        	pm,
        	GenericPackage.AUTHORITY_XRI
        );
    }

    //-------------------------------------------------------------------------
    public static String toFilename(
        String s
    ) {
        s = s.replace(' ', '-');
        s = s.replace(',', '-');
        s = s.replace('/', '-');
        s = s.replace('\\', '-');
        s = s.replace('=', '-');
        s = s.replace('%', '-');
        s = s.replace(':', '-');
        s = s.replace('*', '-');
        s = s.replace('?', '-');
        s = s.replace('+', '-');
        s = s.replace('(', '-');
        s = s.replace(')', '-');
        s = s.replace('<', '-');
        s = s.replace('>', '-');
        s = s.replace('|', '-');
        s = s.replace('"', '-');
        s = s.replace('\'', '-');
        s = s.replace('&', '-');
        s = s.replace('.', '-');
        s = s.replace('#', '-');
        s = s.replace("-", "");
        if(s.length() > 50) {
            s = s.substring(0, 44) + s.substring(s.length()-5);
        }
        return s;
    }
        
    //-----------------------------------------------------------------------
    public static String getPasswordDigest(
        String password,
        String algorithm
    ) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(password.getBytes("UTF-8"));
            return "{" + algorithm + "}" + org.openmdx.base.text.conversion.Base64.encode(md.digest());
        }
        catch(NoSuchAlgorithmException e) {
        }
        catch(UnsupportedEncodingException e) {
        }
        return null;
    }
    
    //-------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public static boolean areEqual(
        Object v1,
        Object v2
    ) {
        if(v1 == null) return v2 == null;
        if(v2 == null) return v1 == null;
        if(
            (v1 instanceof Comparable) && 
            (v2 instanceof Comparable) &&
            (v1.getClass().equals(v2.getClass()))
        ) {
            return ((Comparable<Object>)v1).compareTo(v2) == 0;
        }
        return v1.equals(v2);
    }

    //-------------------------------------------------------------------------
    public static BigDecimal getUomScaleFactor(
        org.opencrx.kernel.uom1.jmi1.Uom from,
        org.opencrx.kernel.uom1.jmi1.Uom to
    ) {
        if(from == null || to == null) {
            return BigDecimal.ZERO;
        }
        else if(from.refMofId().equals(to.refMofId())) {
            return BigDecimal.ONE;
        }
        else if(
            (from.getBaseUom() != null) && 
            from.getBaseUom().refMofId().equals(to.refMofId())
        ) {
            return from.getQuantity() != null ? 
            	from.getQuantity() : 
            	BigDecimal.ZERO;
        }
        else if(
            (to.getBaseUom() != null) && 
            to.getBaseUom().refMofId().equals(from.refMofId())
        ) {
            return (to.getQuantity() != null) && (to.getQuantity().signum() != 0) ? 
            	new BigDecimal(1.0 / to.getQuantity().doubleValue()) : 
            	BigDecimal.ZERO;
        }
        else if(
            (from.getBaseUom() != null) && 
            (to.getBaseUom() != null) && 
            from.getBaseUom().refMofId().equals(to.getBaseUom().refMofId())
        ) {
            return (from.getQuantity() != null) && (to.getQuantity() != null) && (to.getQuantity().signum() != 0) ? 
            	new BigDecimal(from.getQuantity().doubleValue() / to.getQuantity().doubleValue()) : 
            	BigDecimal.ZERO;
        }
        else {
            return BigDecimal.ZERO;
        }
    }

    //-------------------------------------------------------------------------
    public static SimpleDateFormat getLocalizedDateFormat(
    	UserHome userHome
    ) {
    	// User settings
    	Properties userSettings = new Properties();
		try {
	    	if(userHome != null && userHome.getSettings() != null) {
	    		userSettings.load(
	    			new ByteArrayInputStream(
	    				userHome.getSettings().getBytes("UTF-8")
	    			)
	    	    );
	    	}
		}
		catch(Exception e) {}
		// Locale
		Locale userLocale = Locale.getDefault();
		if(userSettings.getProperty(UserSettings.LOCALE_NAME.getName()) != null) {
			String localeAsString = userSettings.getProperty(UserSettings.LOCALE_NAME.getName()); 
	        userLocale = new Locale(
	        	localeAsString.substring(0, 2), 
	        	localeAsString.substring(localeAsString.indexOf("_") + 1)
	        );
		}		
    	SimpleDateFormat dateTimeFormat = (SimpleDateFormat)SimpleDateFormat.getDateTimeInstance(
        	java.text.DateFormat.SHORT,
        	java.text.DateFormat.MEDIUM,
        	userLocale
        );
        DateValue.assert4DigitYear(dateTimeFormat);
        // TimeZone
		if(userSettings.getProperty(UserSettings.TIMEZONE_NAME.getName()) != null) {
			try {
	    		dateTimeFormat.setTimeZone(
	    			TimeZone.getTimeZone(
	    				userSettings.getProperty(UserSettings.TIMEZONE_NAME.getName())
	    			)
	            );
    		}
	    	catch(Exception e) {}
    	}
    	return dateTimeFormat;
    }
    	
    //-------------------------------------------------------------------------
    public interface TraverseObjectTreeCallback {
    
    	public Object visit(
    		RefObject_1_0 object,
    		Object context
    	) throws ServiceException;
    	
    }
    
    //-------------------------------------------------------------------------
    public static String getUidAsString(
    ) {
        return UUIDConversion.toUID(UUIDs.newUUID());        
    }

    //-------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    public static Object traverseObjectTree(
    	RefObject_1_0 object,
        Set<String> referenceFilter,
        TraverseObjectTreeCallback callback,
        Object context
    ) throws ServiceException {
    	Object newContext = callback.visit(
    		object,
    		context
    	);
        Model_1_0 model = Model_1Factory.getModel();
        @SuppressWarnings("rawtypes")
        Map<String,ModelElement_1_0> references = (Map)model.getElement(
        	object.refClass().refMofId()
        ).objGetValue("reference");
        for(ModelElement_1_0 featureDef: references.values()) {
            ModelElement_1_0 referencedEnd = model.getElement(
                featureDef.objGetValue("referencedEnd")
            );
            boolean referenceIsCompositeAndChangeable = 
                model.isReferenceType(featureDef) &&
                AggregationKind.COMPOSITE.equals(referencedEnd.objGetValue("aggregation")) &&
                ((Boolean)referencedEnd.objGetValue("isChangeable")).booleanValue();
            boolean referenceIsSharedAndChangeable = 
                model.isReferenceType(featureDef) &&
                AggregationKind.SHARED.equals(referencedEnd.objGetValue("aggregation")) &&
                ((Boolean)referencedEnd.objGetValue("isChangeable")).booleanValue();            
            // Only navigate changeable references which are either 'composite' or 'shared'
            // Do not navigate references with aggregation 'none'.
            if(referenceIsCompositeAndChangeable || referenceIsSharedAndChangeable) {
                String referenceName = (String)featureDef.objGetValue("name");
                boolean matches = referenceFilter == null;
                if(!matches) {
                    String qualifiedReferenceName = (String)featureDef.objGetValue("qualifiedName");
                    matches =
                        referenceFilter.contains(referenceName) ||
                        referenceFilter.contains(qualifiedReferenceName);
                }
                if(matches) {   
                	@SuppressWarnings("rawtypes")
                    List<?> content = ((RefContainer)object.refGetValue(referenceName)).refGetAll(null);
                    for(Object contained: content) {
                        traverseObjectTree(
                            (RefObject_1_0)contained,
                            referenceFilter,
                            callback,
                            newContext
                        );
                    }
                }
            }
        }	
        return newContext;
    }

    //-----------------------------------------------------------------------
    public static org.openmdx.security.realm1.jmi1.Principal getRequestingPrincipal(
    	PersistenceManager pm,
    	String providerName,
    	String segmentName
    ) {
    	List<String> principalChain = UserObjects.getPrincipalChain(pm);
    	if(principalChain.isEmpty()) return null;
    	return (org.openmdx.security.realm1.jmi1.Principal)pm.getObjectById(
			new Path("xri://@openmdx*org.openmdx.security.realm1").getDescendant("provider", providerName, "segment", "Root", "realm", segmentName, "principal", principalChain.get(0))
        );
    }
    
    //-----------------------------------------------------------------------
    public static boolean principalIsMemberOf(
    	org.openmdx.security.realm1.jmi1.Principal principal,
    	String... principalGroups
    ) {
    	if(principal != null && principalGroups != null) {
	    	List<String> groups = Arrays.asList(principalGroups);
	    	Collection<org.openmdx.security.realm1.jmi1.Group> memberships = principal.getIsMemberOf();
	    	for(org.openmdx.security.realm1.jmi1.Group membership: memberships) {
	    		if(groups.contains(membership.getName())) {
	    			return true;
	    		}
	    	}
    	}
    	return false;
    }

    //-------------------------------------------------------------------------
    public static List<String> getPermissions(
    	org.openmdx.security.realm1.jmi1.Principal principal,
    	String action
    ) throws ServiceException {
    	List<String> permissions = new ArrayList<String>();
    	if(principal instanceof org.opencrx.security.realm1.jmi1.Principal) {
			List<Role> roles = ((org.opencrx.security.realm1.jmi1.Principal)principal).getGrantedRole();
			for(Role role: roles) {
				for(Permission permission: role.<Permission>getPermission()) {
					if(permission.getAction().contains(action)) {
						permissions.add(permission.getName());
					}
				}
			}
    	}
		return permissions;
    }
    
    /**
     *  Checks whether there exists a permission matching the pattern
     *  'object:authority/object path@runAsPrincipal'
     * @param objectIdentity
     * @param runAsPermissions
     * @return
     */
    public static boolean hasObjectRunAsPermission(
    	Path objectIdentity,
    	List<String> runAsPermissions
    ) {
    	for(String runAsPermission: runAsPermissions) {
    		if(runAsPermission.startsWith("object:")) {
    			String[] components = runAsPermission.substring(7, runAsPermission.indexOf("@")).split("/");
    			if(components.length > 0) {
    				Path pattern = new Path("xri://@openmdx*" + components[0]).getDescendant("provider", objectIdentity.get(2), "segment", objectIdentity.get(4));
    				for(int j = 1; j < components.length; j++) {
    					pattern = pattern.getDescendant(components[j]);
    				}
            		if(objectIdentity.isLike(pattern)) {
            			return true;
            		}
    			}
    		}
    	}
    	return false;
    }

    /**
     * Return a localized dateTime formatter.
     * @param language
     * @return
     */
    public static DateFormat getTimeFormat(
        String language
    ) {
        Map<String,DateFormat> timeFormatters = cachedTimeFormat.get();
        DateFormat timeFormat = timeFormatters.get(language);
        if(timeFormat == null) {
            timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, new Locale(language)); 
            timeFormatters.put(
                language,
                timeFormat
            );
        }
        return timeFormat; 
    }
    
    /**
     * Return a localized decimal formatter.
     * @param language
     * @return
     */
    public static DecimalFormat getDecimalFormat(
        String language
    ) {
        Map<String,DecimalFormat> decimalFormatters = cachedDecimalFormat.get();
        DecimalFormat decimalFormat = decimalFormatters.get(language);
        if(decimalFormat == null) {
            decimalFormat = (DecimalFormat)DecimalFormat.getInstance(new Locale(language)); 
            decimalFormatters.put(
                language,
                decimalFormat
            );
        }
        return decimalFormat; 
    }

    /**
     * Return a localized date formatter.
     * @param language
     * @return
     */
    public static DateFormat getDateFormat(
        String language
    ) {
        Map<String,DateFormat> dateFormatters = cachedDateFormat.get();
        DateFormat dateFormat = dateFormatters.get(language);
        if(dateFormat == null) {
            dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, new Locale(language));
            dateFormatters.put(
                language,
                dateFormat
            );
        }
        return dateFormat;
    }
        
    /**
     * Normalize new lines in given string. Patterns of the form (\r\n|\r|\n)
     * are replaced by \n
     * @param s
     * @return
     */
    public static String normalizeNewLines(
    	String s
    ) {
    	Matcher m = CRLF.matcher(s);
    	if(m.find()) {
    		return m.replaceAll("\\\n");
    	} else {
    		return s;
    	}
    }

    //-------------------------------------------------------------------------
    // Members
	//-------------------------------------------------------------------------
    private static Pattern CRLF = Pattern.compile("(\r\n|\r|\n)");

    private static ThreadLocal<Map<String,DateFormat>> cachedDateFormat = new ThreadLocal<Map<String,DateFormat>>() {
        protected synchronized Map<String,DateFormat> initialValue() {
            return new HashMap<String,DateFormat>();
        }
    };
    private static ThreadLocal<Map<String,DateFormat>> cachedTimeFormat = new ThreadLocal<Map<String,DateFormat>>() {
        protected synchronized Map<String,DateFormat> initialValue() {
            return new HashMap<String,DateFormat>();
        }
    };
    private static ThreadLocal<Map<String,DecimalFormat>> cachedDecimalFormat = new ThreadLocal<Map<String,DecimalFormat>>() {
        protected synchronized Map<String,DecimalFormat> initialValue() {
            return new HashMap<String,DecimalFormat>();
        }
    };
    
}

//--- End of File -----------------------------------------------------------
