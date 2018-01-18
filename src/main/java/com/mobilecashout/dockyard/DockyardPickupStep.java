package com.mobilecashout.dockyard;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static com.mobilecashout.dockyard.DockyardComponentProcessor.GENERATED_SOURCES;
import static java.util.stream.IntStream.range;

class DockyardPickupStep implements BasicAnnotationProcessor.ProcessingStep {
    private static final int LIMIT_ITEMS = 65534;
    private static final String MEMORY_SCHEME = "mem";
    private final ProcessingEnvironment processingEnv;

    DockyardPickupStep(final ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public Set<? extends Class<? extends Annotation>> annotations() {
        return Collections.singleton(Dockyard.class);
    }

    @Override
    public Set<Element> process(final SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
        final ComponentEntrySet entrySet = new ComponentEntrySet();

        elementsByAnnotation.get(Dockyard.class).forEach(element -> {
            processAnnotatedElement(entrySet, element);
        });

        final List<JavaFileObject> javaFileObjects = new ArrayList<>();
        final List<String> containers = entrySet.uniqueContainers();

        for (final String container : containers) {
            final List<ComponentEntry> componentEntries = entrySet.entriesForContainer(container);

            if (LIMIT_ITEMS <= componentEntries.size()) {
                throw new RuntimeException("No more than 65,533 Dockyard entries allowed due to limitation of how many " +
                        "fields a class can have in JVM");
            }

            final JavaFileObject javaFileObject = createContainer(
                    container,
                    componentEntries
            );

            javaFileObjects.add(javaFileObject);
        }

        final Messager messager = processingEnv.getMessager();

        for (final JavaFileObject javaFileObject : javaFileObjects) {
            try {
                final URI javaFileUri = javaFileObject.toUri();

                messager.printMessage(Diagnostic.Kind.NOTE, String.format("Generated: %s", javaFileUri));

                if (javaFileUri.getScheme().equals(MEMORY_SCHEME)) {
                    messager.printMessage(
                            Diagnostic.Kind.NOTE,
                            "Test compilation, skipping confirmation..."
                    );
                    continue;
                }

                final File javaFileFile = new File(javaFileUri);

                if (!javaFileFile.exists()) {
                    throw new RuntimeException(String.format(
                            "File does not exist, expected generated source: %s",
                            javaFileFile
                    ));
                }

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        return Collections.emptySet();
    }


    private void processAnnotatedElement(final ComponentEntrySet entrySet, final Element typeElement) {
        final ComponentEntry componentEntry = new ComponentEntry(
                ((TypeElement) typeElement).getQualifiedName().toString()
        );

        entrySet.add(componentEntry);

        final List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();
        final Name dockyardName = processingEnv.getElementUtils().getName(Dockyard.class.getName());

        for (final AnnotationMirror annotationMirror : annotationMirrors) {
            final Name currentName = processingEnv
                    .getElementUtils()
                    .getBinaryName((TypeElement) annotationMirror.getAnnotationType().asElement());

            if (!currentName.equals(dockyardName)) {
                continue;
            }

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

    private synchronized JavaFileObject createContainer(
            final String container,
            final List<ComponentEntry> componentEntries
    ) {
        final ClassName containerClass = ClassName.bestGuess(container);
        final ClassName containerDockyardClass = ClassName.bestGuess(String.format(
                "%sDockyard",
                container
        ));
        final String dockyardClass = containerDockyardClass.toString();

        if (GENERATED_SOURCES.containsKey(dockyardClass)) {
            return GENERATED_SOURCES.get(dockyardClass);
        }

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
                .addModifiers(Modifier.PRIVATE)
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
            final Filer filer = processingEnv.getFiler();

            final JavaFileObject classFile;

            synchronized (GENERATED_SOURCES) {
                classFile = filer.createSourceFile(dockyardClass);
                GENERATED_SOURCES.put(dockyardClass, classFile);
            }

            final Writer writer = classFile.openWriter();
            writer.write("");

            javaFile.writeTo(writer);

            writer.flush();
            writer.close();

            return classFile;

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
