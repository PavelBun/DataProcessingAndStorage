package server;

public record KeyResponse(KeyStore.KeyData keyData, ClientSession session) {
    public KeyStore.KeyData getKeyData() { return keyData; }
    public ClientSession getSession() { return session; }
}