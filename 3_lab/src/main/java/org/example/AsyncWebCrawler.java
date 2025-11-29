package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Phaser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

public class AsyncWebCrawler {
    private final HttpClient client;
    private final String baseUrl;
    private final Set<String> visitedUrls;
    private final ConcurrentLinkedQueue<String> messages;
    private final Phaser phaser;

    public AsyncWebCrawler(String host, int port) {
        this.client = HttpClient.newHttpClient();
        this.baseUrl = "http://" + host + ":" + port;
        this.visitedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.messages = new ConcurrentLinkedQueue<>();
        this.phaser = new Phaser(1);
    }

    public void crawl(String startPath) {
        visitUrl(startPath);
        phaser.arriveAndAwaitAdvance();
    }

    private void visitUrl(String path) {
        // Добавляем / к пути если его нет
        String normalizedPath = path.startsWith("/") ? path : "/" + path;

        if (!visitedUrls.add(normalizedPath)) {
            return;
        }

        phaser.register();

        Thread.startVirtualThread(() -> {
            try {
                System.out.println("Visiting: " + normalizedPath);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + normalizedPath))
                        .GET()
                        .timeout(java.time.Duration.ofSeconds(30))
                        .build();

                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Response from " + normalizedPath + ": " + response.statusCode());

                if (response.statusCode() == 200) {
                    processResponse(response.body());
                }
            } catch (Exception e) {
                System.err.println("Error visiting " + normalizedPath + ": " + e.getMessage());
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    private void processResponse(String jsonBody) {
        try {
            JsonObject json = JsonParser.parseString(jsonBody).getAsJsonObject();

            if (json.has("message")) {
                String message = json.get("message").getAsString();
                messages.add(message);
                System.out.println("Found message: " + message);
            }

            if (json.has("successors")) {
                JsonArray successors = json.get("successors").getAsJsonArray();
                for (var element : successors) {
                    String successorPath = element.getAsString();
                    visitUrl(successorPath);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing response: " + e.getMessage());
        }
    }

    public List<String> getSortedMessages() {
        return messages.stream().sorted().toList();
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default 8080");
            }
        }

        System.out.println("Starting web crawler on " + host + ":" + port);

        AsyncWebCrawler crawler = new AsyncWebCrawler(host, port);

        long startTime = System.currentTimeMillis();
        crawler.crawl("/");
        long endTime = System.currentTimeMillis();

        List<String> sortedMessages = crawler.getSortedMessages();

        System.out.println("\n=== CRAWLING RESULTS ===");
        System.out.println("Collected " + sortedMessages.size() + " messages:");
        sortedMessages.forEach(System.out::println);
        System.out.printf("Time taken: %.2f seconds%n", (endTime - startTime) / 1000.0);
    }
}