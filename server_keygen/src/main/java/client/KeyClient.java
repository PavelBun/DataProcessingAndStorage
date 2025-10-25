package client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class KeyClient {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: KeyClient <name> <host> <port> [delay] [crash]");
            return;
        }

        String name = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        int delay = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        boolean crash = args.length > 4 && Boolean.parseBoolean(args[4]);

        byte[] privateKeyBytes = new byte[0];
        byte[] certBytes = new byte[0];

        try {
            System.out.println("=== CLIENT: Connecting to " + host + ":" + port);
            Socket socket = new Socket(host, port);
            System.out.println("=== CLIENT: Connected successfully");

            // Отправляем имя с нулевым байтом
            OutputStream out = socket.getOutputStream();
            byte[] nameBytes = name.getBytes("ASCII");
            System.out.println("=== CLIENT: Sending name: '" + name + "' (" + nameBytes.length + " bytes + null byte)");
            out.write(nameBytes);
            out.write(0);
            out.flush();
            System.out.println("=== CLIENT: Name sent successfully");

            if (crash) {
                System.out.println("=== CLIENT: Crashing as requested");
                System.exit(1);
            }

            if (delay > 0) {
                System.out.println("=== CLIENT: Delaying for " + delay + " seconds");
                Thread.sleep(delay * 1000);
            }

            // Читаем ответ
            InputStream in = socket.getInputStream();
            System.out.println("=== CLIENT: Starting to read response...");

            // Читаем приватный ключ
            byte[] lenBytes = readBytes(in, 4);
            int privateKeyLen = bytesToInt(lenBytes);
            System.out.println("=== CLIENT: Private key length: " + privateKeyLen + " bytes");

            if (privateKeyLen > 0) {
                privateKeyBytes = readBytes(in, privateKeyLen); // Убираем объявление byte[]
                System.out.println("=== CLIENT: Received private key: " + privateKeyBytes.length + " bytes");
            } else {
                System.out.println("=== CLIENT: ERROR: Private key length is 0!");
            }

            // Читаем сертификат
            lenBytes = readBytes(in, 4);
            int certLen = bytesToInt(lenBytes);
            System.out.println("=== CLIENT: Certificate length: " + certLen + " bytes");

            if (certLen > 0) {
                certBytes = readBytes(in, certLen);
                System.out.println("=== CLIENT: Received certificate: " + certBytes.length + " bytes");
            } else {
                System.out.println("=== CLIENT: Certificate is empty (this is OK for testing)");
            }

            socket.close();
            System.out.println("=== CLIENT: Connection closed");

            System.out.println("=== CLIENT: SUCCESS - Would save files:");
            System.out.println("=== CLIENT:   " + name + ".key (" + privateKeyLen + " bytes)");
            System.out.println("=== CLIENT:   " + name + ".crt (" + certLen + " bytes)");

            saveKeyFiles(name, privateKeyBytes, certBytes);
            System.out.println("=== CLIENT: Keys saved successfully for: " + name);

        } catch (Exception e) {
            System.err.println("=== CLIENT ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] readBytes(InputStream in, int length) throws IOException {
        byte[] buffer = new byte[length];
        int read = 0;
        while (read < length) {
            int result = in.read(buffer, read, length - read);
            if (result == -1) throw new IOException("Unexpected end of stream");
            read += result;
        }
        return buffer;
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
    }

    private static void saveKeyFiles(String name, byte[] privateKeyBytes, byte[] certBytes) throws Exception {
        // Сохраняем приватный ключ
        Files.write(Paths.get(name + ".key"), privateKeyBytes);

        // Сохраняем сертификат
        Files.write(Paths.get(name + ".crt"), certBytes);
    }
}