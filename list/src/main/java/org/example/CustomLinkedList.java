package org.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Пользовательская реализация односвязного списка
class CustomLinkedList implements Iterable<String> {
    private Node head;
    private final Lock headLock = new ReentrantLock();
    private volatile int size = 0;
    private Node currentSortPosition = null;

    public void addFirst(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Значение не может быть null");
        }

        Node newNode = new Node(value);
        headLock.lock();
        try {
            newNode.next = head;
            head = newNode;
            size++;
            currentSortPosition = null; // Сброс при изменении списка
        } finally {
            headLock.unlock();
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new LinkedListIterator();
    }

    // Отдельный класс итератора 
    private class LinkedListIterator implements Iterator<String> {
        private Node current = head;

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Нет больше элементов в списке");
            }
            String value = current.value;
            current = current.next;
            return value;
        }
    }

    public int size() {
        return size;
    }

    public int bubbleSortStep() {
        if (head == null || head.next == null) {
            return 0;
        }

        headLock.lock();
        try {
            // Инициализация или сброс позиции сортировки
            if (currentSortPosition == null || currentSortPosition.next == null) {
                currentSortPosition = head;
            }

            Node prev = null;
            Node current = head;

            // Находим узел, предшествующий currentSortPosition
            while (current != null && current != currentSortPosition) {
                prev = current;
                current = current.next;
            }

            // Проверка граничных условий
            if (current == null || current.next == null) {
                currentSortPosition = head;
                return 0;
            }

            Node next = current.next;

            // Проверяем необходимость перестановки
            if (current.value.compareTo(next.value) <= 0) {
                currentSortPosition = current.next;
                return 0;
            }

            // Захват блокировок в строгом порядке
            if (prev != null) {
                prev.lock();
            }
            current.lock();
            next.lock();

            try {
                // Повторная проверка после захвата блокировок
                if (current.value.compareTo(next.value) > 0) {
                    // Перестановка ссылок
                    if (prev == null) {
                        head = next;
                    } else {
                        prev.next = next;
                    }
                    current.next = next.next;
                    next.next = current;

                    // Обновление позиции для следующего шага
                    currentSortPosition = (current.next != null) ? current.next : head;
                    return 1;
                }

                // Если условие изменилось, просто двигаемся дальше
                currentSortPosition = current.next;
                return 0;

            } finally {
                // Освобождение блокировок в обратном порядке
                next.unlock();
                current.unlock();
                if (prev != null) {
                    prev.unlock();
                }
            }
        } catch (Exception e) {
            System.err.println("Исключение в bubbleSortStep: " + e.getMessage());
            currentSortPosition = head; // Сброс при ошибке
            return 0;
        } finally {
            headLock.unlock();
        }
    }

    public List<String> toList() {
        List<String> result = new ArrayList<>();
        Node current = head;
        while (current != null) {
            result.add(current.value);
            current = current.next;
        }
        return result;
    }
}