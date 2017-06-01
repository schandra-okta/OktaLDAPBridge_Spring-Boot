/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.okta.ldapbridge;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Enumeration;
import java.util.Properties;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sundarganesan
 */
public final class LDAPUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LDAPUtil.class);

    //List of attributes to searched and returned in the JSON object
    static String[] attributeFilter = null;
    static String dbuser = "";
    static String dbpassword = "";
    static String connectionString = "";
    static String searchbase = "";
    static String attributes = "";
    
    private static final LDAPUtil onlyInstance = new LDAPUtil();
    
    public static LDAPUtil getInstance () {
        return onlyInstance;
        
    }
    
    private LDAPUtil() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = LDAPUtil.class.getResourceAsStream("/OktaLDAPBridgeConfig.properties");
            EncryptionUtil encUtilInstance = EncryptionUtil.getInstance();

            // load properties file
            prop.load(input);

            dbuser = encUtilInstance.decryptAES(prop.getProperty("dbuser"));
            dbpassword = encUtilInstance.decryptAES(prop.getProperty("dbpassword"));
            connectionString = prop.getProperty("connectionString");
            searchbase = prop.getProperty("searchbase");
            attributes = prop.getProperty("attributes");
            
            //Remove below logs after testing
            //LOGGER.debug("dbuser : " + dbuser);
            //LOGGER.debug("dbpassword : " + dbpassword);
            //LOGGER.debug("connectionString : " + connectionString);
            //LOGGER.debug("searchbase : " + searchbase);
            //LOGGER.debug("attributes : " + attributes);

            attributeFilter = attributes.split(",");

        } catch (IOException ex) {
            LOGGER.error("Error reading LDAP configuration", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing config file", e);
                }
            }
        }        
    }
    
    

  public static String queryLDAP(String filter) throws Exception {
    Properties env = new Properties();

    String sp = "com.sun.jndi.ldap.LdapCtxFactory";
    env.put(Context.INITIAL_CONTEXT_FACTORY, sp);
    env.put(Context.PROVIDER_URL, connectionString);
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, dbuser);
    env.put(Context.SECURITY_CREDENTIALS, dbpassword);

    DirContext dctx = new InitialDirContext(env);

    String base = searchbase;

    SearchControls sc = new SearchControls();
    
    sc.setReturningAttributes(attributeFilter);
    sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

    NamingEnumeration results = dctx.search(base, filter, sc);
    int i = 0;
    String retStr = "";
    SearchResult sr = (SearchResult) results.next();
    i++;
    while(results.hasMore()){
         i++;
    }
    dctx.close();
    if(i==1) {
        LOGGER.debug("Found exactly one user for filter : " + filter);
        retStr = convertToJson(sr);
    } else {
        LOGGER.debug("No results found for filter : " + filter);
        throw new javax.naming.NamingException("Check username");
    }
    return retStr;
  }

  public static boolean updateLDAP(String filter, Map<String, Object> attrValmap) throws Exception {
    Properties env = new Properties();

    String sp = "com.sun.jndi.ldap.LdapCtxFactory";
    env.put(Context.INITIAL_CONTEXT_FACTORY, sp);
    env.put(Context.PROVIDER_URL, connectionString);
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, dbuser);
    env.put(Context.SECURITY_CREDENTIALS, dbpassword);

    DirContext dctx = new InitialDirContext(env);

    String base = searchbase;

    SearchControls sc = new SearchControls();
    
    sc.setReturningAttributes(new String[]{"dn"});//Don't need the rest
    sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

    NamingEnumeration results = dctx.search(base, filter, sc);
    int i = 0;
    SearchResult sr = (SearchResult) results.next();
    i++;
    while(results.hasMore()){
         i++;
    }
    if(i==1) {
        
        String dn = sr.getNameInNamespace();
        LOGGER.debug("Found exactly one user for filter : " + filter);
        LOGGER.debug("DN for user : " + dn);
        
        ModificationItem[] mods = new ModificationItem[attrValmap.keySet().size()];
        int count = 0;
        Iterator it = attrValmap.keySet().iterator();
        while (it.hasNext())
        {
            String key = it.next().toString();
            String val = attrValmap.get(key).toString();
            LOGGER.debug("Modifying attribute : " + key + " to value : "+val);
            Attribute mod = new BasicAttribute(key, val);
            mods[count++] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod);
        }

        dctx.modifyAttributes(dn, mods);

    } else {
        throw new javax.naming.NamingException("Check username");
    }
    dctx.close();
    return true;
  }
  
  public static String convertToJson(SearchResult sr) {
      JSONObject retObj = new JSONObject();
      Attributes attrs = sr.getAttributes();
      int i = 0;
      while (i<attributeFilter.length) {
            String name = attributeFilter[i];
            String value = "";
            Attribute attr = attrs.get(name);
            try {
                value = "";
                if (!name.equalsIgnoreCase("JDMember")) {
                    // No special handling for single valued attributes
                	try{
                		value = attr.get().toString();
                	}
                	catch(NullPointerException npe){
                		//Attribute doesn't have value in LDAP
                		LOGGER.warn("No value found for attribute : "+name);
                	}
                } else {
                    // Special handling is required for multi-valued attributes like JDMember
                	try{
                		Enumeration valueEnum = attr.getAll();
	                    while(valueEnum.hasMoreElements()) {
	                        if (value != "") {
	                            // Comma is used to separate multiple values
	                            value = value + ", " + valueEnum.nextElement();
	                        } else {
	                            // First element gets added directly
	                            value = (String)valueEnum.nextElement();
	                        }
	                    }
                	} catch (NullPointerException npe){
                		//Attribute doesn't have value in LDAP
                		LOGGER.warn("No value found for attribute : "+name);
                	}
                }
            } catch (NamingException ex) {
                LOGGER.error(ex + " Unable to retrieve attribute to build JSON");
                
            }
            LOGGER.debug("Attr : "+name+" has value :" + value);
            retObj.put(name, value);
            i++;
      }
      
      LOGGER.debug("Converted to JSON String : "+ retObj.toString());
      return retObj.toString();
  }

  public String buildJSONError (String message)
  {
	  JSONObject errorObj = new JSONObject();
	  errorObj.put("status", "FAILURE");
	  errorObj.put("details", message);
	  
	  return errorObj.toString();	  
  }
  
  //Reused from example code at https://www.owasp.org/index.php/Preventing_LDAP_Injection_in_Java
  //to sanitize LDAP search filters to prevent LDAP injection attacks
  public static final String escapeLDAPSearchFilter(String filter) {
      StringBuffer sb = new StringBuffer(); // If using JDK >= 1.5 consider using StringBuilder
      for (int i = 0; i < filter.length(); i++) {
          char curChar = filter.charAt(i);
          switch (curChar) {
              case '\\':
                  sb.append("\\5c");
                  break;
              case '*':
                  sb.append("\\2a");
                  break;
              case '(':
                  sb.append("\\28");
                  break;
              case ')':
                  sb.append("\\29");
                  break;
              case '\u0000': 
                  sb.append("\\00"); 
                  break;
              default:
                  sb.append(curChar);
          }
      }
      return sb.toString();
  }
  
}
