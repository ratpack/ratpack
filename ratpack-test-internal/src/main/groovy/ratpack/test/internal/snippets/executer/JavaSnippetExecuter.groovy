/*
 * Copyright 2014 the original author or authors.
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

package ratpack.test.internal.snippets.executer

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ratpack.func.Block
import ratpack.test.internal.snippets.TestCodeSnippet
import ratpack.test.internal.snippets.fixture.SnippetFixture

import javax.tools.*
import java.lang.reflect.InvocationTargetException

@Slf4j
@CompileStatic
class JavaSnippetExecuter implements SnippetExecuter {

  private final String[] args
  private final SnippetFixture fixture

  JavaSnippetExecuter(SnippetFixture fixture, String... args) {
    this.fixture = fixture
    this.args = args
  }

  @Override
  SnippetFixture getFixture() {
    return fixture
  }

  @Override
  void execute(TestCodeSnippet snippet) throws Exception {
    def compiler = ToolProvider.getSystemJavaCompiler()
    List<ByteArrayJavaClass> classFileObjects = []
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>()

    def fileManager = new ForwardingJavaFileManager<JavaFileManager>(compiler.getStandardFileManager(diagnostics, null, null)) {
      JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, final String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        def fileObject = new ByteArrayJavaClass(className)
        classFileObjects << fileObject
        fileObject
      }
    }

    def fullSnippet = assembleFullSnippet(snippet)
    def className = detectClassName(fullSnippet)
    def source = new StringJavaSource(className, fullSnippet)

    def task = compiler.getTask(null, fileManager, diagnostics, ["-Xlint:deprecation", "-Xlint:unchecked"], null, Arrays.asList(source))
    def result = task.call()
    fileManager.close()

    for (Diagnostic<? extends JavaFileObject> it : diagnostics.diagnostics) {
      log.error "$it.kind: ${it.getMessage(null)}"
    }

    if (diagnostics.diagnostics) {
      def first = diagnostics.diagnostics.first()
      throw new CompileException(new AssertionError("$first.kind: ${first.getMessage(null)} ($first.lineNumber:$first.columnNumber)", null), first.lineNumber.toInteger())
    }

    if (!result) {
      throw new AssertionError("Compilation failed", null)
    }

    ClassLoader classLoader = new GroovyClassLoader()
    for (ByteArrayJavaClass javaClass : classFileObjects) {
      classLoader.defineClass(javaClass.name - "/", javaClass.bytes)
    }

    def exampleClass = classLoader.loadClass(className)
    def previousContextClassLoader = Thread.currentThread().getContextClassLoader()
    try {
      Thread.currentThread().setContextClassLoader(classLoader)
      def mainMethod = exampleClass.getMethod("main", Class.forName("[Ljava.lang.String;"))
      fixture.around({ mainMethod.invoke(null, [args] as Object[]) } as Block)
    } catch (NoSuchMethodException ignore) {
      // Class has no test method
    } catch (InvocationTargetException e) {
      throw e.cause
    } finally {
      Thread.currentThread().setContextClassLoader(previousContextClassLoader)
    }
  }

  private static String detectClassName(String snippet) {
    def match = snippet =~ /public class (\w+)/
    def className = match ? match.group(1) : "Example"
    def packageName = detectPackage(snippet)
    packageName ? "${packageName}.$className".toString() : className
  }

  private static String detectPackage(String snippet) {
    def match = snippet =~ /package ([\w.]+);/
    match ? match.group(1) : null
  }

  private String assembleFullSnippet(TestCodeSnippet snippet) {
    def imports = new StringBuilder()
    def snippetMinusImports = new StringBuilder()
    snippet.snippet.readLines().each { line ->
      ["package ", "import "].any { line.trim().startsWith(it) } ? imports.append(line).append("\n") : snippetMinusImports.append(line).append("\n")
    }
    def fullSnippet = imports.toString() + fixture.pre() + snippet.executer.fixture.transform(snippetMinusImports.toString()) + fixture.post()
    fullSnippet
  }

  static class StringJavaSource extends SimpleJavaFileObject {
    private final String code

    StringJavaSource(String name, String code) {
      super(URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE)
      this.code = code
    }

    CharSequence getCharContent(boolean ignoreEncodingErrors) {
      code
    }
  }

  class ByteArrayJavaClass extends SimpleJavaFileObject {
    private final ByteArrayOutputStream stream

    ByteArrayJavaClass(String name) {
      super(URI.create("bytes:///" + name), JavaFileObject.Kind.CLASS)
      stream = new ByteArrayOutputStream()
    }

    OutputStream openOutputStream() throws IOException {
      stream
    }

    byte[] getBytes() {
      stream.toByteArray()
    }
  }
}
