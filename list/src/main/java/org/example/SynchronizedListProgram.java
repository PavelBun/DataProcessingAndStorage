package org.example;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SynchronizedListProgram {
    private static final int THREAD_COUNT = 2;
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final int MAX_DISPLAY_ITEMS = 50;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            System.out.println("\nПолучен сигнал завершения...");
        }));

        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("=== Синхронизированный доступ к списку ===");
            System.out.println("Выберите реализацию списка:");
            System.out.println("1 - Собственная реализация связного списка");
            System.out.println("2 - ArrayList с Collections.synchronizedList");
            System.out.print("Ваш выбор: ");

            String input = scanner.nextLine().trim();
            int choice = parseChoice(input);

            if (choice == 1) {
                runWithCustomList(scanner);
            } else {
                runWithArrayList(scanner);
            }
        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
            System.out.println("Программа завершена.");
        }
    }

    private static int parseChoice(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Неверный ввод, используется вариант 1");
            return 1;
        }
    }

    private static void runWithCustomList(Scanner scanner) {
        CustomLinkedList list = new CustomLinkedList();
        List<SortingThread> threads = new ArrayList<>();
        AtomicBoolean runningFlag = new AtomicBoolean(true);

        // Запуск потоков сортировки
        for (int i = 0; i < THREAD_COUNT; i++) {
            SortingThread thread = new SortingThread(list, runningFlag);
            thread.start();
            threads.add(thread);
        }

        printInstructions("Собственная реализация связного списка");

        try {
            processUserInput(scanner, list, runningFlag);
        } finally {
            runningFlag.set(false);
            waitForThreadsCompletion(threads);
            printStatistics(threads, "собственная реализация");
        }
    }

    private static void runWithArrayList(Scanner scanner) {
        List<String> list = Collections.synchronizedList(new ArrayList<>());
        List<ArrayListSortingThread> threads = new ArrayList<>();
        AtomicBoolean runningFlag = new AtomicBoolean(true);

        // Запуск потоков сортировки
        for (int i = 0; i < THREAD_COUNT; i++) {
            ArrayListSortingThread thread = new ArrayListSortingThread(list, runningFlag);
            thread.start();
            threads.add(thread);
        }

        printInstructions("ArrayList с Collections.synchronizedList");

        try {
            processUserInputForArrayList(scanner, list, runningFlag);
        } finally {
            runningFlag.set(false);
            waitForArrayListThreadsCompletion(threads);
            printArrayListStatistics(threads);
        }
    }

    private static void processUserInput(Scanner scanner, CustomLinkedList list, AtomicBoolean runningFlag) {
        while (runningFlag.get() && running.get()) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();

            if (processCommand(input, list, runningFlag)) {
                break;
            }
        }
    }

    private static void processUserInputForArrayList(Scanner scanner, List<String> list, AtomicBoolean runningFlag) {
        while (runningFlag.get() && running.get()) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;

            String input = scanner.nextLine().trim();

            if (processCommandForArrayList(input, list, runningFlag)) {
                break;
            }
        }
    }

    private static boolean processCommand(String input, CustomLinkedList list, AtomicBoolean runningFlag) {
        if (input.equalsIgnoreCase("exit")) {
            return true;
        } else if (input.equalsIgnoreCase("stop")) {
            System.out.println("Остановка сортировки...");
            runningFlag.set(false);
            return true;
        } else if (input.isEmpty()) {
            printListState(list);
        } else {
            addStringToList(list, input);
        }
        return false;
    }

    private static boolean processCommandForArrayList(String input, List<String> list, AtomicBoolean runningFlag) {
        if (input.equalsIgnoreCase("exit")) {
            return true;
        } else if (input.equalsIgnoreCase("stop")) {
            System.out.println("Остановка сортировки...");
            runningFlag.set(false);
            return true;
        } else if (input.isEmpty()) {
            printArrayListState(list);
        } else {
            addStringToArrayList(list, input);
        }
        return false;
    }

    private static void addStringToList(CustomLinkedList list, String input) {
        try {
            if (input.length() > 80) {
                int parts = splitAndAddLongString(list, input);
                System.out.println("✓ Добавлено " + parts + " частей строки");
            } else {
                list.addFirst(input);
                System.out.println("✓ Добавлена строка: '" + input + "'");
            }
        } catch (Exception e) {
            System.err.println("✗ Ошибка при добавлении: " + e.getMessage());
        }
    }

    private static void addStringToArrayList(List<String> list, String input) {
        try {
            if (input.length() > 80) {
                int parts = splitAndAddLongStringToArrayList(list, input);
                System.out.println("✓ Добавлено " + parts + " частей строки");
            } else {
                synchronized (list) {
                    list.add(0, input);
                }
                System.out.println("✓ Добавлена строка: '" + input + "'");
            }
        } catch (Exception e) {
            System.err.println("✗ Ошибка при добавлении: " + e.getMessage());
        }
    }

    private static int splitAndAddLongString(CustomLinkedList list, String input) {
        int parts = 0;
        for (int i = 0; i < input.length(); i += 80) {
            int end = Math.min(i + 80, input.length());
            String part = input.substring(i, end);
            list.addFirst(part);
            parts++;
        }
        return parts;
    }

    private static int splitAndAddLongStringToArrayList(List<String> list, String input) {
        int parts = 0;
        for (int i = 0; i < input.length(); i += 80) {
            int end = Math.min(i + 80, input.length());
            String part = input.substring(i, end);
            synchronized (list) {
                list.add(0, part);
            }
            parts++;
        }
        return parts;
    }

    private static void waitForThreadsCompletion(List<SortingThread> threads) {
        for (SortingThread thread : threads) {
            thread.stopSorting();
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                System.err.println("Прервано ожидание завершения потока");
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void waitForArrayListThreadsCompletion(List<ArrayListSortingThread> threads) {
        for (ArrayListSortingThread thread : threads) {
            thread.stopSorting();
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                System.err.println("Прервано ожидание завершения потока");
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void printInstructions(String implementation) {
        System.out.println("\n=== " + implementation + " ===");
        System.out.println("Запущено потоков сортировки: " + THREAD_COUNT);
        System.out.println("Команды:");
        System.out.println("  - Ввод текста: добавление строки в список");
        System.out.println("  - Пустая строка: вывод текущего состояния списка");
        System.out.println("  - 'stop': остановка сортировки и вывод статистики");
        System.out.println("  - 'exit': завершение программы");
        System.out.println("=============================================");
    }

    private static void printListState(CustomLinkedList list) {
        System.out.println("\n--- Текущее состояние списка ---");
        try {
            List<String> items = list.toList();
            printItemsWithLimit(items);
        } catch (Exception e) {
            System.err.println("Ошибка при выводе состояния: " + e.getMessage());
        }
        System.out.println("--------------------------------\n");
    }

    private static void printArrayListState(List<String> list) {
        System.out.println("\n--- Текущее состояние списка ---");
        try {
            synchronized (list) {
                printItemsWithLimit(new ArrayList<>(list));
            }
        } catch (Exception e) {
            System.err.println("Ошибка при выводе состояния: " + e.getMessage());
        }
        System.out.println("--------------------------------\n");
    }

    private static void printItemsWithLimit(List<String> items) {
        int totalSize = items.size();
        if (totalSize == 0) {
            System.out.println("Список пуст");
        } else {
            int displayCount = Math.min(totalSize, MAX_DISPLAY_ITEMS);
            for (int i = 0; i < displayCount; i++) {
                System.out.printf("%2d: '%s'%n", i, items.get(i));
            }
            if (totalSize > MAX_DISPLAY_ITEMS) {
                System.out.printf("... и еще %d элементов%n", totalSize - MAX_DISPLAY_ITEMS);
            }
        }
        System.out.println("Всего элементов: " + totalSize);
    }

    private static void printStatistics(List<SortingThread> threads, String implName) {
        System.out.println("\n=== Статистика ===");
        System.out.println("Реализация: " + implName);
        long totalSteps = 0;
        for (int i = 0; i < threads.size(); i++) {
            long steps = threads.get(i).getStepsCount();
            totalSteps += steps;
            System.out.println("Поток " + i + ": " + steps + " шагов сортировки");
        }
        System.out.println("Всего шагов: " + totalSteps);
        System.out.println("==================\n");
    }

    private static void printArrayListStatistics(List<ArrayListSortingThread> threads) {
        System.out.println("\n=== Статистика ===");
        System.out.println("Реализация: ArrayList с Collections.synchronizedList");
        long totalSteps = 0;
        for (int i = 0; i < threads.size(); i++) {
            long steps = threads.get(i).getStepsCount();
            totalSteps += steps;
            System.out.println("Поток " + i + ": " + steps + " шагов сортировки");
        }
        System.out.println("Всего шагов: " + totalSteps);
        System.out.println("==================\n");
    }
}