package com.mobilecashout.dockyard;

import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.inject.Named;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("com.mobilecashout.dockyard.Dockyard")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DockyardComponentProcessor extends AbstractProcessor {
    @Override
    public boolean process(
            final Set<? extends TypeElement> annotations,
            final RoundEnvironment roundEnv
    ) {
        final ComponentEntrySet entrySet = new ComponentEntrySet();

        for (final TypeElement annotation : annotations) {
            final Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(annotation);
            for (final Element typeElement : elementsAnnotatedWith) {
                final ComponentEntry componentEntry = new ComponentEntry(
                        ((TypeElement) typeElement).getQualifiedName().toString()
                );
                entrySet.add(componentEntry);

                final List<? extends AnnotationMirror> annotationMirrors = typeElement.getAnnotationMirrors();

                for (final AnnotationMirror annotationMirror : annotationMirrors) {

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
            }
        }

        for (final String container : entrySet.uniqueContainers()) {
            createContainer(container, entrySet.entriesForContainer(container));
        }

        return true;
    }

    private void createContainer(final String container, final ComponentEntry[] componentEntries) {
        final ClassName containerClass = ClassName.bestGuess(container);
        final ClassName containerDockyardClass = ClassName.bestGuess(String.format(
                "%sDockyard",
                container
        ));

        final MethodSpec.Builder constructorBuilder = MethodSpec
                .constructorBuilder()
                .addAnnotation(Inject.class)
                .addModifiers(Modifier.PUBLIC);

        int i = 0;
        for (final ComponentEntry componentEntry : componentEntries) {
            final String argName = String.format("a%d", i);
            final ParameterSpec.Builder injectableParameterBuilder = ParameterSpec
                    .builder(ClassName.bestGuess(componentEntry.getActualClassName()), argName)
                    .addModifiers(Modifier.FINAL);

            if (null != componentEntry.getNamedValue() && componentEntry.getNamedValue().length() > 0) {
                final AnnotationSpec namedSpec = AnnotationSpec
                        .builder(Named.class)
                        .addMember("value", "$S", componentEntry.getNamedValue())
                        .build();

                injectableParameterBuilder.addAnnotation(namedSpec);
            }

            constructorBuilder
                    .addParameter(injectableParameterBuilder.build());
            i++;
        }

        constructorBuilder
                .addStatement("this.$N = new $T[]{$L}", "instances", containerClass, getArgList(i));

        final FieldSpec instances = FieldSpec
                .builder(ArrayTypeName.of(containerClass), "instances", Modifier.PROTECTED, Modifier.FINAL)
                .build();

        final MethodSpec getMethodSpec = MethodSpec.methodBuilder("getAll")
                .addModifiers(Modifier.PUBLIC)
                .returns(ArrayTypeName.of(containerClass))
                .addStatement("return this.$N", "instances")
                .build();

        final TypeSpec dockyardContainer = TypeSpec.classBuilder(containerDockyardClass)
                .addSuperinterface(DockyardContainer.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(constructorBuilder.build())
                .addMethod(getMethodSpec)
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

    private String getArgList(final int argCount) {
        final String[] argListArray = new String[argCount];

        for (int i = 0; i < argCount; i++) {
            argListArray[i] = String.format("a%d", i);
        }

        return String.join(", ", argListArray);
    }
}
