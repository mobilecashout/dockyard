package com.mobilecashout.dockyard;

import java.util.HashSet;
import java.util.Set;

class ComponentEntrySet {
    private final Set<ComponentEntry> entrySet = new HashSet<>();

    void add(final ComponentEntry componentEntry) {
        entrySet.add(componentEntry);
    }

    String[] uniqueContainers() {
        final Set<String> containers = new HashSet<>();
        entrySet.forEach(entry -> containers.addAll(entry.getContainers()));
        return containers.toArray(new String[0]);
    }

    ComponentEntry[] entriesForContainer(final String container) {
        return entrySet
                .stream()
                .filter(entry -> entry.getContainers().contains(container))
                .toArray(ComponentEntry[]::new);
    }
}
