/*
 * Copyright 2022 the original author or authors.
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

package ratpack.test.docs.executer;

import groovy.lang.GroovyClassLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.test.docs.SnippetExecuter;
import ratpack.test.docs.TestCodeSnippet;
import ratpack.test.docs.SnippetFixture;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSnippetExecuter implements SnippetExecuter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaSnippetExecuter.class);
  private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("public class (\\w+)");
  private static final Pattern PACKAGE_PATTERN = Pattern.compile("package ([\\w.]+)");
  private final String[] args;
  private final SnippetFixture fixture;

  public JavaSnippetExecuter(SnippetFixture fixture, String... args) {
    this.fixture = fixture;
    this.args = args;
  }

  @Override
  public SnippetFixture getFixture() {
    return fixture;
  }

  @Override
  public void execute(TestCodeSnippet snippet) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    List<ByteArrayJavaClass> classFileObjects = new ArrayList<>();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    JavaFileManager fileManager = new ForwardingJavaFileManager<JavaFileManager>(compiler.getStandardFileManager(diagnostics, null, null)) {

      @Override
      public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, final String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        ByteArrayJavaClass fileObject = new ByteArrayJavaClass(className);
        classFileObjects.add(fileObject);
        return fileObject;
      }
    };

    ExtractedSnippet extractedSnippet = extractImports(snippet.getSnippet());
    String fullSnippet = fixture.build(extractedSnippet);

    String className = detectClassName(fullSnippet);
    StringJavaSource source = new StringJavaSource(className, fullSnippet);

    JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, Arrays.asList("-Xlint:deprecation", "-Xlint:unchecked"), null, Collections.singletonList(source));
    Boolean result = task.call();
    fileManager.close();

    for (Diagnostic<? extends JavaFileObject> it : diagnostics.getDiagnostics()) {
      LOGGER.error(it.getKind().toString() + ": " + it.getMessage(null));
    }

    List<Diagnostic<? extends JavaFileObject>> dl = diagnostics.getDiagnostics();
    if (dl != null && dl.size() > 0) {
      Diagnostic<? extends JavaFileObject> first = dl.get(0);
      throw new CompileException(new AssertionError(first.getKind().toString() + ": " + first.getMessage(null) + "(" + first.getLineNumber() + ":" + first.getColumnNumber() + ")", null), (int) first.getLineNumber());
    }

    if (!result) {
      throw new AssertionError("Compilation failed", null);
    }

    GroovyClassLoader classLoader = new GroovyClassLoader();
    for (ByteArrayJavaClass javaClass : classFileObjects) {
      classLoader.defineClass(StringUtils.strip(javaClass.getName(), "/"), javaClass.getBytes());
    }

    Class<?> exampleClass = classLoader.loadClass(className);
    ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(classLoader);
      Method mainMethod = exampleClass.getMethod("main", Class.forName("[Ljava.lang.String;"));
      fixture.around(() ->
        mainMethod.invoke(null, new Object[]{args})
      );
    } catch (NoSuchMethodException ignore) {
      // Class has no test method
    } catch (InvocationTargetException e) {
      throw (Exception) e.getCause();
    } finally {
      Thread.currentThread().setContextClassLoader(previousContextClassLoader);
    }
  }

  private static String detectClassName(String snippet) {
    Matcher match = CLASS_NAME_PATTERN.matcher(snippet);
    String className = match.find() ? match.group(1) : "Example";
    String packageName = detectPackage(snippet);
    return packageName != null && !packageName.isEmpty() ? packageName + "." + className : className;
  }

  private static String detectPackage(String snippet) {
    Matcher match = PACKAGE_PATTERN.matcher(snippet);
    return match.find() ? match.group(1) : null;
  }

  public static class StringJavaSource extends SimpleJavaFileObject {
    private final String code;

    public StringJavaSource(String name, String code) {
      super(URI.create("string:///" + name.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
      this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return code;
    }
  }

  public class ByteArrayJavaClass extends SimpleJavaFileObject {
    private final ByteArrayOutputStream stream;

    public ByteArrayJavaClass(String name) {
      super(URI.create("bytes:///" + name), JavaFileObject.Kind.CLASS);
      stream = new ByteArrayOutputStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
      return stream;
    }

    byte[] getBytes() {
      return stream.toByteArray();
    }
  }
}
