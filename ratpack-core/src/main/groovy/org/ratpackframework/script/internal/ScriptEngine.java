/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.script.internal;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.CompileStatic;
import groovy.transform.InheritConstructors;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

public class ScriptEngine<T extends Script> {

  private final GroovyClassLoader classLoader;

  public ScriptEngine(ClassLoader parentLoader, boolean staticCompile, Class<T> scriptBaseClass) {
    this.classLoader = createClassLoader(parentLoader, staticCompile, scriptBaseClass);
  }

  public <T extends Script> T run(String scriptName, String scriptText, Object... scriptConstructionArgs) throws InstantiationException, IllegalAccessException {
    T script = create(scriptName, scriptText, scriptConstructionArgs);
    script.run();
    return script;
  }

  public <T extends Script> T create(String scriptName, String scriptText, Object... scriptConstructionArgs) throws IllegalAccessException, InstantiationException {
    @SuppressWarnings("unchecked") Class<T> scriptClass = classLoader.parseClass(scriptText, scriptName);
    return DefaultGroovyMethods.newInstance(scriptClass, scriptConstructionArgs);
  }

  private GroovyClassLoader createClassLoader(ClassLoader parentLoader, final boolean staticCompile, Class<? extends Script> scriptBaseClass) {
    final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
    compilerConfiguration.getOptimizationOptions().put("indy", true);
    compilerConfiguration.setScriptBaseClass(scriptBaseClass.getName());
    compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws org.codehaus.groovy.control.CompilationFailedException {
        if (staticCompile) {
          classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
        }
        classNode.addAnnotation(new AnnotationNode(new ClassNode(InheritConstructors.class)));
      }
    });
    return new GroovyClassLoader(parentLoader, compilerConfiguration);
  }

}
