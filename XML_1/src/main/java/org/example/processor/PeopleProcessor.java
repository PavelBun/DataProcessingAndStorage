package org.example.processor;

import org.example.model.Person;

import java.util.*;

public class PeopleProcessor {

    public void processRelationships(Map<String, Person> people) {
        System.out.println("Обработка связей между людьми...");

        // Создаем карту для быстрого поиска по имени
        Map<String, String> nameToId = new HashMap<>();
        for (Person person : people.values()) {
            String name = person.getBestName();
            if (name != null && !name.equals("Unknown")) {
                nameToId.put(normalizeName(name), person.getId());
            }
        }

        // Разрешаем имена в ID
        for (Person person : people.values()) {
            resolveNamesToIds(person, nameToId);
        }

        // Устанавливаем обратные связи
        for (Person person : people.values()) {
            establishReciprocalRelationships(person, people);
        }

        System.out.println("Обработка связей завершена.");
    }

    private void resolveNamesToIds(Person person, Map<String, String> nameToId) {
        resolveNameSet(person.getSpouseNames(), person.getSpouseIds(), nameToId);
        resolveNameSet(person.getParentNames(), person.getParentIds(), nameToId);
        resolveNameSet(person.getFatherNames(), person.getFatherIds(), nameToId);
        resolveNameSet(person.getMotherNames(), person.getMotherIds(), nameToId);
        resolveNameSet(person.getChildNames(), person.getChildIds(), nameToId);
        resolveNameSet(person.getSonNames(), person.getSonIds(), nameToId);
        resolveNameSet(person.getDaughterNames(), person.getDaughterIds(), nameToId);
        resolveNameSet(person.getSiblingNames(), person.getSiblingIds(), nameToId);
        resolveNameSet(person.getBrotherNames(), person.getBrotherIds(), nameToId);
        resolveNameSet(person.getSisterNames(), person.getSisterIds(), nameToId);
    }

    private void resolveNameSet(Set<String> names, Set<String> ids, Map<String, String> nameToId) {
        Iterator<String> iterator = names.iterator();
        while (iterator.hasNext()) {
            String name = iterator.next();
            String id = findPersonIdByName(name, nameToId);
            if (id != null && !id.equals("")) {
                ids.add(id);
                iterator.remove(); // Удаляем имя, так как нашли ID
            }
        }
    }

    private String findPersonIdByName(String name, Map<String, String> nameToId) {
        if (name == null || name.trim().isEmpty()) return null;

        String normalized = normalizeName(name.trim());

        // Прямой поиск по нормализованному имени
        String id = nameToId.get(normalized);
        if (id != null) return id;

        // Попробуем найти по части имени
        for (Map.Entry<String, String> entry : nameToId.entrySet()) {
            if (entry.getKey().contains(normalized) || normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private void establishReciprocalRelationships(Person person, Map<String, Person> people) {
        // Супруги: если A имеет супруга B, то B должен иметь супруга A
        for (String spouseId : person.getSpouseIds()) {
            Person spouse = people.get(spouseId);
            if (spouse != null && !spouse.getSpouseIds().contains(person.getId())) {
                spouse.getSpouseIds().add(person.getId());
            }
        }

        // Родители: если A имеет родителя B, то B должен иметь ребенка A
        for (String parentId : person.getParentIds()) {
            Person parent = people.get(parentId);
            if (parent != null && !parent.getChildIds().contains(person.getId())) {
                parent.getChildIds().add(person.getId());

                // Классифицируем ребенка по полу
                if (person.isMale()) {
                    parent.getSonIds().add(person.getId());
                } else if (person.isFemale()) {
                    parent.getDaughterIds().add(person.getId());
                }
            }
        }

        // Дети: если A имеет ребенка B, то B должен иметь родителя A
        for (String childId : person.getChildIds()) {
            Person child = people.get(childId);
            if (child != null && !child.getParentIds().contains(person.getId())) {
                child.getParentIds().add(person.getId());

                // Классифицируем родителя по полу
                if (person.isMale()) {
                    child.getFatherIds().add(person.getId());
                } else if (person.isFemale()) {
                    child.getMotherIds().add(person.getId());
                }
            }
        }

        // Братья/сестры: если A имеет брата/сестру B, то B должен иметь брата/сестру A
        for (String siblingId : person.getSiblingIds()) {
            Person sibling = people.get(siblingId);
            if (sibling != null && !sibling.getSiblingIds().contains(person.getId())) {
                sibling.getSiblingIds().add(person.getId());

                // Классифицируем по полу
                if (person.isMale()) {
                    sibling.getBrotherIds().add(person.getId());
                } else if (person.isFemale()) {
                    sibling.getSisterIds().add(person.getId());
                }
            }
        }
    }

    public void checkConsistency(Map<String, Person> people) {
        System.out.println("Проверка консистентности данных...");
        int inconsistencies = 0;

        for (Person person : people.values()) {
            int actualChildren = person.getChildIds().size() + person.getChildNames().size();
            int actualSiblings = person.getSiblingIds().size() + person.getSiblingNames().size();

            if (person.getExpectedChildren() != null) {
                if (actualChildren != person.getExpectedChildren()) {
                    System.err.printf("Несоответствие детей: %s (ID: %s) - ожидалось %d, найдено %d%n",
                            person.getBestName(), person.getId(), person.getExpectedChildren(), actualChildren);
                    inconsistencies++;
                }
            }

            if (person.getExpectedSiblings() != null) {
                if (actualSiblings != person.getExpectedSiblings()) {
                    System.err.printf("Несоответствие братьев/сестер: %s (ID: %s) - ожидалось %d, найдено %d%n",
                            person.getBestName(), person.getId(), person.getExpectedSiblings(), actualSiblings);
                    inconsistencies++;
                }
            }
        }

        if (inconsistencies > 0) {
            System.err.println("Обнаружено несоответствий: " + inconsistencies);
        } else {
            System.out.println("Консистентность данных подтверждена.");
        }
    }

    private String normalizeName(String name) {
        return name.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}