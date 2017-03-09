package com.mobilecashout.test;

import com.mobilecashout.dockyard.DockyardContainer;
import javax.inject.Inject;
import javax.inject.Named;

public class InterfaceADockyard implements DockyardContainer {
    protected final InterfaceA[] instances;

    @Inject
    public InterfaceADockyard(@Named("hello") final TestA a0, @Named("world") final TestB a1) {
        this.instances = new InterfaceA[]{a0, a1};
    }

    public InterfaceA[] getAll() {
        return this.instances;
    }
}