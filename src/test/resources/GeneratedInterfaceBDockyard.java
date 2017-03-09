package com.mobilecashout.test;

import com.mobilecashout.dockyard.DockyardContainer;
import javax.inject.Inject;
import javax.inject.Named;

public class InterfaceBDockyard implements DockyardContainer {
    protected final InterfaceB[] instances;

    @Inject
    public InterfaceBDockyard(@Named("hello") final TestA a0) {
        this.instances = new InterfaceB[]{a0};
    }

    public InterfaceB[] getAll() {
        return this.instances;
    }
}