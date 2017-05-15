/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.okta.ldapbridge;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author schandra
 */
public class EncryptionUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionUtil.class);
    private static String storePass = "";
    private static String keyPass = "";
    private static String keystorePath = "";
    private static Key encKey = null;
    private static final EncryptionUtil onlyInstance = new EncryptionUtil();
        
    public static EncryptionUtil getInstance () {
        return onlyInstance;

    }
    
    private EncryptionUtil() {
        Properties prop = new Properties();
    	InputStream input = null;

        try{
            input = EncryptionUtil.class.getResourceAsStream("/OktaLDAPBridgeConfig.properties");
            prop.load(input);

            keystorePath = prop.getProperty("keystorePath");            
            storePass = prop.getProperty("storePass","changeit");
            keyPass = prop.getProperty("keyPass","changeit");
            KeyStore keyStore = KeyStore.getInstance("JCEKS");
            FileInputStream stream = new FileInputStream(keystorePath);
            keyStore.load(stream, storePass.toCharArray());
            encKey = keyStore.getKey("crypto", keyPass.toCharArray());
        }
        catch(Exception e)
        {
            LOGGER.error("Error reading in encryption details : "+e.getLocalizedMessage());
        }
    }
    
    public String decryptAES(String inputData)
    {
        String decryptedString = "Incorrect_Data";
        try{
            Cipher cipher = Cipher.getInstance("AES");
            SecretKey secretKey = new SecretKeySpec(encKey.getEncoded(), "AES");

            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(inputData.getBytes()));
            decryptedString = new String(decryptedBytes);
        }
        catch (Exception e){
            LOGGER.error("Exception decrypting the input passed in : "+e.getLocalizedMessage());
        }
        return decryptedString;
    }

}
