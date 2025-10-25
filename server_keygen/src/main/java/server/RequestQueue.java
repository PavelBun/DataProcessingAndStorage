package server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestQueue {
    private final BlockingQueue<KeyRequest> queue = new LinkedBlockingQueue<>();

    public void addRequest(KeyRequest request) {
        queue.offer(request);
    }

    public KeyRequest takeRequest() throws InterruptedException {
        return queue.take();
    }
}