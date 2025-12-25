package org.example.model;

import java.util.*;

public class Person {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String gender = "unknown";

    // Связи по именам (изначальные данные)
    private Set<String> spouseNames = new LinkedHashSet<>();
    private Set<String> parentNames = new LinkedHashSet<>();
    private Set<String> childNames = new LinkedHashSet<>();
    private Set<String> siblingNames = new LinkedHashSet<>();

    // Специфичные связи по именам
    private Set<String> fatherNames = new LinkedHashSet<>();
    private Set<String> motherNames = new LinkedHashSet<>();
    private Set<String> brotherNames = new LinkedHashSet<>();
    private Set<String> sisterNames = new LinkedHashSet<>();
    private Set<String> sonNames = new LinkedHashSet<>();
    private Set<String> daughterNames = new LinkedHashSet<>();

    // Связи по ID (после обработки)
    private Set<String> spouseIds = new LinkedHashSet<>();
    private Set<String> parentIds = new LinkedHashSet<>();
    private Set<String> childIds = new LinkedHashSet<>();
    private Set<String> siblingIds = new LinkedHashSet<>();

    // Специфичные связи по ID
    private Set<String> fatherIds = new LinkedHashSet<>();
    private Set<String> motherIds = new LinkedHashSet<>();
    private Set<String> brotherIds = new LinkedHashSet<>();
    private Set<String> sisterIds = new LinkedHashSet<>();
    private Set<String> sonIds = new LinkedHashSet<>();
    private Set<String> daughterIds = new LinkedHashSet<>();

    // Для проверки консистентности
    private Integer expectedChildren;
    private Integer expectedSiblings;

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Set<String> getSpouseNames() { return spouseNames; }
    public void setSpouseNames(Set<String> spouseNames) { this.spouseNames = spouseNames; }

    public Set<String> getParentNames() { return parentNames; }
    public void setParentNames(Set<String> parentNames) { this.parentNames = parentNames; }

    public Set<String> getChildNames() { return childNames; }
    public void setChildNames(Set<String> childNames) { this.childNames = childNames; }

    public Set<String> getSiblingNames() { return siblingNames; }
    public void setSiblingNames(Set<String> siblingNames) { this.siblingNames = siblingNames; }

    public Set<String> getFatherNames() { return fatherNames; }
    public void setFatherNames(Set<String> fatherNames) { this.fatherNames = fatherNames; }

    public Set<String> getMotherNames() { return motherNames; }
    public void setMotherNames(Set<String> motherNames) { this.motherNames = motherNames; }

    public Set<String> getBrotherNames() { return brotherNames; }
    public void setBrotherNames(Set<String> brotherNames) { this.brotherNames = brotherNames; }

    public Set<String> getSisterNames() { return sisterNames; }
    public void setSisterNames(Set<String> sisterNames) { this.sisterNames = sisterNames; }

    public Set<String> getSonNames() { return sonNames; }
    public void setSonNames(Set<String> sonNames) { this.sonNames = sonNames; }

    public Set<String> getDaughterNames() { return daughterNames; }
    public void setDaughterNames(Set<String> daughterNames) { this.daughterNames = daughterNames; }

    public Set<String> getSpouseIds() { return spouseIds; }
    public void setSpouseIds(Set<String> spouseIds) { this.spouseIds = spouseIds; }

    public Set<String> getParentIds() { return parentIds; }
    public void setParentIds(Set<String> parentIds) { this.parentIds = parentIds; }

    public Set<String> getChildIds() { return childIds; }
    public void setChildIds(Set<String> childIds) { this.childIds = childIds; }

    public Set<String> getSiblingIds() { return siblingIds; }
    public void setSiblingIds(Set<String> siblingIds) { this.siblingIds = siblingIds; }

    public Set<String> getFatherIds() { return fatherIds; }
    public void setFatherIds(Set<String> fatherIds) { this.fatherIds = fatherIds; }

    public Set<String> getMotherIds() { return motherIds; }
    public void setMotherIds(Set<String> motherIds) { this.motherIds = motherIds; }

    public Set<String> getBrotherIds() { return brotherIds; }
    public void setBrotherIds(Set<String> brotherIds) { this.brotherIds = brotherIds; }

    public Set<String> getSisterIds() { return sisterIds; }
    public void setSisterIds(Set<String> sisterIds) { this.sisterIds = sisterIds; }

    public Set<String> getSonIds() { return sonIds; }
    public void setSonIds(Set<String> sonIds) { this.sonIds = sonIds; }

    public Set<String> getDaughterIds() { return daughterIds; }
    public void setDaughterIds(Set<String> daughterIds) { this.daughterIds = daughterIds; }

    public Integer getExpectedChildren() { return expectedChildren; }
    public void setExpectedChildren(Integer expectedChildren) { this.expectedChildren = expectedChildren; }

    public Integer getExpectedSiblings() { return expectedSiblings; }
    public void setExpectedSiblings(Integer expectedSiblings) { this.expectedSiblings = expectedSiblings; }

    // Вспомогательные методы
    public String getBestName() {
        if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
        if (firstName != null && lastName != null) return firstName + " " + lastName;
        if (firstName != null) return firstName;
        if (lastName != null) return lastName;
        return id != null ? id : "Unknown";
    }

    public boolean isMale() { return "male".equals(gender); }
    public boolean isFemale() { return "female".equals(gender); }

    // Метод для слияния с другим объектом Person (при объединении дубликатов)
    public void merge(Person other) {
        if (this.id == null && other.id != null) this.id = other.id;
        if (this.firstName == null && other.firstName != null) this.firstName = other.firstName;
        if (this.lastName == null && other.lastName != null) this.lastName = other.lastName;
        if (this.fullName == null && other.fullName != null) this.fullName = other.fullName;

        if ("unknown".equals(this.gender) && !"unknown".equals(other.gender)) {
            this.gender = other.gender;
        }

        this.spouseNames.addAll(other.spouseNames);
        this.parentNames.addAll(other.parentNames);
        this.childNames.addAll(other.childNames);
        this.siblingNames.addAll(other.siblingNames);

        this.fatherNames.addAll(other.fatherNames);
        this.motherNames.addAll(other.motherNames);
        this.brotherNames.addAll(other.brotherNames);
        this.sisterNames.addAll(other.sisterNames);
        this.sonNames.addAll(other.sonNames);
        this.daughterNames.addAll(other.daughterNames);

        this.spouseIds.addAll(other.spouseIds);
        this.parentIds.addAll(other.parentIds);
        this.childIds.addAll(other.childIds);
        this.siblingIds.addAll(other.siblingIds);

        this.fatherIds.addAll(other.fatherIds);
        this.motherIds.addAll(other.motherIds);
        this.brotherIds.addAll(other.brotherIds);
        this.sisterIds.addAll(other.sisterIds);
        this.sonIds.addAll(other.sonIds);
        this.daughterIds.addAll(other.daughterIds);

        if (this.expectedChildren == null && other.expectedChildren != null) {
            this.expectedChildren = other.expectedChildren;
        }
        if (this.expectedSiblings == null && other.expectedSiblings != null) {
            this.expectedSiblings = other.expectedSiblings;
        }
    }
}