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
import groovy.lang.GroovyObject;
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

public class ScriptRunner {

  public <T extends Script> T run(String scriptName, String scriptText, Class<T> scriptBaseClass, ClassLoader parentLoader, boolean staticCompile, Object... scriptConstructionArgs) throws InstantiationException, IllegalAccessException {
    T script = createScript(scriptName, scriptText, scriptBaseClass, parentLoader, staticCompile, scriptConstructionArgs);
    script.run();
    return script;
  }

  private <T extends Script> T createScript(String scriptName, String scriptText, Class<T> scriptBaseClass, ClassLoader parentLoader, boolean staticCompile, Object... scriptConstructionArgs) throws IllegalAccessException, InstantiationException {
    GroovyClassLoader classLoader = createClassLoader(scriptBaseClass, parentLoader, staticCompile);
    @SuppressWarnings("unchecked") Class<T> scriptClass = classLoader.parseClass(scriptText, scriptName);
    return DefaultGroovyMethods.newInstance(scriptClass, scriptConstructionArgs);
  }

  private GroovyClassLoader createClassLoader(Class<? extends Script> scriptClass, final ClassLoader parentLoader, final boolean staticCompile) {
    final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
    compilerConfiguration.getOptimizationOptions().put("indy", true);
    compilerConfiguration.setScriptBaseClass(scriptClass.getName());
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
