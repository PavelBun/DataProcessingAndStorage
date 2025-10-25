package server;

import java.io.FileInputStream;
import java.security.PrivateKey;
import java.security.KeyStore;

public class KeyUtils {

    public static PrivateKey loadPrivateKey(String filename) throws Exception {
        try (FileInputStream fis = new FileInputStream(filename)) {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(fis, "password".toCharArray());
            return (PrivateKey) keystore.getKey("ca", "password".toCharArray());
        }
    }
}