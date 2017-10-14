package com.mobilecashout.dockyard;

import java.util.*;
import java.util.stream.Collectors;

class ComponentEntrySet {
    private final Set<ComponentEntry> entrySet = new HashSet<>();

    void add(final ComponentEntry componentEntry) {
        entrySet.add(componentEntry);
    }

    List<String> uniqueContainers() {
        final Set<String> containers = new HashSet<>();
        entrySet.forEach(entry -> containers.addAll(entry.getContainers()));
        final ArrayList<String> names = new ArrayList<>(containers);
        names.sort(Comparator.naturalOrder());
        return names;
    }

    List<ComponentEntry> entriesForContainer(final String container) {
        return entrySet
                .stream()
                .filter(entry -> entry.getContainers().contains(container))
                .sorted(Comparator.comparing(ComponentEntry::getActualClassName))
                .collect(Collectors.toList());
    }
}
