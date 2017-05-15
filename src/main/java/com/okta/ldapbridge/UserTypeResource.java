/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.okta.ldapbridge;

import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.json.*;

import java.text.MessageFormat;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * REST Web Service
 *
 * @author sundarganesan
 */
@Component
@Path("OktaLDAPBridge/api/v1/UserType/{userName}")
public class UserTypeResource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserTypeResource.class);
    private LDAPUtil myLDAPUtil = null;
    
    String searchFilter = "(uid={0})";
    /**
     * Creates a new instance of UserTypeResource
     */
    public UserTypeResource() {
    	myLDAPUtil = LDAPUtil.getInstance();
    }

    /**
     * Retrieves representation of an instance of com.okta.oktaldapbridge.UserTypeResource
     * @param userName
     * @return an instance of java.lang.String
     */
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJson(@PathParam("userName") String userName) {
        
        LOGGER.debug("Looking for : " + userName);
        if(userName == null || userName.trim().length() == 0) {
            LOGGER.error("Empty or missing userName passed.");
            return Response.status(Response.Status.BAD_REQUEST).entity(myLDAPUtil.buildJSONError("UserName cannot be empty")).build();
        }
        searchFilter = MessageFormat.format(searchFilter,userName);
        String result = new String();
        try {
            result = LDAPUtil.queryLDAP(searchFilter);
        } catch (Exception e) {
            LOGGER.error("Exception querying LDAP", e);
            return Response.status(Response.Status.NOT_FOUND).entity(myLDAPUtil.buildJSONError("User not found in LDAP for userName: " + userName)).build();
        }
        // Build response object
        JSONObject resp = new JSONObject();
        resp.put("status", "SUCCESS");
        JSONObject uClass = new JSONObject(result);
        String uStr = uClass.getString("jdUserClass");
        resp.put("userType", uStr);
        
        return Response.status(Response.Status.OK).entity(resp.toString()).build();
    }
}
