package com.mobilecashout.dockyard;

import com.google.auto.common.BasicAnnotationProcessor;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.JavaFileObject;
import java.util.Collections;
import java.util.HashMap;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DockyardComponentProcessor extends BasicAnnotationProcessor {
    final static HashMap<String, JavaFileObject> GENERATED_SOURCES = new HashMap<>();

    @Override
    protected Iterable<? extends ProcessingStep> initSteps() {
        return Collections.singletonList(
                new DockyardPickupStep(processingEnv)
        );
    }

    @Override
    protected void postRound(RoundEnvironment roundEnv) {
        super.postRound(roundEnv);
        GENERATED_SOURCES.clear();
    }
}
