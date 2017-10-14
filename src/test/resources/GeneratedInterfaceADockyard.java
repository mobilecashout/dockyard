package com.mobilecashout.test;

import com.mobilecashout.dockyard.DockyardContainer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class InterfaceADockyard implements DockyardContainer {
    @Inject
    @Named("hello")
    protected TestA a0;

    @Inject
    @Named("world")
    protected TestB a1;

    protected List<InterfaceA> instances = null;

    @Inject
    public InterfaceADockyard() {
    }

    public List<InterfaceA> getAll() {
        if (null == instances) {
            instances = Collections.unmodifiableList(Arrays.asList(new InterfaceA[] {a0,a1}));
        }
        return instances;
    }
}
