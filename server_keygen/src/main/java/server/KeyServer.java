package server;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class KeyServer {
    private final int port;
    private final int generatorThreads;
    private final PrivateKey issuerPrivateKey;
    private final String issuerName;

    public KeyServer(int port, int generatorThreads, PrivateKey issuerPrivateKey, String issuerName) {
        this.port = port;
        this.generatorThreads = generatorThreads;
        this.issuerPrivateKey = issuerPrivateKey;
        this.issuerName = issuerName;
    }

    public void start() throws IOException {
        ThreadPoolExecutor generatorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(generatorThreads);
        KeyStore keyStore = new KeyStore();
        RequestQueue requestQueue = new RequestQueue();
        ResponseQueue responseQueue = new ResponseQueue();

        // Запуск генераторов ключей
        for (int i = 0; i < generatorThreads; i++) {
            KeyGeneratorWorker generator = new KeyGeneratorWorker(
                    requestQueue, responseQueue, keyStore, issuerPrivateKey, issuerName);
            generatorExecutor.execute(generator);
        }

        // Запуск обработчика соединений
        ConnectionHandler connectionHandler = new ConnectionHandler(
                port, requestQueue, responseQueue, keyStore);
        new Thread(connectionHandler, "ConnectionHandler").start();

        System.out.println("Key server started on port " + port + " with " + generatorThreads + " generator threads");
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: KeyServer <port> <generatorThreads> <issuerKeyFile> <issuerName>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            int generatorThreads = Integer.parseInt(args[1]);
            String issuerKeyFile = args[2];
            String issuerName = args[3];

            PrivateKey issuerPrivateKey = KeyUtils.loadPrivateKey(issuerKeyFile);
            KeyServer server = new KeyServer(port, generatorThreads, issuerPrivateKey, issuerName);
            server.start();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}