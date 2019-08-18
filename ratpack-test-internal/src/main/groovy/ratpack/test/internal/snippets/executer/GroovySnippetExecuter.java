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

package ratpack.test.internal.snippets.executer;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import ratpack.test.internal.snippets.TestCodeSnippet;
import ratpack.test.internal.snippets.fixture.SnippetFixture;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public class GroovySnippetExecuter implements SnippetExecuter {

  private final boolean compileStatic;
  private final SnippetFixture fixture;

  public GroovySnippetExecuter(boolean compileStatic, SnippetFixture fixture) {
    this.compileStatic = compileStatic;
    this.fixture = fixture;
  }

  @Override
  public SnippetFixture getFixture() {
    return fixture;
  }

  @Override
  public void execute(TestCodeSnippet snippet) throws Exception {
    CompilerConfiguration config = new CompilerConfiguration();
    config.addCompilationCustomizers(new CompilationCustomizer(CompilePhase.CONVERSION) {
      @Override
      public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        if (compileStatic) {
          classNode.addAnnotation(new AnnotationNode(new ClassNode(CompileStatic.class)));
        }
      }
    });

    ClassLoader classLoader = new URLClassLoader(new URL[]{}, getClass().getClassLoader());
    GroovyShell groovyShell = new GroovyShell(classLoader, new Binding(), config);
    List<String> importsAndSnippet = extractImports(snippet.getSnippet());

    String imports = importsAndSnippet.get(0);
    String snippetMinusImports = fixture.transform(importsAndSnippet.get(1));
    String fullSnippet = imports + fixture.pre() + snippetMinusImports + fixture.post();

    Script script;
    try {
      script = groovyShell.parse(fullSnippet, snippet.getClassName());
    } catch (MultipleCompilationErrorsException e) {
      Message error = e.getErrorCollector().getError(0);
      if (error instanceof SyntaxErrorMessage) {
        //noinspection ThrowableResultOfMethodCallIgnored
        System.out.println(snippet.getSnippet());
        throw new CompileException(e, ((SyntaxErrorMessage) error).getCause().getLine());
      } else {
        throw e;
      }
    }

    ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(groovyShell.getClassLoader());
      fixture.around(script::run);
    } finally {
      Thread.currentThread().setContextClassLoader(previousContextClassLoader);
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
