package com.mobilecashout.dockyard;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.junit.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

public class ComponentProcessorTest {
    @Test
    public void it_compiles_container() {
        final Compilation compilation = javac()
                .withProcessors(new DockyardComponentProcessor())
                .withOptions()
                .compile(JavaFileObjects.forResource("Test.java"));

        assertThat(compilation).succeeded();

        assertThat(compilation)
                .generatedSourceFile("com/mobilecashout/test/InterfaceADockyard")
                .hasSourceEquivalentTo(JavaFileObjects.forResource("GeneratedInterfaceADockyard.java"));

        assertThat(compilation)
                .generatedSourceFile("com/mobilecashout/test/InterfaceBDockyard")
                .hasSourceEquivalentTo(JavaFileObjects.forResource("GeneratedInterfaceBDockyard.java"));
    }
}