package org.example.parser;

import org.example.model.Person;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StaxXmlParser {

    public Map<String, Person> parse(Path input) throws Exception {
        Map<String, Person> peopleById = new LinkedHashMap<>();
        Map<String, Person> nameToPerson = new HashMap<>();

        XMLInputFactory factory = XMLInputFactory.newInstance();

        try (InputStream is = Files.newInputStream(input)) {
            XMLStreamReader reader = factory.createXMLStreamReader(is);

            Person currentPerson = null;
            StringBuilder currentText = new StringBuilder();
            String currentElement = null;
            boolean inPerson = false;
            boolean inFullname = false;

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String elementName = reader.getLocalName();
                        currentElement = elementName;

                        if ("person".equals(elementName)) {
                            currentPerson = new Person();
                            inPerson = true;

                            // Ищем ID
                            for (int i = 0; i < reader.getAttributeCount(); i++) {
                                if ("id".equals(reader.getAttributeLocalName(i))) {
                                    String idValue = reader.getAttributeValue(i);
                                    if (idValue != null && !idValue.trim().isEmpty()) {
                                        currentPerson.setId(idValue.trim());
                                    }
                                    break;
                                }
                            }
                        } else if ("fullname".equals(elementName) && currentPerson != null) {
                            inFullname = true;
                        }
                        currentText.setLength(0);
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        if (inPerson) {
                            currentText.append(reader.getText());
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String elementNameEnd = reader.getLocalName();
                        String text = currentText.toString().trim();

                        if ("person".equals(elementNameEnd) && currentPerson != null) {
                            // Завершаем обработку человека
                            finishPerson(currentPerson);
                            savePerson(currentPerson, peopleById, nameToPerson);
                            currentPerson = null;
                            inPerson = false;
                        } else if ("fullname".equals(elementNameEnd)) {
                            inFullname = false;
                        } else if (inPerson && currentPerson != null && !text.isEmpty()) {
                            // Обрабатываем элемент
                            processElement(currentPerson, elementNameEnd, text, inFullname);
                        }
                        currentElement = null;
                        currentText.setLength(0);
                        break;
                }
            }
        }

        // Объединяем дубликаты по имени
        mergeDuplicates(peopleById, nameToPerson);

        return peopleById;
    }

    private void processElement(Person person, String elementName, String text, boolean inFullname) {
        switch (elementName.toLowerCase()) {
            case "firstname":
            case "first":
                if (person.getFirstName() == null) person.setFirstName(text);
                break;
            case "surname":
            case "lastname":
            case "last":
            case "family":
                if (person.getLastName() == null) person.setLastName(text);
                break;
            case "fullname":
            case "full":
                if (person.getFullName() == null && !inFullname) {
                    person.setFullName(text);
                }
                break;
            case "gender":
                String normalizedGender = normalizeGender(text);
                if ("unknown".equals(person.getGender()) ||
                        (!"unknown".equals(normalizedGender) && !normalizedGender.equals(person.getGender()))) {
                    person.setGender(normalizedGender);
                }
                break;
            case "spouse":
            case "husband":
            case "wife":
                if (isValidName(text)) {
                    // Проверяем, не является ли это ID
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getSpouseIds().add(text);
                    } else {
                        person.getSpouseNames().add(text);
                    }
                }
                break;
            case "parent":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getParentIds().add(text);
                    } else {
                        person.getParentNames().add(text);
                    }
                }
                break;
            case "father":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getFatherIds().add(text);
                        person.getParentIds().add(text);
                    } else {
                        person.getFatherNames().add(text);
                        person.getParentNames().add(text);
                    }
                }
                break;
            case "mother":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getMotherIds().add(text);
                        person.getParentIds().add(text);
                    } else {
                        person.getMotherNames().add(text);
                        person.getParentNames().add(text);
                    }
                }
                break;
            case "child":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getChildIds().add(text);
                    } else {
                        person.getChildNames().add(text);
                    }
                }
                break;
            case "son":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getSonIds().add(text);
                        person.getChildIds().add(text);
                    } else {
                        person.getSonNames().add(text);
                        person.getChildNames().add(text);
                    }
                }
                break;
            case "daughter":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getDaughterIds().add(text);
                        person.getChildIds().add(text);
                    } else {
                        person.getDaughterNames().add(text);
                        person.getChildNames().add(text);
                    }
                }
                break;
            case "sibling":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getSiblingIds().add(text);
                    } else {
                        person.getSiblingNames().add(text);
                    }
                }
                break;
            case "brother":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getBrotherIds().add(text);
                        person.getSiblingIds().add(text);
                    } else {
                        person.getBrotherNames().add(text);
                        person.getSiblingNames().add(text);
                    }
                }
                break;
            case "sister":
                if (isValidName(text)) {
                    if (text.startsWith("P") && text.substring(1).matches("\\d+")) {
                        person.getSisterIds().add(text);
                        person.getSiblingIds().add(text);
                    } else {
                        person.getSisterNames().add(text);
                        person.getSiblingNames().add(text);
                    }
                }
                break;
            case "children-number":
                try {
                    int value = Integer.parseInt(text);
                    if (value >= 0) {
                        person.setExpectedChildren(value);
                    }
                } catch (NumberFormatException e) {
                    // Игнорируем
                }
                break;
            case "siblings-number":
                try {
                    int value = Integer.parseInt(text);
                    if (value >= 0) {
                        person.setExpectedSiblings(value);
                    }
                } catch (NumberFormatException e) {
                    // Игнорируем
                }
                break;
        }
    }

    private void finishPerson(Person person) {
        // Устанавливаем ID, если его нет
        if (person.getId() == null || person.getId().isEmpty()) {
            String name = person.getBestName();
            if (!"Unknown".equals(name)) {
                // Генерируем ID на основе имени
                person.setId("P" + Math.abs(name.hashCode()));
            } else {
                person.setId("P_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
            }
        }

        // Заполняем fullName, если его нет
        if (person.getFullName() == null || person.getFullName().trim().isEmpty()) {
            if (person.getFirstName() != null && person.getLastName() != null) {
                person.setFullName(person.getFirstName() + " " + person.getLastName());
            } else if (person.getFirstName() != null) {
                person.setFullName(person.getFirstName());
            } else if (person.getLastName() != null) {
                person.setFullName(person.getLastName());
            } else {
                person.setFullName(person.getId());
            }
        }

        // Улучшаем определение пола по имени
        improveGenderByFirstName(person);
    }

    private void improveGenderByFirstName(Person person) {
        if (!"unknown".equals(person.getGender())) return;
        if (person.getFirstName() == null) return;

        String name = person.getFirstName().toLowerCase().trim();

        // Женские окончания
        String[] femaleEndings = {"a", "e", "ie", "ette", "elle", "is", "ys", "ice", "ina",
                "elle", "ia", "na", "ra", "la", "ta", "sha", "tha", "ca"};

        // Мужские окончания
        String[] maleEndings = {"o", "n", "r", "d", "s", "t", "k", "l", "m", "b",
                "y", "er", "or", "us", "io", "ex", "ix", "ax"};




        // Проверяем окончания
        for (String ending : femaleEndings) {
            if (name.endsWith(ending)) {
                person.setGender("female");
                return;
            }
        }

        for (String ending : maleEndings) {
            if (name.endsWith(ending)) {
                person.setGender("male");
                return;
            }
        }
    }

    private String normalizeGender(String gender) {
        if (gender == null) return "unknown";
        String g = gender.toLowerCase().trim();
        if (g.equals("m") || g.equals("male") || g.equals("муж") || g.equals("мужской")) return "male";
        if (g.equals("f") || g.equals("female") || g.equals("жен") || g.equals("женский")) return "female";
        return "unknown";
    }

    private boolean isValidName(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return !trimmed.isEmpty() &&
                !trimmed.equalsIgnoreCase("unknown") &&
                !trimmed.equalsIgnoreCase("none") &&
                !trimmed.equalsIgnoreCase("null");
    }

    private void savePerson(Person person, Map<String, Person> peopleById, Map<String, Person> nameToPerson) {
        // Проверяем, нет ли уже человека с таким ID
        Person existing = peopleById.get(person.getId());
        if (existing != null) {
            existing.merge(person);
            peopleById.put(existing.getId(), existing);
            person = existing;
        } else {
            peopleById.put(person.getId(), person);
        }

        // Индексируем по имени
        String name = person.getBestName();
        if (!"Unknown".equals(name)) {
            nameToPerson.put(normalizeName(name), person);

            // Также индексируем по отдельным частям
            if (person.getFirstName() != null) {
                nameToPerson.put(normalizeName(person.getFirstName()), person);
            }
            if (person.getLastName() != null) {
                nameToPerson.put(normalizeName(person.getLastName()), person);
            }
        }
    }

    private void mergeDuplicates(Map<String, Person> peopleById, Map<String, Person> nameToPerson) {
        // Создаем список для удаления дубликатов
        Set<String> toRemove = new HashSet<>();

        // Ищем людей с одинаковыми именами
        Map<String, List<Person>> duplicatesByName = new HashMap<>();

        for (Person person : peopleById.values()) {
            String nameKey = normalizeName(person.getBestName());
            if (!"unknown".equals(nameKey)) {
                duplicatesByName.computeIfAbsent(nameKey, k -> new ArrayList<>()).add(person);
            }
        }

        // Объединяем дубликаты
        for (List<Person> duplicateList : duplicatesByName.values()) {
            if (duplicateList.size() > 1) {
                Person mainPerson = duplicateList.get(0);

                // Сливаем остальных с первым
                for (int i = 1; i < duplicateList.size(); i++) {
                    Person duplicate = duplicateList.get(i);
                    mainPerson.merge(duplicate);
                    toRemove.add(duplicate.getId());
                }
            }
        }

        // Удаляем дубликаты
        for (String id : toRemove) {
            peopleById.remove(id);
        }
    }

    private String normalizeName(String name) {
        return name.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}