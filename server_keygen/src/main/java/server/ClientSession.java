package server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ClientSession {
    private final SocketChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteArrayOutputStream nameBuffer;
    private String name;
    private KeyStore.KeyData keyData;
    private ByteBuffer writeBuffer;

    public ClientSession(SocketChannel channel) {
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(1024);
        this.nameBuffer = new ByteArrayOutputStream();
        this.name = null;
        this.keyData = null;
        this.writeBuffer = null;
    }

    public boolean readName() throws IOException {
        readBuffer.clear();
        int bytesRead = channel.read(readBuffer);

        if (bytesRead == -1) {
            throw new IOException("End of stream");
        }

        readBuffer.flip();
        while (readBuffer.hasRemaining()) {
            byte b = readBuffer.get();
            if (b == 0) {
                name = nameBuffer.toString("ASCII");
                nameBuffer.reset();
                return true;
            } else {
                nameBuffer.write(b);
            }
        }

        return false;
    }

    public void setKeyData(KeyStore.KeyData keyData) {
        this.keyData = keyData;
        prepareWriteBuffer();
    }

    private void prepareWriteBuffer() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // Записываем приватный ключ
            byte[] privateKeyBytes = keyData.privateKey().getEncoded();
            output.write(intToBytes(privateKeyBytes.length));
            output.write(privateKeyBytes);

            // Записываем сертификат
            byte[] certBytes = keyData.certificate().getEncoded();
            output.write(intToBytes(certBytes.length));
            output.write(certBytes);

            byte[] allData = output.toByteArray();
            writeBuffer = ByteBuffer.wrap(allData);

        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare response data", e);
        }
    }

    private byte[] intToBytes(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value
        };
    }

    public boolean writeData() throws IOException {
        if (writeBuffer == null) {
            System.err.println("=== ERROR: No data to write for client: " + name);
            return true;
        }

        System.out.println("=== Starting to write data to: " + name);
        System.out.println("=== Buffer - position: " + writeBuffer.position() + ", limit: " + writeBuffer.limit() + ", remaining: " + writeBuffer.remaining());

        int bytesWritten = channel.write(writeBuffer);
        System.out.println("=== Bytes written: " + bytesWritten);
        System.out.println("=== Remaining after write: " + writeBuffer.remaining());

        if (!writeBuffer.hasRemaining()) {
            writeBuffer = null;
            System.out.println("=== SUCCESS: All data sent to: " + name);
            return true;
        }

        System.out.println("=== More data to send to: " + name);
        return false;
    }

    public String getName() { return name; }
    public SocketChannel getChannel() { return channel; }
    public KeyStore.KeyData getKeyData() { return keyData; }

    public void close() throws IOException {
        channel.close();
    }
}