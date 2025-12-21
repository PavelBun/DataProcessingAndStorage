package org.example;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Узел односвязного списка
class Node {
    String value;
    Node next;
    final Lock lock = new ReentrantLock();

    Node(String value) {
        this.value = value;
    }

    void lock() {
        lock.lock();
    }

    void unlock() {
        lock.unlock();
    }

    @Override
    public String toString() {
        return value;
    }
}


