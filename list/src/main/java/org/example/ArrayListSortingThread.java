package org.example;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class ArrayListSortingThread implements Runnable {
    private final List<String> list;
    private final AtomicBoolean running;
    private final AtomicLong stepsCount = new AtomicLong(0);
    private final Thread workerThread;
    private int currentPosition = 0;

    public ArrayListSortingThread(List<String> list, AtomicBoolean running) {
        this.list = list;
        this.running = running;
        this.workerThread = new Thread(this, "ArrayListSortingThread-" + System.currentTimeMillis());
        this.workerThread.setDaemon(true);
    }

    public void start() {
        workerThread.start();
    }

    @Override
    public void run() {
        System.out.println("Поток сортировки " + Thread.currentThread().getName() + " запущен");

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                int swaps = bubbleSortStep();
                stepsCount.incrementAndGet();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " получил прерывание");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Ошибка в " + Thread.currentThread().getName() +
                        ": " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        System.out.println(Thread.currentThread().getName() + " завершен, шагов: " + stepsCount.get());
    }

    private int bubbleSortStep() {
        int swaps = 0;
        synchronized (list) {
            if (list.size() <= 1) return 0;

            if (currentPosition >= list.size() - 1) {
                currentPosition = 0;
            }

            if (currentPosition < list.size() - 1) {
                if (list.get(currentPosition).compareTo(list.get(currentPosition + 1)) > 0) {
                    Collections.swap(list, currentPosition, currentPosition + 1);
                    swaps = 1;
                }
                currentPosition++;
            }
        }
        return swaps;
    }

    public void stopSorting() {
        workerThread.interrupt();
    }

    public long getStepsCount() {
        return stepsCount.get();
    }

    public void join() throws InterruptedException {
        workerThread.join();
    }

    public void join(long millis) throws InterruptedException {
        workerThread.join(millis);
    }
}