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
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.nio.file.Path;
import java.security.CodeSource;

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
        return new CompilationUnit(config, source, this) {
          {
            verifier = new Verifier() {
              @Override
              public void visitClass(ClassNode node) {
                if (node.implementsInterface(ClassHelper.GENERATED_CLOSURE_Type)) {
                  AnnotationNode lineNumberAnnotation = new AnnotationNode(LINE_NUMBER_CLASS_NODE);
                  lineNumberAnnotation.addMember("value", new ConstantExpression(node.getLineNumber(), true));
                  node.addAnnotation(lineNumberAnnotation);
                }

                super.visitClass(node);
              }
            };
          }
        };
      }
    };

  }

}
