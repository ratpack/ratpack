/*
 * Copyright 2003-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.templating.internal;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyRuntimeException;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.vertx.java.core.buffer.Buffer;

import java.io.IOException;

/**
 * This is a fork of {@link groovy.text.SimpleTemplateEngine}.
 *
 * That class is not suitable for the kind of extension required.
 */
public class TemplateCompiler {
  private boolean verbose;
  private final TemplateParser parser = new TemplateParser();
  private final CompilerConfiguration compilerConfiguration;

  public TemplateCompiler() {
    this(false);
  }

  public TemplateCompiler(boolean verbose) {
    this.verbose = verbose;
    compilerConfiguration = new CompilerConfiguration();
    compilerConfiguration.getOptimizationOptions().put("indy", true);
    compilerConfiguration.setScriptBaseClass(TemplateScript.class.getName());
    compilerConfiguration.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
      }
    });
  }

  public CompiledTemplate compile(ClassLoader parentLoader, Buffer templateSource, String name) throws CompilationFailedException, IOException {
    GroovyClassLoader classLoader = new GroovyClassLoader(parentLoader, compilerConfiguration);

    Buffer scriptSource = new Buffer(templateSource.length());
    parser.parse(templateSource, scriptSource);

    String scriptSourceString = scriptSource.toString();

    if (verbose) {
      System.out.println("\n-- script source --");
      System.out.print(scriptSourceString);
      System.out.println("\n-- script end --\n");
    }

    Class<? extends TemplateScript> scriptClass;
    try {
      //noinspection unchecked
      scriptClass = classLoader.parseClass(scriptSourceString, name);
    } catch (Exception e) {
      throw new GroovyRuntimeException("Failed to parse template script (your template may contain an error or be trying to use expressions not currently supported): " + e.getMessage());
    }

    return new CompiledTemplate(scriptClass);
  }

}
