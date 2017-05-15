/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.okta.ldapbridge;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sundarganesan
 */
public final class HTTPUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPUtil.class);
    static String oktaApiKey = "";
    private static final HTTPUtil onlyInstance = new HTTPUtil();

    public static HTTPUtil getInstance () {
        return onlyInstance;
    }
    private HTTPUtil() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            EncryptionUtil encUtilInstance = EncryptionUtil.getInstance();
            input = LDAPUtil.class.getResourceAsStream("/OktaLDAPBridgeConfig.properties");

            // load a properties file
            prop.load(input);

            oktaApiKey = encUtilInstance.decryptAES(prop.getProperty("oktaApiKey"));
            
            //Remove this log after testing
            //LOGGER.debug("Decrypted Okta API Key : "+oktaApiKey);
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

    public RetObj get(String resource) {
        LOGGER.debug("Working on GET : "+resource);
        RetObj ret = new RetObj();
        try {
            URL url = new URL(resource);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
  
            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            conn.setRequestProperty("Authorization", "SSWS " + oktaApiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("GET");
            String line;
            ret.responseCode = conn.getResponseCode();
            if (ret.responseCode == 200) {
                LOGGER.debug("Success response received.");
                InputStream s = conn.getInputStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.output = "";
                        while ((line = rd.readLine()) != null) {
                            ret.output += line;
                        }
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading response", ioe);
                        throw ioe;
                    }
                }
            } else {
                LOGGER.error("Error response received. Code : "+ret.responseCode);
                InputStream s = conn.getInputStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.errorOutput = "";
                        while ((line = rd.readLine()) != null) {
                            ret.errorOutput += line;
                        }
                        LOGGER.error("Error returned from Okta API : ", ret.errorOutput);
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading error response", ioe);
                        throw ioe;
                    }                   
                }
            }
            String rll = conn.getHeaderField("X-Rate-Limit-Limit");
            if (rll != null && rll.length() > 0) {
                try {
                    ret.rateLimitLimit = Integer.parseInt(rll);
                } catch (Exception e) {
                    ret.rateLimitLimit = 1200;
                }
            }
        } catch (IOException e) {
            ret.exception = e.getMessage();
            LOGGER.error("IOException calling Okta API : ", e);
        }
        LOGGER.debug("Returning from GET : "+resource);
        return ret;
    }
	
        
    public RetObj post(String resource, String data) {
        LOGGER.debug("Working on POST : "+resource+" with payload : "+data);
        RetObj ret = new RetObj();
        try {
            URL url = new URL(resource);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
       
            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "SSWS " + oktaApiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("POST");
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            String line;
            ret.responseCode = conn.getResponseCode();
            if (ret.responseCode == 201 || ret.responseCode == 200) {
                LOGGER.debug("Success response received.");
                InputStream s = conn.getInputStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.output = "";
                        while ((line = rd.readLine()) != null) {
                            ret.output += line;
                        }
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading response", ioe);
                        throw ioe;
                    }
                }
            } else {
                LOGGER.error("Error response received. Code : "+ret.responseCode);
                InputStream s = conn.getErrorStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.errorOutput = "";
                        while ((line = rd.readLine()) != null) {
                            ret.errorOutput += line;
                        }
                        LOGGER.error("Error returned from Okta API : ", ret.errorOutput);
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading error response", ioe);
                        throw ioe;
                    }                   
                }
            }
            String rll = conn.getHeaderField("X-Rate-Limit-Limit");
            if (rll != null && rll.length() > 0) {
                try {
                    ret.rateLimitLimit = Integer.parseInt(rll);
                } catch (Exception e) {
                    ret.rateLimitLimit = 1200;
                }
            }
        } catch (IOException e) {
            ret.exception = e.getMessage();
            LOGGER.error("IOException calling Okta API : ", e);
        }
        LOGGER.debug("Returning from POST : "+resource);
        return ret;
    }
        
        
    public RetObj put(String resource, String data) {
        LOGGER.debug("Working on PUT : "+resource+" with payload : "+data);
        RetObj ret = new RetObj();
        try {
            URL url = new URL(resource);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
       
            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "SSWS " + oktaApiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("PUT");
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            String line;
            ret.responseCode = conn.getResponseCode();
            if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
                LOGGER.debug("Success response received.");
                InputStream s = conn.getInputStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.output = "";
                        while ((line = rd.readLine()) != null) {
                            ret.output += line;
                        }
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading response", ioe);
                        throw ioe;
                    }
                }
            } else {
                LOGGER.error("Error response received. Code : "+ret.responseCode);
                InputStream s = conn.getErrorStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.errorOutput = "";
                        while ((line = rd.readLine()) != null) {
                            ret.errorOutput += line;
                        }
                        LOGGER.error("Error returned from Okta API : ", ret.errorOutput);
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading error response", ioe);
                        throw ioe;
                    }                   
                }
            }
            String rll = conn.getHeaderField("X-Rate-Limit-Limit");
            if (rll != null && rll.length() > 0) {
                try {
                    ret.rateLimitLimit = Integer.parseInt(rll);
                } catch (Exception e) {
                    ret.rateLimitLimit = 1200;
                }
            }
        } catch (IOException e) {
            ret.exception = e.getMessage();
            LOGGER.error("IOException calling Okta API : ", e);
        }
        LOGGER.debug("Returning from PUT : "+resource);
        return ret;
    }
        
    public RetObj delete(String resource, String data) {
        LOGGER.debug("Working on DELETE : "+resource+" with payload : "+data);
        RetObj ret = new RetObj();
        try {
            URL url = new URL(resource);
            HttpURLConnection conn;
     
                conn = (HttpURLConnection) url.openConnection();
       
            conn.setConnectTimeout(1000000);
            conn.setReadTimeout(1000000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "SSWS " + oktaApiKey);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("DELETE");
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            String line;
            ret.responseCode = conn.getResponseCode();
            if (ret.responseCode == 201 || ret.responseCode == 200 || ret.responseCode == 204) {
                LOGGER.debug("Success response received.");
                InputStream s = conn.getInputStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.output = "";
                        while ((line = rd.readLine()) != null) {
                            ret.output += line;
                        }
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading response", ioe);
                        throw ioe;
                    }
                }
            } else {
                LOGGER.error("Error response received. Code : "+ret.responseCode);
                InputStream s = conn.getErrorStream();
                if (s != null) {
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(s));
                        ret.errorOutput = "";
                        while ((line = rd.readLine()) != null) {
                            ret.errorOutput += line;
                        }
                        LOGGER.error("Error returned from Okta API : ", ret.errorOutput);
                    }
                    catch(IOException ioe)
                    {
                        LOGGER.error("Error while reading error response", ioe);
                        throw ioe;
                    }                   
                }
            }
            String rll = conn.getHeaderField("X-Rate-Limit-Limit");
            if (rll != null && rll.length() > 0) {
                try {
                    ret.rateLimitLimit = Integer.parseInt(rll);
                } catch (Exception e) {
                    ret.rateLimitLimit = 1200;
                }
            }
        } catch (IOException e) {
            ret.exception = e.getMessage();
            LOGGER.error("IOException calling Okta API : ", e);
        }
        LOGGER.debug("Returning from DELETE : "+resource);
        return ret;
    }
}