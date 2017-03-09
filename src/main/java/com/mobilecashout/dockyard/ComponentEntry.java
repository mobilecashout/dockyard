package com.mobilecashout.dockyard;

import java.util.HashSet;
import java.util.Set;

class ComponentEntry {
    private final String actualClassName;
    private String namedValue;
    private Set<String> containers = new HashSet<>();

    ComponentEntry(final String actualClassName) {
        this.actualClassName = actualClassName;
    }

    void setNamedValue(final String namedValue) {
        this.namedValue = namedValue;
    }

    void addContainer(final String iClass) {
        containers.add(iClass);
    }

    String getNamedValue() {
        return namedValue;
    }

    Set<String> getContainers() {
        return containers;
    }

    String getActualClassName() {
        return actualClassName;
    }
}
