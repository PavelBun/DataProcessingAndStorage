package server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ResponseQueue {
    private final BlockingQueue<KeyResponse> queue = new LinkedBlockingQueue<>();

    public void addResponse(KeyResponse response) {
        queue.offer(response);
    }

    public KeyResponse takeResponse() throws InterruptedException {
        return queue.take();
    }
}