package server;

import java.security.PrivateKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class KeyGeneratorWorker implements Runnable {
    private final RequestQueue requestQueue;
    private final ResponseQueue responseQueue;
    private final KeyStore keyStore;
    private final PrivateKey issuerPrivateKey;
    private final String issuerName;
    private final KeyPairGenerator keyPairGenerator;

    public KeyGeneratorWorker(RequestQueue requestQueue, ResponseQueue responseQueue,
                              KeyStore keyStore, PrivateKey issuerPrivateKey, String issuerName) {
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        this.keyStore = keyStore;
        this.issuerPrivateKey = issuerPrivateKey;
        this.issuerName = issuerName;

        try {
            this.keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(8192);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize key pair generator", e);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                KeyRequest request = requestQueue.takeRequest();
                String name = request.getName();

                // Проверка не сгенерированы ли ключи другим потоком
                KeyStore.KeyData existingData = keyStore.getIfReady(name);
                if (existingData != null) {
                    responseQueue.addResponse(new KeyResponse(existingData, request.getSession()));
                    continue;
                }

                // Генерируем новую пару ключей
                KeyPair keyPair = keyPairGenerator.generateKeyPair();
                java.security.cert.Certificate certificate =
                        CertificateGenerator.generateCertificate(name, issuerName, keyPair, issuerPrivateKey);

                KeyStore.KeyData keyData = new KeyStore.KeyData(keyPair.getPrivate(), certificate);

                // Сохраняем и отправляем ответ
                keyStore.put(name, keyData);
                responseQueue.addResponse(new KeyResponse(keyData, request.getSession()));

                System.out.println("Generated keys for: " + name);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Key generation error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}