package client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TokenHandling {
    
    private static KeyStore loadKeyStore(String fileName, char[] password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        java.io.FileInputStream fis = null;
        
        try {
            fis = new java.io.FileInputStream(fileName);
            keyStore.load(fis, password);
        } catch(FileNotFoundException e) {
            keyStore.load(fis, password);
        } finally {
            if(fis != null) {
                fis.close();
            }
        }

        return keyStore;
    }


    /**
     * @param password
     * @param fileName
     * @param alias
     * @param key
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static void saveToPKCS12KeyStore(final String password, String fileName, String alias, String key) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        char[] pw = password.toCharArray();
        
        final KeyStore keyStore = TokenHandling.loadKeyStore(fileName, pw);
        // Replace all non Base64 characters.
        key = key.replaceAll("-", "+");
        key = key.replaceAll("_", "/");


        // Generate key from string.
        byte[] decodedKey = Base64.getDecoder().decode(key);
        javax.crypto.SecretKey mykey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

        // Save my key.
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pw);
        KeyStore.SecretKeyEntry kEntry = new KeyStore.SecretKeyEntry(mykey);
        keyStore.setEntry(alias, kEntry, protParam);

        // Store away the keystore.
        java.io.FileOutputStream fos = null;
        try {
            fos = new java.io.FileOutputStream(fileName);
            keyStore.store(fos, pw);
        } finally {
            if(fos != null) {
                fos.close();
            }
        }
    }

    /**
     * @param password
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableEntryException
     */
    public static String getFromPKCS12KeyStore(final String password, String fileName, String alias) 
        throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableEntryException {
        char[] pw = password.toCharArray();

        final KeyStore keyStore = TokenHandling.loadKeyStore(fileName, pw);

        // Get my key.
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pw);
        KeyStore.SecretKeyEntry kEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, protParam);
        SecretKey myKey = kEntry.getSecretKey(); 

        // Generate string from key.
        String ret  = Base64.getEncoder().encodeToString(myKey.getEncoded());
        ret = ret.replaceAll("\\+", "-");
        ret = ret.replaceAll("/", "_");
        return ret;

    }

    
}
