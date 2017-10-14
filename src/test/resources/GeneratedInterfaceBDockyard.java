package com.mobilecashout.test;

import com.mobilecashout.dockyard.DockyardContainer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class InterfaceBDockyard implements DockyardContainer {
    @Inject
    @Named("hello")
    protected TestA a0;

    protected List<InterfaceB> instances = null;

    @Inject
    public InterfaceBDockyard() {
    }

    public List<InterfaceB> getAll() {
        if (null == instances) {
            instances = Collections.unmodifiableList(Arrays.asList(new InterfaceB[] {a0}));
        }
        return instances;
    }
}
