package server;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.concurrent.ConcurrentHashMap;

public class KeyStore {
    private final ConcurrentHashMap<String, KeyData> store = new ConcurrentHashMap<>();

    public record KeyData(PrivateKey privateKey, Certificate certificate) {}

    public void put(String name, KeyData keyData) {
        store.put(name, keyData);
    }

    public KeyData getIfReady(String name) {
        return store.get(name);
    }
}