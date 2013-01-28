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
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

public class ScriptRunner {

  public void runWithDelegate(String scriptName, String scriptText, final GroovyObject delegate, ClassLoader parentLoader, boolean staticCompile) throws IllegalAccessException, InstantiationException {
    run(scriptName, scriptText, DelegatingScript.class, parentLoader, staticCompile, new Configure<DelegatingScript>() {
      @Override
      public void configure(DelegatingScript thing) {
        thing.$setDelegate(delegate);
      }
    });
  }

  public <T extends Script> T run(String scriptName, String scriptText, Class<T> scriptBaseClass, ClassLoader parentLoader, boolean staticCompile, Configure<T> configure) throws InstantiationException, IllegalAccessException {
    T script = createScript(scriptName, scriptText, scriptBaseClass, parentLoader, staticCompile);
    configure.configure(script);
    script.run();
    return script;
  }

  private <T extends Script> T createScript(String scriptName, String scriptText, Class<T> scriptBaseClass, ClassLoader parentLoader, boolean staticCompile) throws IllegalAccessException, InstantiationException {
    GroovyClassLoader classLoader = createClassLoader(scriptBaseClass, parentLoader, staticCompile);
    @SuppressWarnings("unchecked") Class<T> scriptClass = classLoader.parseClass(scriptText, scriptName);
    return scriptBaseClass.cast(scriptClass.newInstance());
  }

  private GroovyClassLoader createClassLoader(Class<? extends Script> scriptClass, ClassLoader parentLoader, boolean staticCompile) {
    CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
    compilerConfiguration.getOptimizationOptions().put("indy", true);
    compilerConfiguration.setScriptBaseClass(scriptClass.getName());
    if (staticCompile) {
      compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws org.codehaus.groovy.control.CompilationFailedException {
          classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
        }
      });
    }
    return new GroovyClassLoader(parentLoader, compilerConfiguration);
  }

}
