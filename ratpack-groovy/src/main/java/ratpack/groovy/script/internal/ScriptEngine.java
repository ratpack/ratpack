/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.groovy.script.internal;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.CompileStatic;
import groovy.transform.InheritConstructors;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.function.Consumer;

public class ScriptEngine<T extends Script> {

  private static final ClassNode LINE_NUMBER_CLASS_NODE = new ClassNode(LineNumber.class);

  private final boolean staticCompile;
  private final Class<T> scriptBaseClass;
  private ClassLoader parentLoader;

  public ScriptEngine(ClassLoader parentLoader, boolean staticCompile, Class<T> scriptBaseClass) {
    this.parentLoader = parentLoader;
    this.staticCompile = staticCompile;
    this.scriptBaseClass = scriptBaseClass;
  }

  public T create(String scriptName, Path scriptPath, String scriptText, Object... scriptConstructionArgs) throws IllegalAccessException, InstantiationException {
    Class<T> scriptClass = compile(scriptName, scriptPath, scriptText);
    return DefaultGroovyMethods.newInstance(scriptClass, scriptConstructionArgs);
  }

  @SuppressWarnings("unchecked")
  public Class<T> compile(String scriptName, String scriptText) throws IllegalAccessException, InstantiationException {
    return createClassLoader(null).parseClass(scriptText, scriptName);
  }

  @SuppressWarnings("unchecked")
  public Class<T> compile(String scriptName, Path scriptPath, String scriptText) throws IllegalAccessException, InstantiationException {
    return createClassLoader(scriptPath).parseClass(scriptText, scriptName);
  }

  private GroovyClassLoader createClassLoader(final Path scriptPath) {
    final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
    if (!scriptBaseClass.equals(Script.class)) {
      compilerConfiguration.setScriptBaseClass(scriptBaseClass.getName());
    }
    compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        if (staticCompile) {
          classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
        }
        classNode.addAnnotation(new AnnotationNode(new ClassNode(InheritConstructors.class)));
        if (scriptPath != null) {
          AnnotationNode scriptPathAnnotation = new AnnotationNode(new ClassNode(ScriptPath.class));
          scriptPathAnnotation.addMember("value", new ConstantExpression(scriptPath.toUri().toString()));
          classNode.addAnnotation(scriptPathAnnotation);
        }
      }
    });

    return new GroovyClassLoader(parentLoader, compilerConfiguration) {

      @Override
      protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        return new CustomCompilationUnit(config, source, this, node -> {
          if (node.implementsInterface(ClassHelper.GENERATED_CLOSURE_Type)) {
            AnnotationNode lineNumberAnnotation = new AnnotationNode(LINE_NUMBER_CLASS_NODE);
            lineNumberAnnotation.addMember("value", new ConstantExpression(node.getLineNumber(), true));
            node.addAnnotation(lineNumberAnnotation);
          }
        });
      }
    };

  }

  // This is lifted from https://github.com/gradle/gradle/blob/c04626e744aaccf269c194fe982464826034b9a8/subprojects/core/src/main/java/org/gradle/groovy/scripts/internal/CustomCompilationUnit.java#L37
  static class CustomCompilationUnit extends CompilationUnit {

    public CustomCompilationUnit(CompilerConfiguration configuration, CodeSource codeSource, GroovyClassLoader loader, Consumer<? super ClassNode> customVerifier) {
      super(configuration, codeSource, loader);
      installCustomCodegen(customVerifier);
    }

    private void installCustomCodegen(Consumer<? super ClassNode> customVerifier) {
      final IPrimaryClassNodeOperation nodeOperation = prepareCustomCodegen(customVerifier);
      addFirstPhaseOperation(nodeOperation, Phases.CLASS_GENERATION);
    }

    @Override
    public void addPhaseOperation(IPrimaryClassNodeOperation op, int phase) {
      if (phase != Phases.CLASS_GENERATION) {
        super.addPhaseOperation(op, phase);
      }
    }

    // this is using a decoration of the existing classgen implementation
    // it can't be implemented as a phase as our customVerifier needs to visit closures as well
    private IPrimaryClassNodeOperation prepareCustomCodegen(Consumer<? super ClassNode> customVerifier) {
      try {
        final Field classgen = getClassgenField();
        IPrimaryClassNodeOperation realClassgen = (IPrimaryClassNodeOperation) classgen.get(this);
        final IPrimaryClassNodeOperation decoratedClassgen = decoratedNodeOperation(customVerifier, realClassgen);
        classgen.set(this, decoratedClassgen);
        return decoratedClassgen;
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException("Unable to install custom rules code generation", e);
      }
    }

    private Field getClassgenField() {
      try {
        final Field classgen = CompilationUnit.class.getDeclaredField("classgen");
        classgen.setAccessible(true);
        return classgen;
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("Unable to detect class generation in Groovy CompilationUnit", e);
      }
    }

    private static IPrimaryClassNodeOperation decoratedNodeOperation(Consumer<? super ClassNode> customVerifier, IPrimaryClassNodeOperation realClassgen) {
      return new IPrimaryClassNodeOperation() {

        @Override
        public boolean needSortedInput() {
          return realClassgen.needSortedInput();
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
          customVerifier.accept(classNode);
          realClassgen.call(source, context, classNode);
        }
      };
    }
  }

}
