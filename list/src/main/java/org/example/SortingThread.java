package org.example;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class SortingThread implements Runnable {
    private final CustomLinkedList list;
    private final AtomicBoolean running;
    private final AtomicLong stepsCount = new AtomicLong(0);
    private final Thread workerThread;

    public SortingThread(CustomLinkedList list, AtomicBoolean running) {
        this.list = list;
        this.running = running;
        this.workerThread = new Thread(this, "SortingThread-" + System.currentTimeMillis());
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
                // Делаем шаг сортировки
                int swaps = list.bubbleSortStep();
                stepsCount.incrementAndGet();

                if (swaps > 0) {
                    System.out.println(Thread.currentThread().getName() +
                            " сделал перестановку, шаг: " + stepsCount.get());
                }
                Thread.sleep(1000);

            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " получил прерывание");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Ошибка в " + Thread.currentThread().getName() +
                        ": " + e.getMessage());

                // Продолжаем работу после ошибки с задержкой
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