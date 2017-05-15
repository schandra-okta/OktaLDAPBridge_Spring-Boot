# README

1. A new JCE keystore needs to be generated to store the encryption key. Create the keystore using the following command - necessary passwords are set up during this process
```
$ keytool -genseckey -alias crypto -keyalg AES -keysize 128 -storetype jceks -keystore crypto.jks
Enter keystore password:  
Re-enter new password: 
Enter key password for <crypto>
        (RETURN if same as keystore password):  
$ 
```

2) There could be a different keystore password and key password set up.
* Add keystore location to the OktaLDAPBridgeConfig.properties file as value of property **keystorePath**.
* Also add keystore password to OktaLDAPBridgeConfig.properties as value of property **storePass**.
* Finally, add key password to OktaLDAPBridgeConfig.properties as value of property **keyPass**.

<sub>Note : Typically, 128-bit keys are used for encryption. If a larger keysize is required, you will additionally have to patch the JRE. Refer to the appropriate link for more details [JRE 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) / [JRE 1.7](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html) / [JRE 1.6](http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html). You will need to extract the jar files (local_policy.jar and US_export_policy.jar) from the archive downloaded and save them in $JAVA_HOME/jre/lib/security/</sub>

3) Run **java -jar Encryption.jar crypto.jks <storepass> {<keypass>} <plaintext>** : Generates the cipher text. Verify that the Plain-text String echoed back by the utility matches what was passed in.
```
$ java -jar Encryption.jar crypto.jks changeit "Secret Passphrase"
Plain-text String : Secret Passphrase
Encrypted String : W2Uyhd0uZHtuKRBPI8B3i6z3ShNEqX4LmoTns4wrhos=
$ 
```
Use this utility to encrypt the values for dbuser, dbpassword and oktaApiKey. Update the appropriate properties in OktaLDAPBridgeConfig.properties with the encrypted values. The bridge code expects these values encrypted and proceeds to decrypt the properties before using.

Also use the tool to encrypt a value for the secretPassphrase. For this property, configure the cleartext value in the properties file and send the encrypted text as the value of the Authorization header.
