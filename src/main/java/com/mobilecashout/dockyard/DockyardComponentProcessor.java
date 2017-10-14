package com.mobilecashout.dockyard;

import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.range;

@SupportedAnnotationTypes("com.mobilecashout.dockyard.Dockyard")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DockyardComponentProcessor extends AbstractProcessor {
    private static final int EXEC_AWAIT_MINUTES = 60;
    private static final int LIMIT_ITEMS = 65534;

    @Override
    public boolean process(
            final Set<? extends TypeElement> annotations,
            final RoundEnvironment roundEnv
    ) {
        final ComponentEntrySet entrySet = new ComponentEntrySet();

        for (final TypeElement annotation : annotations) {
            processTypeElement(roundEnv, entrySet, annotation);
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime
                .getRuntime()
                .availableProcessors());

        for (final String container : entrySet.uniqueContainers()) {
            final List<ComponentEntry> componentEntries = entrySet.entriesForContainer(container);

            if (LIMIT_ITEMS <= componentEntries.size()) {
                throw new RuntimeException("No more than 65,533 Dockyard entries allowed due to limitation of how many " +
                        "fields a class can have in JVM");
            }

            executorService.submit(() -> createContainer(container, componentEntries));
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(EXEC_AWAIT_MINUTES, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private void processTypeElement(
            final RoundEnvironment roundEnv,
            final ComponentEntrySet entrySet,
            final TypeElement annotation
    ) {
        final Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(annotation);

        for (final Element typeElement : elementsAnnotatedWith) {
            processTypeElement(entrySet, typeElement);
        }
    }

    private void processTypeElement(final ComponentEntrySet entrySet, final Element typeElement) {
        final ComponentEntry componentEntry = new ComponentEntry(
                ((TypeElement) typeElement).getQualifiedName().toString()
        );

        entrySet.add(componentEntry);

        final List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();

        for (final AnnotationMirror annotationMirror : annotationMirrors) {

            processAnnotationMirror(componentEntry, annotationMirror);
        }
    }

    private void processAnnotationMirror(final ComponentEntry componentEntry, final AnnotationMirror annotationMirror) {
        final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();

        for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {

            final String annotationKey = entry.getKey().getSimpleName().toString();
            final Object annotationValue = entry.getValue().getValue();

            switch (annotationKey) {
                case "name":
                    componentEntry.setNamedValue(annotationValue.toString());
                    break;
                case "value":
                    final List<? extends AnnotationValue> typeMirrors = (List<? extends AnnotationValue>) annotationValue;
                    for (final AnnotationValue typeMirror : typeMirrors) {
                        componentEntry.addContainer(typeMirror.getValue().toString());
                    }
                    break;
            }
        }
    }

    private void createContainer(final String container, final List<ComponentEntry> componentEntries) {
        final ClassName containerClass = ClassName.bestGuess(container);
        final ClassName containerDockyardClass = ClassName.bestGuess(String.format(
                "%sDockyard",
                container
        ));

        final List<FieldSpec> fieldsToInject = new ArrayList<>();

        int i = 0;
        for (final ComponentEntry componentEntry : componentEntries) {
            final String toInjectFieldName = String.format("a%d", i);
            final ClassName toInjectClass = ClassName.bestGuess(componentEntry.getActualClassName());

            final List<AnnotationSpec> annotations = new ArrayList<>();

            annotations.add(AnnotationSpec.builder(Inject.class).build());

            final String namedValue = componentEntry.getNamedValue();

            if (null != namedValue && namedValue.length() > 0) {
                final AnnotationSpec build = AnnotationSpec
                        .builder(Named.class)
                        .addMember("value", "$S", namedValue)
                        .build();

                annotations.add(build);
            }

            final FieldSpec local = FieldSpec
                    .builder(toInjectClass, toInjectFieldName, Modifier.PROTECTED)
                    .addAnnotations(annotations)
                    .build();

            fieldsToInject.add(local);
            i++;
        }

        final String injectedFieldsConstruct = range(0, componentEntries.size())
                .mapToObj(ii -> String.format("a%d", ii))
                .collect(Collectors.joining(","));

        final ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(
                ClassName.get(List.class),
                containerClass
        );
        final FieldSpec instances = FieldSpec
                .builder(parameterizedTypeName, "instances")
                .addModifiers(Modifier.PROTECTED)
                .initializer("null")
                .build();

        final MethodSpec getAllMethodSpec = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC)
                .returns(parameterizedTypeName)
                .beginControlFlow("if (null == instances)")
                .addStatement(
                        "instances = $T.unmodifiableList($T.asList(new $T {" + injectedFieldsConstruct + "}))",
                        ClassName.get(Collections.class),
                        ClassName.get(Arrays.class),
                        ArrayTypeName.of(containerClass)
                )
                .endControlFlow()
                .addStatement("return $N", "instances")
                .build();

        final MethodSpec constructorMethodSpec = MethodSpec
                .constructorBuilder()
                .addAnnotation(Inject.class)
                .addModifiers(Modifier.PUBLIC)
                .build();

        final TypeSpec dockyardContainer = TypeSpec.classBuilder(containerDockyardClass)
                .addSuperinterface(DockyardContainer.class)
                .addAnnotation(Singleton.class)
                .addModifiers(Modifier.PUBLIC)
                .addFields(fieldsToInject)
                .addMethod(constructorMethodSpec)
                .addMethod(getAllMethodSpec)
                .addField(instances)
                .build();

        final JavaFile javaFile = JavaFile
                .builder(containerDockyardClass.packageName(), dockyardContainer)
                .build();

        try {
            final JavaFileObject classFile = processingEnv
                    .getFiler()
                    .createSourceFile(containerDockyardClass.toString());

            final Writer writer = classFile.openWriter();

            javaFile.writeTo(writer);

            writer.flush();
            writer.close();

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
