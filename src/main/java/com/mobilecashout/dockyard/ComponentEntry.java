package com.mobilecashout.dockyard;

import java.util.HashSet;
import java.util.Objects;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ComponentEntry)) return false;
        final ComponentEntry that = (ComponentEntry) o;
        return Objects.equals(actualClassName, that.actualClassName) &&
                Objects.equals(namedValue, that.namedValue) &&
                Objects.equals(containers, that.containers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actualClassName, namedValue, containers);
    }
}
