package org.example.writer;

import org.example.model.Person;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class XmlWriter {

    public void write(Map<String, Person> people, Path output) throws Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();

        try (OutputStream os = Files.newOutputStream(output)) {
            XMLStreamWriter writer = factory.createXMLStreamWriter(os, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("people");
            writer.writeAttribute("count", String.valueOf(people.size()));

            // Сортируем по ID для читаемости
            List<Person> sortedPeople = new ArrayList<>(people.values());
            sortedPeople.sort(Comparator.comparing(Person::getId));

            for (Person person : sortedPeople) {
                writer.writeStartElement("person");
                writer.writeAttribute("id", person.getId());

                // Имя - только если есть данные
                boolean hasNameData = person.getFirstName() != null ||
                        person.getLastName() != null ||
                        person.getFullName() != null;

                if (hasNameData) {
                    writer.writeStartElement("name");
                    if (person.getFirstName() != null) {
                        writer.writeStartElement("first");
                        writer.writeCharacters(person.getFirstName());
                        writer.writeEndElement();
                    }
                    if (person.getLastName() != null) {
                        writer.writeStartElement("last");
                        writer.writeCharacters(person.getLastName());
                        writer.writeEndElement();
                    }
                    if (person.getFullName() != null) {
                        writer.writeStartElement("full");
                        writer.writeCharacters(person.getFullName());
                        writer.writeEndElement();
                    }
                    writer.writeEndElement(); // name
                }

                writer.writeEmptyElement("gender");
                writer.writeAttribute("value", person.getGender());

                // Супруги
                writeSpouses(writer, person, people);

                // Родители
                writeParents(writer, person, people);

                // Дети
                writeChildren(writer, person, people);

                // Братья и сестры
                writeSiblings(writer, person, people);

                writer.writeEmptyElement("expected");
                int actualChildren = person.getChildIds().size() + person.getChildNames().size();
                int actualSiblings = person.getSiblingIds().size() + person.getSiblingNames().size();

                writer.writeAttribute("children", String.valueOf(
                        person.getExpectedChildren() != null ?
                                person.getExpectedChildren() : actualChildren
                ));

                writer.writeAttribute("siblings", String.valueOf(
                        person.getExpectedSiblings() != null ?
                                person.getExpectedSiblings() : actualSiblings
                ));

                writer.writeEndElement(); // person
            }

            writer.writeEndElement(); // people
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }

        System.out.println("Финальный XML создан: " + output.toAbsolutePath());
    }

    private void writeSpouses(XMLStreamWriter writer, Person person, Map<String, Person> people) throws Exception {
        if (!person.getSpouseIds().isEmpty() || !person.getSpouseNames().isEmpty()) {
            writer.writeStartElement("spouses");

            // Сначала по ID
            for (String spouseId : person.getSpouseIds()) {
                Person spouse = people.get(spouseId);
                writer.writeEmptyElement("spouse");
                if (spouse != null && spouse.getFullName() != null) {
                    writer.writeAttribute("name", spouse.getFullName());
                }
                writer.writeAttribute("id", spouseId);
            }

            // Затем по именам
            for (String spouseName : person.getSpouseNames()) {
                writer.writeStartElement("spouse");
                writer.writeCharacters(spouseName);
                writer.writeEndElement();
            }

            writer.writeEndElement(); // spouses
        }
    }

    private void writeParents(XMLStreamWriter writer, Person person, Map<String, Person> people) throws Exception {
        boolean hasParents = !person.getParentIds().isEmpty() ||
                !person.getParentNames().isEmpty() ||
                !person.getFatherIds().isEmpty() ||
                !person.getMotherIds().isEmpty() ||
                !person.getFatherNames().isEmpty() ||
                !person.getMotherNames().isEmpty();

        if (hasParents) {
            writer.writeStartElement("parents");

            // Отцы по ID
            for (String fatherId : person.getFatherIds()) {
                Person father = people.get(fatherId);
                writer.writeEmptyElement("father");
                if (father != null && father.getFullName() != null) {
                    writer.writeAttribute("name", father.getFullName());
                }
                writer.writeAttribute("id", fatherId);
            }

            // Матери по ID
            for (String motherId : person.getMotherIds()) {
                Person mother = people.get(motherId);
                writer.writeEmptyElement("mother");
                if (mother != null && mother.getFullName() != null) {
                    writer.writeAttribute("name", mother.getFullName());
                }
                writer.writeAttribute("id", motherId);
            }

            // Остальные родители по ID
            for (String parentId : person.getParentIds()) {
                if (!person.getFatherIds().contains(parentId) &&
                        !person.getMotherIds().contains(parentId)) {
                    Person parent = people.get(parentId);
                    writer.writeEmptyElement("parent");
                    if (parent != null && parent.getFullName() != null) {
                        writer.writeAttribute("name", parent.getFullName());
                    }
                    writer.writeAttribute("id", parentId);
                }
            }

            // Отцы по именам
            for (String fatherName : person.getFatherNames()) {
                writer.writeStartElement("father");
                writer.writeCharacters(fatherName);
                writer.writeEndElement();
            }

            // Матери по именам
            for (String motherName : person.getMotherNames()) {
                writer.writeStartElement("mother");
                writer.writeCharacters(motherName);
                writer.writeEndElement();
            }

            // Остальные родители по именам
            for (String parentName : person.getParentNames()) {
                writer.writeStartElement("parent");
                writer.writeCharacters(parentName);
                writer.writeEndElement();
            }

            writer.writeEndElement(); // parents
        }
    }

    private void writeChildren(XMLStreamWriter writer, Person person, Map<String, Person> people) throws Exception {
        boolean hasChildren = !person.getChildIds().isEmpty() ||
                !person.getChildNames().isEmpty() ||
                !person.getSonIds().isEmpty() ||
                !person.getDaughterIds().isEmpty() ||
                !person.getSonNames().isEmpty() ||
                !person.getDaughterNames().isEmpty();

        if (hasChildren) {
            writer.writeStartElement("children");

            // Определяем пол детей по ID
            for (String childId : person.getChildIds()) {
                Person child = people.get(childId);
                String gender = child != null ? child.getGender() : "unknown";

                if ("male".equals(gender)) {
                    writer.writeEmptyElement("son");
                } else if ("female".equals(gender)) {
                    writer.writeEmptyElement("daughter");
                } else {
                    writer.writeEmptyElement("child");
                }

                if (child != null && child.getFullName() != null) {
                    writer.writeAttribute("name", child.getFullName());
                }
                writer.writeAttribute("id", childId);
            }

            // Сыновья по ID
            for (String sonId : person.getSonIds()) {
                Person son = people.get(sonId);
                writer.writeEmptyElement("son");
                if (son != null && son.getFullName() != null) {
                    writer.writeAttribute("name", son.getFullName());
                }
                writer.writeAttribute("id", sonId);
            }

            // Дочери по ID
            for (String daughterId : person.getDaughterIds()) {
                Person daughter = people.get(daughterId);
                writer.writeEmptyElement("daughter");
                if (daughter != null && daughter.getFullName() != null) {
                    writer.writeAttribute("name", daughter.getFullName());
                }
                writer.writeAttribute("id", daughterId);
            }

            // Дети по именам
            for (String childName : person.getChildNames()) {
                writer.writeStartElement("child");
                writer.writeCharacters(childName);
                writer.writeEndElement();
            }

            // Сыновья по именам
            for (String sonName : person.getSonNames()) {
                writer.writeStartElement("son");
                writer.writeCharacters(sonName);
                writer.writeEndElement();
            }

            // Дочери по именам
            for (String daughterName : person.getDaughterNames()) {
                writer.writeStartElement("daughter");
                writer.writeCharacters(daughterName);
                writer.writeEndElement();
            }

            writer.writeEndElement(); // children
        }
    }

    private void writeSiblings(XMLStreamWriter writer, Person person, Map<String, Person> people) throws Exception {
        boolean hasSiblings = !person.getSiblingIds().isEmpty() ||
                !person.getSiblingNames().isEmpty() ||
                !person.getBrotherIds().isEmpty() ||
                !person.getSisterIds().isEmpty() ||
                !person.getBrotherNames().isEmpty() ||
                !person.getSisterNames().isEmpty();

        if (hasSiblings) {
            writer.writeStartElement("siblings");

            // Определяем пол братьев/сестер по ID
            for (String siblingId : person.getSiblingIds()) {
                Person sibling = people.get(siblingId);
                String gender = sibling != null ? sibling.getGender() : "unknown";

                if ("male".equals(gender)) {
                    writer.writeEmptyElement("brother");
                } else if ("female".equals(gender)) {
                    writer.writeEmptyElement("sister");
                } else {
                    writer.writeEmptyElement("sibling");
                }

                if (sibling != null && sibling.getFullName() != null) {
                    writer.writeAttribute("name", sibling.getFullName());
                }
                writer.writeAttribute("id", siblingId);
            }

            // Братья по ID
            for (String brotherId : person.getBrotherIds()) {
                Person brother = people.get(brotherId);
                writer.writeEmptyElement("brother");
                if (brother != null && brother.getFullName() != null) {
                    writer.writeAttribute("name", brother.getFullName());
                }
                writer.writeAttribute("id", brotherId);
            }

            // Сестры по ID
            for (String sisterId : person.getSisterIds()) {
                Person sister = people.get(sisterId);
                writer.writeEmptyElement("sister");
                if (sister != null && sister.getFullName() != null) {
                    writer.writeAttribute("name", sister.getFullName());
                }
                writer.writeAttribute("id", sisterId);
            }

            // Братья/сестры по именам
            for (String siblingName : person.getSiblingNames()) {
                writer.writeStartElement("sibling");
                writer.writeCharacters(siblingName);
                writer.writeEndElement();
            }

            // Братья по именам
            for (String brotherName : person.getBrotherNames()) {
                writer.writeStartElement("brother");
                writer.writeCharacters(brotherName);
                writer.writeEndElement();
            }

            // Сестры по именам
            for (String sisterName : person.getSisterNames()) {
                writer.writeStartElement("sister");
                writer.writeCharacters(sisterName);
                writer.writeEndElement();
            }

            writer.writeEndElement(); // siblings
        }
    }
}