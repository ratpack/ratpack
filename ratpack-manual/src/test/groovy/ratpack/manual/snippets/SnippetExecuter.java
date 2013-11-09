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

package ratpack.manual.snippets;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import ratpack.manual.snippets.fixtures.SnippetFixture;

import java.util.Arrays;
import java.util.List;

public class SnippetExecuter {

  private final GroovyShell groovyShell;

  public SnippetExecuter() {
    CompilerConfiguration config = new CompilerConfiguration();
    config.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
      }
    });

    groovyShell = new GroovyShell(config);
  }

  public void execute(TestCodeSnippet snippet) {
    List<String> importsAndSnippet = extractImports(snippet.getSnippet());

    String imports = importsAndSnippet.get(0);
    String snippetMinusImports = importsAndSnippet.get(1);

    SnippetFixture fixture = snippet.getFixture();
    String fullSnippet = imports + fixture.pre() + snippetMinusImports + fixture.post() + ";0;";

    Script script = groovyShell.parse(fullSnippet, snippet.getClassName());

    fixture.setup();
    try {
      script.run();
    } finally {
      fixture.cleanup();
    }
  }

  private List<String> extractImports(String snippet) {
    StringBuilder imports = new StringBuilder();
    StringBuilder scriptMinusImports = new StringBuilder();

    for (String line : snippet.split("\\n")) {
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
}
