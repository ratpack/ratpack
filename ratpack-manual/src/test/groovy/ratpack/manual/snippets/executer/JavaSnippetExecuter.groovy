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

package ratpack.manual.snippets.executer

import groovy.transform.CompileStatic
import ratpack.manual.snippets.TestCodeSnippet

import javax.tools.*
import java.lang.reflect.InvocationTargetException
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
public class JavaSnippetExecuter implements SnippetExecuter {
  @Override
  public void execute(TestCodeSnippet snippet) {
    def compiler = ToolProvider.getSystemJavaCompiler()
    List<ByteArrayJavaClass> classFileObjects = []
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>()

    def fileManager = new ForwardingJavaFileManager<JavaFileManager>(compiler.getStandardFileManager(diagnostics, null, null)) {
      public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, final String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        def fileObject = new ByteArrayJavaClass(className)
        classFileObjects << fileObject
        fileObject
      }
    }

    def source = new StringJavaSource("Example", snippet.completeSnippet)

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

    def exampleClass = classLoader.loadClass("Example")
    try {
      def mainMethod = exampleClass.getMethod("main", Class.forName("[Ljava.lang.String;"))
      mainMethod.invoke(null, [[] as String[]] as Object[])
    } catch (NoSuchMethodException ignore) {
      // Class has no test method
    } catch (InvocationTargetException e) {
     throw e.cause
    }
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
