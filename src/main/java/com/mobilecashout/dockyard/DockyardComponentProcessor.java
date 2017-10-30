package com.mobilecashout.dockyard;

import com.google.auto.common.BasicAnnotationProcessor;

import java.util.Collections;

public class DockyardComponentProcessor extends BasicAnnotationProcessor {
    @Override
    protected Iterable<? extends ProcessingStep> initSteps() {
        return Collections.singletonList(
                new DockyardPickupStep(processingEnv)
        );
    }
}
