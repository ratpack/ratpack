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

package org.ratpackframework.manual.fixture;

import groovy.lang.GroovyShell;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.Arrays;
import java.util.List;

public class DefaultScriptRunner implements ScriptRunner {

  private final GroovyShell groovyShell;
  private final String prefix;
  private final String suffix;

  public DefaultScriptRunner(String prefix, String suffix) {
    this.prefix = prefix;
    this.suffix = suffix;

    CompilerConfiguration config = new CompilerConfiguration();
    config.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
      }
    });

    groovyShell = new GroovyShell(config);
  }

  public DefaultScriptRunner() {
    this("", "");
  }

  public void runScript(String script, String sourceClassName) {
    List<String> importsAndScript = extractImports(script);
    String scriptText = importsAndScript.get(0) + prefix + importsAndScript.get(1) + suffix + ";0;";
    groovyShell.evaluate(scriptText, sourceClassName);
  }

  private List<String> extractImports(String script) {
    StringBuilder imports = new StringBuilder();
    StringBuilder scriptMinusImports = new StringBuilder();

    for (String line : script.split("\\n")) {
      StringBuilder target;
      if (line.trim().startsWith("import ")) {
        target = imports;
      } else {
        target = scriptMinusImports;
      }

      target.append(line).append("\n");
    }

    return Arrays.asList(imports.toString(), scriptMinusImports.toString());
  }

  public int getScriptLineOffset() {
    return prefix.split("\\n").length - 1;
  }

}
