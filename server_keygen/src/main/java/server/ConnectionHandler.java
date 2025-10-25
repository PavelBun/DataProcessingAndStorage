package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionHandler implements Runnable {
    private final int port;
    private final RequestQueue requestQueue;
    private final ResponseQueue responseQueue;
    private final KeyStore keyStore;
    private final ConcurrentHashMap<SelectionKey, ClientSession> sessions;

    public ConnectionHandler(int port, RequestQueue requestQueue,
                             ResponseQueue responseQueue, KeyStore keyStore) {
        this.port = port;
        this.requestQueue = requestQueue;
        this.responseQueue = responseQueue;
        this.keyStore = keyStore;
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            // Поток для обработки ответов
            Thread responseHandler = new Thread(new ResponseHandler(selector, responseQueue), "ResponseHandler");
            responseHandler.start();

            System.out.println("Connection handler started on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        acceptClient(selector, serverChannel);
                    } else if (key.isReadable()) {
                        readFromClient(key);
                    } else if (key.isWritable()) {
                        writeToClient(key);
                    }
                }
            }

            responseHandler.interrupt();
            responseHandler.join();

        } catch (Exception e) {
            System.err.println("Connection handler error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writeToClient(SelectionKey key) throws IOException {
        ClientSession session = sessions.get(key);
        if (session == null) return;

        try {
            boolean allDataSent = session.writeData();
            if (allDataSent) {
                System.out.println("=== All data sent to client: " + session.getName());
                closeClient(key);
            }
        } catch (IOException e) {
            System.err.println("Error writing to client: " + e.getMessage());
            closeClient(key);
        }
    }
    private void acceptClient(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ);

        ClientSession session = new ClientSession(clientChannel);
        sessions.put(key, session);

        System.out.println("New client connected: " + clientChannel.getRemoteAddress());
    }

    private void readFromClient(SelectionKey key) throws IOException {
        ClientSession session = sessions.get(key);
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            if (session.readName()) {
                String name = session.getName();
                System.out.println("Received request for name: " + name);

                // Проверка есть ли уже готовые ключи
                KeyStore.KeyData keyData = keyStore.getIfReady(name);
                if (keyData != null) {
                    // Отправка ключей
                    session.setKeyData(keyData);
                    key.interestOps(SelectionKey.OP_WRITE);
                } else {
                    // Ставим в очередь на генерацию
                    requestQueue.addRequest(new KeyRequest(name, session));
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + channel.getRemoteAddress());
            closeClient(key);
        }
    }

    private void closeClient(SelectionKey key) throws IOException {
        ClientSession session = sessions.remove(key);
        if (session != null) {
            session.close();
        }
        key.channel().close();
        key.cancel();
    }

    // Внутренний класс для обработки ответов
    private class ResponseHandler implements Runnable {
        private final Selector selector;
        private final ResponseQueue responseQueue;

        public ResponseHandler(Selector selector, ResponseQueue responseQueue) {
            this.selector = selector;
            this.responseQueue = responseQueue;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    KeyResponse response = responseQueue.takeResponse();
                    ClientSession session = response.getSession();

                    // Находим ключ для этого сеанса
                    selector.keys().stream()
                            .filter(key -> key.channel() == session.getChannel())
                            .findFirst()
                            .ifPresent(key -> {
                                session.setKeyData(response.getKeyData());
                                key.interestOps(SelectionKey.OP_WRITE);
                                selector.wakeup(); // Будим селектор для обработки записи
                            });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}