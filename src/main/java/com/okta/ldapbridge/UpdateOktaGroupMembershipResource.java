/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.okta.ldapbridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * REST Web Service
 *
 * @author sundarganesan
 */

@Component
@Path("OktaLDAPBridge/api/v1/UpdateOktaGroupMembership")
public class UpdateOktaGroupMembershipResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOktaGroupMembershipResource.class);

    private String oktaAPIUrlPrefix = new String();
    private String oktaSearchUserUrl = new String();
    private String oktaGetGroupMembershipUrl = new String();
    private String oktaSearchGroupUrl = new String();
    private String oktaCreateGroupUrl = new String();
    private String oktaAddDeleteUserToGroupUrl = new String();
    private boolean oktaNewLDAPAgentAvailable = false;
    private String oktaJDMemberAttrName = new String();
    private HTTPUtil myHTTPUtil = null;
    private LDAPUtil myLDAPUtil = null;
    
    @Context
    private UriInfo context;

    /**
     * Creates a new instance of UpdateOktaGroupMembershipResource
     */
    public UpdateOktaGroupMembershipResource() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = LDAPUtil.class.getResourceAsStream("/OktaLDAPBridgeConfig.properties");

            // load a properties file
            prop.load(input);

            oktaAPIUrlPrefix = prop.getProperty("oktaAPIUrlPrefix");
            oktaSearchUserUrl = oktaAPIUrlPrefix+"/users/";
            oktaSearchGroupUrl = oktaAPIUrlPrefix+"/groups?q=";
            oktaCreateGroupUrl = oktaAPIUrlPrefix+"/groups";
            oktaAddDeleteUserToGroupUrl = oktaAPIUrlPrefix+"/groups/{0}/users/{1}";
            oktaGetGroupMembershipUrl = oktaAPIUrlPrefix+"/users/{0}/groups";

            oktaNewLDAPAgentAvailable = Boolean.parseBoolean(prop.getProperty("oktaNewLDAPAgentAvailable", "false"));
            oktaJDMemberAttrName = prop.getProperty("oktaJDMemberAttrName", "jdMember");
            myHTTPUtil = HTTPUtil.getInstance();
            myLDAPUtil = LDAPUtil.getInstance();
        } catch (IOException ex) {
            LOGGER.error("Error during reading config file : ", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error("Error closing config file handle : ", e);
                }
            }
        }
    }

    /**
     * POST method for updating or creating an instance of UpdateOktaGroupMembershipResource
     * @param content representation for the resource
     * @return 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postJson(String content) {
        
        //Read username from content 
        String userName = new JSONObject(content).getString("userName");
        LOGGER.debug("Receive update groups call for : " + userName);
        if(userName == null || userName.trim().length() == 0) {
            LOGGER.error("Empty or missing userName passed.");
            return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("userName cannot be empty")).build();
        }
       
        String ldapObjStr = new String();
        String oktaObjStr = new String();
        String oktaGroupMemStr = new String();
        RetObj ret = new RetObj();

        try {
            //Query LDAP for username and get JDMember attributes of the entry
            ldapObjStr = LDAPUtil.queryLDAP("(uid="+userName + ")");
            LOGGER.debug("ldapObjStr : " + ldapObjStr);
        } catch (Exception ex) {
            LOGGER.error(null, ex);
            return Response.status(Response.Status.NOT_FOUND).entity(myLDAPUtil.buildJSONError("User not found in LDAP for userName: " + userName)).build();
        }  
            
        //Query Okta for User with username in content
        String uUrl = oktaSearchUserUrl;
        
        try {
            uUrl = uUrl + URLEncoder.encode(userName, "UTF-8");
            LOGGER.debug("Calling Okta Users API GET : " + uUrl);
            ret = myHTTPUtil.get(uUrl);
        } catch (UnsupportedEncodingException ex) {
            LOGGER.error(null, ex);
        }
        LOGGER.debug("Return from GET : " + ret.output);
        
        oktaObjStr = ret.output;
        
        String uUid = new String();
        //Extract UID from Okta's response
        if(oktaObjStr!= null) {
            JSONObject jObj = new JSONObject(oktaObjStr);
            uUid = jObj.getString("id");
            LOGGER.debug("Found user with uUid " + uUid);
        }
        else
        {
            LOGGER.error("No matching user found in Okta");
            return Response.status(Response.Status.NOT_FOUND).entity(myLDAPUtil.buildJSONError("User not found in Okta for userName: " + userName)).build();        	
        }
        //Query Okta for user's group membership
        String gUrl = MessageFormat.format(oktaGetGroupMembershipUrl, uUid);
        
        LOGGER.debug("Calling Okta Groups API GET : " + gUrl);      
        ret = myHTTPUtil.get(gUrl);       
        LOGGER.debug("Return from GET : " + ret.output);
        
        oktaGroupMemStr = ret.output;
        
        //Compare user's current group membership with JDMember groups
        JSONObject lObj = new JSONObject(ldapObjStr);
        String jdMemberListStr = (String)lObj.get("JDMember");
        LOGGER.debug("JDMember value from LDAP : " + jdMemberListStr);
        List li = Arrays.asList(jdMemberListStr.split(","));
        int ctr = 0;
        while (ctr < li.size()) {
            // remove whitespaces in the Group names
            li.set(ctr, ((String)li.get(ctr)).trim());
            ctr++;
        }
                 
        Set<String> jdMemberListSet = new HashSet<String>(li);
        JSONObject uObj = new JSONObject(oktaObjStr);       
        JSONArray jArr = new JSONArray(oktaGroupMemStr);
        
        LOGGER.debug("Length of JSON Array for Current Okta Groups : " + jArr.length());
        int i = 0;
        Set<String> userInGroupsSet = new HashSet<String>();
        
        Iterator<Object> itr = jArr.iterator();
        while(itr.hasNext()) {
            JSONObject temp = (JSONObject)itr.next();
            LOGGER.debug("Group " + i + " JSON : " + temp.toString());
            LOGGER.debug("Group id : " + temp.get("id"));
            LOGGER.debug("Group profile JSON : " + temp.get("profile"));
            JSONObject pro = temp.getJSONObject("profile");
            String gname = (String)pro.get("name");
            LOGGER.debug("Group name : " + gname);
            String type = (String)temp.get("type");
            LOGGER.debug("Group type : " + temp.get("type"));
            if (gname.equals("Everyone") || gname.toLowerCase().startsWith("okta_")) {
                 LOGGER.debug("Skipping group!!!!!");  

            } else {
                userInGroupsSet.add(gname.trim());
            }
        }
          
        LOGGER.debug("jdMemberListSet " + jdMemberListSet.toString());
        
        LOGGER.debug("userInGroupsSet " + userInGroupsSet.toString());
        
        Set<String> s1 = new HashSet<String>(jdMemberListSet);
        Set<String> s2 = new HashSet<String>(userInGroupsSet);
        
        s1.removeAll(userInGroupsSet);
        s2.removeAll(jdMemberListSet);
        
        LOGGER.debug("Groups to be added to user : " + s1.toString());
        LOGGER.debug("Groups to be removed from user : " + s2.toString());
        
        Iterator<String> s1itr = s1.iterator();
            
        while(s1itr.hasNext()) {
            String g = s1itr.next();
            g=g.trim();
            String gid = getGroupId(g);
            if(gid.contains("NOT FOUND")) {
                LOGGER.debug("Need to create Group : "+g); 
                gUrl = oktaCreateGroupUrl;
                JSONObject jobj = new JSONObject();
                JSONObject jpro = new JSONObject();
                jpro.put("name", g);
                jpro.put("description", g);
                jobj.put("profile", jpro);
                RetObj retAns = myHTTPUtil.post(gUrl, jobj.toString());
                if(retAns.responseCode != 200) {
                    LOGGER.debug("Error creating group");
                    return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("Runtime Exception: Unable to create required Group: "+g)).build();    
                } else {
                    LOGGER.debug(retAns.output);
                    int start = retAns.output.indexOf("id");
                    int end = retAns.output.indexOf("created");
                    String temp = retAns.output.substring(start+5, end-3);
                    LOGGER.debug("Successfully created Group : "+g); 
                    
                    gid = temp;
                    
                    String tUrl = MessageFormat.format(oktaAddDeleteUserToGroupUrl, gid, uUid);
                    RetObj reto2 = myHTTPUtil.put(tUrl, "");
                    if (reto2.responseCode == 204) {
                        LOGGER.debug("Adding user " + uUid + " to group " + gid + " COMPLETE");
                    } else {
                        LOGGER.error("Adding user " + uUid + " to group " + gid + " FAILED!!!!!!");
                        return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("Runtime Exception: Unable to add user to Group")).build();    
                    }
                }
            } else if(gid.contains("MULTIPLE")) {
                LOGGER.error("Multiple groups with same name found. Returning failure."); 
                return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("Runtime Exception: Multiple values returned from Okta for a JDMember group name. Delete all but one of these groups manually to proceed.")).build();
            } else {
                LOGGER.debug("Adding user " + uUid + " to group " + gid);
                String tUrl = MessageFormat.format(oktaAddDeleteUserToGroupUrl, gid, uUid);
                RetObj retAns2 = myHTTPUtil.put(tUrl, "");
                if (retAns2.responseCode == 204) {
                    LOGGER.debug("Adding user " + uUid + " to group " + gid + " COMPLETE");
                } else {
                    LOGGER.error("Adding user " + uUid + " to group " + gid + " FAILED!!!!!!");
                    return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("Runtime Exception: Unable to add user to Group")).build();    
                }
            }
        }
        
        Iterator<String> s2itr = s2.iterator();
        while(s2itr.hasNext()) {
            String g = s2itr.next();
            g=g.trim();
            String gid = getGroupId(g);
            if(gid.contains("MULTIPLE")) {
                LOGGER.error("Multiple groups with same name found. Returning failure."); 
                return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("Runtime Exception: Multiple values returned from Okta for a JDMember group name. Delete all but one of these groups manually to proceed.")).build();
            } else if(gid.contains("NOT FOUND")) {
            	continue;
            	//If the user is in a group to be deleted and the gid is not found,
            	//there's no membership to delete. Probably app_group with same name?
            } else {
                LOGGER.debug("Removing user " + uUid + " from group " + gid);
                String tUrl = MessageFormat.format(oktaAddDeleteUserToGroupUrl, gid, uUid);
                RetObj reto = myHTTPUtil.delete(tUrl, "");
                if (reto.responseCode == 204) {
                    LOGGER.debug("Removing user " + uUid + " to group " + gid + " COMPLETE");
                } else {
                    LOGGER.error("Removing user " + uUid + " to group " + gid + " FAILED!!!!!!");
                    return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("Runtime Exception: Unable to delete user from Group")).build();
                }
            }
        }
        
        if(!oktaNewLDAPAgentAvailable){
            //Build the JSON string to update user's Okta profile
            JSONObject oktaProfileObj = new JSONObject();
            JSONObject oktaProfileInner = new JSONObject();
            oktaProfileInner.put(oktaJDMemberAttrName, jdMemberListSet);
            oktaProfileObj.put("profile", oktaProfileInner);
            String userResourceURI = oktaAPIUrlPrefix+"/users/"+uUid;
            myHTTPUtil.post(userResourceURI, oktaProfileObj.toString());
        }
        
        // Build response object
        JSONObject resp = new JSONObject();
        resp.put("status", "SUCCESS");
        if (!oktaNewLDAPAgentAvailable)
        {
            resp.put(oktaJDMemberAttrName, jdMemberListSet);
        }
        return Response.status(Response.Status.OK).entity(resp.toString()).build();
    }
    
    private String getGroupId(String g) {
        String gid = new String();
        int count = 0;
        String gUrl = oktaSearchGroupUrl + g.trim();
        RetObj ret = myHTTPUtil.get(gUrl);
        if(ret.responseCode == 200) {
            JSONArray jArr = new JSONArray(ret.output);
            Iterator<Object> itr = jArr.iterator();
            if(jArr.length()==0) {
                LOGGER.debug("Need to create Group"); 
                gid = "NOT FOUND";
                return gid;
            } 
            if(jArr.length() > 1) {                
                LOGGER.debug("Multiple Groups returned from query, checking if we can identify only 1 as relevant"); 
                while (itr.hasNext()){
                    JSONObject temp = (JSONObject)itr.next();
                    String name = temp.getJSONObject("profile").getString("name");
                    String type = temp.getString("type");

                    if ("OKTA_GROUP".equals(type) && g.equalsIgnoreCase(name))
                    {
                        count++;
                        gid = temp.getString("id");
                        LOGGER.debug("Found a matching group. Checking to see if there are more");
                    }
                }
                LOGGER.debug("Total groups matching name found = "+count); 
                if (count > 1)
                    gid = "MULTIPLE GROUPS FOUND WITH SAME NAME";
                else if (count==0)
                    gid = "NOT FOUND";
                return gid;
            }                 
        } else {
            LOGGER.debug("Error searching for Group"); 
            gid = "ERROR";
        }
        return gid;
    }
}
