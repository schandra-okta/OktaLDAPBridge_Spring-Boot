/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.okta.ldapbridge;

/**
 *
 * @author schandra
 */
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;  
import org.springframework.core.annotation.Order;  
import org.springframework.stereotype.Component;  

@Component  
@Order(Ordered.HIGHEST_PRECEDENCE)  
public class AuthorizationRequestFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationRequestFilter.class);
    private static String secretPassphrase = new String();
    private static final EncryptionUtil encUtilInstance = EncryptionUtil.getInstance();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    	LOGGER.debug("Initializing AuthorizationRequestFilter...");
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = AuthorizationRequestFilter.class.getResourceAsStream("/OktaLDAPBridgeConfig.properties");
            prop.load(input);
            secretPassphrase = prop.getProperty("secretPassphrase");
        } catch (IOException ex) {
            LOGGER.error("Error initializing AuthorizationRequestFilter", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error("Error initializing AuthorizationRequestFilter while closing properties file stream", e);
                }
            }
        }
    }

    @Override
    public void destroy() {
    	LOGGER.debug("Destroying AuthorizationRequestFilter...");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest  = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
    	String authzHeader = httpRequest.getHeader("Authorization");
    	LOGGER.debug("Authorization header is : "+authzHeader);
        
        if(null==authzHeader||!(encUtilInstance.decryptAES(authzHeader).equals(secretPassphrase)))
        {
        	LOGGER.debug("Failed matching authorization header to expected value...");
        	LOGGER.debug("Cannot proceed with request - sending failure response...");
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        	httpResponse.setContentType("application/json");
            PrintWriter out = httpResponse.getWriter();
            JSONObject json = new JSONObject();
            json.put("status", "FAILURE");
            json.put("details", "Missing or bad credentials");
            out.println(json.toString());
            out.close();         
        } else{
        	LOGGER.debug("Successfully authorized - moving on with further processing...");      	
        	chain.doFilter(request,response);
        }
    }
}