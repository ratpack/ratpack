/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rocker

import com.fizzed.rocker.RenderingException
import com.fizzed.rocker.compiler.JavaGeneratorMain
import ratpack.error.ServerErrorHandler
import ratpack.render.RendererException
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.tools.*

class RatpackRockerSpec extends RatpackGroovyDslSpec {

  public static final String OPTIMIZE_FLAG = "rocker.option.optimize"

  def setup() {
    bindings {
      module RockerModule
    }
  }

  def "can render rocker template of single buffer"() {
    given:
    Class<?> templateClass = createRockerModel """
<p>Hello world!</p>
"""

    when:
    handlers {
      get {
        render templateClass.template()
      }
    }

    then:
    text == "<p>Hello world!</p>\n"
  }

  def "can render rocker template of multi buffer"() {
    given:
    Class<?> templateClass = createRockerModel """
@args (String title)
<p>Hello @title!</p>
"""

    when:
    handlers {
      get {
        render templateClass.template("world")
      }
    }

    then:
    text == "<p>Hello world!</p>\n"
  }

  def "errors are specially treated"() {
    given:
    Class<?> templateClass = createRockerModel """
@args (String title)
<p>Hello @title!</p>
"""
    Exception e

    when:
    handlers {
      register {
        add(ServerErrorHandler, { ctx, ex -> e = ex; ctx.response.send() } as ServerErrorHandler)
      }
      get {
        render templateClass.template(null)
      }
    }

    then:
    text == ""
    e instanceof RendererException
    e.cause instanceof RenderingException
    e.cause.cause instanceof NullPointerException
  }

  Class<?> createRockerModel(String content) {
    def packagePath = RatpackRockerSpec.package.name.replace('.', '/')
    baseDir.write("in/$packagePath/Temp.rocker.html", content)

    String value = System.setProperty(OPTIMIZE_FLAG, "true")
    try {
      JavaGeneratorMain.main(
        "-t", baseDir.path("in").toAbsolutePath().toString(),
        "-o", baseDir.path("out").toAbsolutePath().toString(),
      )
    } finally {
      value == null ? System.clearProperty(OPTIMIZE_FLAG) : System.setProperty(OPTIMIZE_FLAG, value)
    }

    def classesDir = baseDir.path("classes")
    def javaFile = baseDir.path("out/$packagePath/Temp.java")

    def templateClassName = "${RatpackRockerSpec.package.name}.Temp"
    def compiler = ToolProvider.getSystemJavaCompiler()
    def fileManager = new ForwardingJavaFileManager<JavaFileManager>(compiler.getStandardFileManager(null, null, null)) {
      JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, final String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        new SimpleJavaFileObject(javaFile.toUri(), kind) {
          @Override
          OutputStream openOutputStream() throws IOException {
            def path = "${className.replace('.', '/')}.class"
            def file = classesDir.resolve(path).toFile()
            file.parentFile.mkdirs()
            file.newOutputStream()
          }
        }
      }
    }
    def task = compiler.getTask(null, fileManager, null, null, null, [new SimpleJavaFileObject(javaFile.toUri(), JavaFileObject.Kind.SOURCE) {
      @Override
      CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        javaFile.text
      }
    }])
    task.call()

    def classLoader = new GroovyClassLoader()
    classLoader.addURL(classesDir.toUri().toURL())
    def templateClass = classLoader.loadClass(templateClassName)
    templateClass
  }

}
