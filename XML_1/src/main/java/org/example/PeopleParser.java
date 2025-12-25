package org.example;

import org.example.model.Person;
import org.example.parser.StaxXmlParser;
import org.example.processor.PeopleProcessor;
import org.example.writer.XmlWriter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class PeopleParser {
    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        String inputPath = args.length > 0 ? args[0] : "src/main/resources/people.xml";
        String outputPath = args.length > 1 ? args[1] : "output/people_structured.xml";

        System.out.println("Входной файл: " + inputPath);
        System.out.println("Выходной файл: " + outputPath);

        Path outPath = Paths.get(outputPath);
        if (outPath.getParent() != null) {
            java.nio.file.Files.createDirectories(outPath.getParent());
        }

        // Парсим XML
        StaxXmlParser parser = new StaxXmlParser();
        Map<String, Person> people = parser.parse(Paths.get(inputPath));
        System.out.println("Найдено уникальных людей: " + people.size());

        // Обрабатываем связи
        PeopleProcessor processor = new PeopleProcessor();
        processor.processRelationships(people);

        // Проверяем консистентность
        processor.checkConsistency(people);

        // Записываем структурированный XML
        XmlWriter writer = new XmlWriter();
        writer.write(people, outPath);

        long endTime = System.currentTimeMillis();
        System.out.println("Время выполнения: " + (endTime - startTime) + " мс");
        System.out.println("Обработка завершена успешно!");
    }
}