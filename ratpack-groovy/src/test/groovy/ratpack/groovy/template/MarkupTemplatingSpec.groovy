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

package ratpack.groovy.template

import ratpack.test.embed.internal.JarFileEphemeralBaseDir
import ratpack.test.internal.RatpackGroovyDslSpec

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static ratpack.groovy.Groovy.groovyMarkupTemplate

class MarkupTemplatingSpec extends RatpackGroovyDslSpec {

  def contextClassLoader = new GroovyClassLoader()
  def originalContextClassLoader

  def setup() {
    originalContextClassLoader = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = contextClassLoader
  }

  def cleanup() {
    Thread.currentThread().contextClassLoader = originalContextClassLoader
  }

  def "can render template"() {
    given:
    write "templates/foo.gtpl", "yield 'a '; yield value; yield ' b '; 3.times {  yield ' a ' }"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("foo.gtpl", value: "bar")
      }
    }

    then:
    text == "a bar b  a  a  a "
  }

  def "can render template with builder syntax"() {
    given:
    write "templates/foo.gtpl", "div { p(value) }"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("foo.gtpl", value: "bar")
      }
    }

    then:
    text == "<div><p>bar</p></div>"
  }

  def "templates are auto-escaped by default"() {
    given:
    write "templates/foo.gtpl", "div(value)"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("foo.gtpl", value: "<bar>")
      }
    }

    then:
    text == "<div>&lt;bar&gt;</div>"
  }

  def "auto-escape can be configured via templateconfiguration from guice"() {
    given:
    write "templates/foo.gtpl", "div(value)"
    bindings {
      module(MarkupTemplateModule) { it.autoEscape = false }
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("foo.gtpl", value: "<bar>")
      }
    }

    then:
    text == "<div><bar></div>"
  }

  def "auto expanding empty elements is off by default"() {
    given:
    write "templates/foo.gtpl", "div()"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("foo.gtpl")
      }
    }

    then:
    text == "<div/>"
  }

  def "empty elements can be configured to be auto expanded"() {
    given:
    write "templates/foo.gtpl", "div()"
    bindings {
      module(MarkupTemplateModule) { it.expandEmptyElements = true }
    }
    when:
    handlers {
      get {
        render groovyMarkupTemplate("foo.gtpl")
      }
    }

    then:
    text == "<div></div>"
  }

  def "can include another template"() {
    given:
    write "templates/foo.gtpl", "div { include template:'bar.gtpl' }"
    write "templates/bar.gtpl", "p(value)"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("foo.gtpl", value: "bar")
      }
    }

    then:
    text == "<div><p>bar</p></div>"
  }

  def "can render inner template"() {
    given:
    write "templates/outer.gtpl", 'yield "outer: $value, "; layout "inner.gtpl", value: "inner"'
    write "templates/inner.gtpl", 'yield "inner: $value"'
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("outer.gtpl", value: "outer")
      }
    }

    then:
    text == "outer: outer, inner: inner"
  }

  def "inner templates are rendered in order"() {
    given:
    write "templates/head.gtpl", "yield 'head'"
    write "templates/middle.gtpl", 'include template:"head.gtpl"; yield "-middle-"; include template:"footer.gtpl"'
    write "templates/footer.gtpl", "yield 'footer'"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get { render groovyMarkupTemplate("middle.gtpl") }
    }

    then:
    getText() == "head-middle-footer"
  }

  def "can render inner, inner template"() {
    given:
    write "templates/outer.gtpl", 'yield "outer: $value, "; layout "inner.gtpl", value: "inner"'
    write "templates/inner.gtpl", 'yield "inner: $value, "; layout "innerInner.gtpl", value: 1; yield ", "; layout "innerInner.gtpl", value: 2; yield ", "; layout "innerInner.gtpl", value: 1'
    write "templates/innerInner.gtpl", 'yield "innerInner: $value"'
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("outer.gtpl", value: "outer")
      }
    }

    then:
    text == "outer: outer, inner: inner, innerInner: 1, innerInner: 2, innerInner: 1"
  }


  def "nested templates can inherit the outer model"() {
    given:
    write "templates/outer.gtpl", 'yield "outer: $a$b, "; layout (*:model, b: "B", "inner.gtpl")'
    write "templates/inner.gtpl", 'yield "inner: $a$b, "; layout (*:model, a: "A", "innerInner.gtpl")'
    write "templates/innerInner.gtpl", 'yield "innerInner: $a$b"'
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("outer.gtpl", a: "a", b: "b")
      }
    }

    then:
    text == "outer: ab, inner: aB, innerInner: AB"
  }

  def "can use layout for template composition"() {
    given:
    write "templates/layout.gtpl", """html { head { title(pageTitle) } body { pageBody() }}"""
    write 'templates/body.gtpl', "p('This is the body')"
    write 'templates/main.gtpl', '''
      layout "layout.gtpl",
        pageTitle: "My Page",
        pageBody: contents { include template: "body.gtpl" }
    '''
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get {
        render groovyMarkupTemplate("main.gtpl")
      }
    }

    then:
    text == "<html><head><title>My Page</title></head><body><p>This is the body</p></body></html>"
  }

  def "templates are reloadable in development mode"() {
    given:
    serverConfig { development(true) }
    write "templates/t.gtpl", "yield 1"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get { render groovyMarkupTemplate("t.gtpl") }
    }

    then:
    text == "1"

    when:
    sleep 1000
    write "templates/t.gtpl", "yield 2"

    then:
    text == "2"
  }

  def "templates are not reloadable in development false mode"() {
    given:
    serverConfig { development(false) }
    write "templates/t.gtpl", "yield 1"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    handlers {
      get { render groovyMarkupTemplate("t.gtpl") }
    }

    then:
    text == "1"

    when:
    write "templates/t.gtpl", "yield 2"

    then:
    text == "1"
  }

  def "templates are reloadable if reloading is forced"() {
    if (baseDir instanceof JarFileEphemeralBaseDir) {
      // https://jira.codehaus.org/browse/GROOVY-7002
      return
    }

    given:
    write "templates/t.gtpl", "yield 1"
    bindings {
      module(MarkupTemplateModule)
    }

    when:
    bindings {
      module(MarkupTemplateModule) { it.cacheTemplates = false }
    }
    handlers {
      get { render groovyMarkupTemplate("t.gtpl") }
    }

    then:
    text == "1"

    when:
    sleep 1000
    write "templates/t.gtpl", "yield 2"

    then:
    text == "2"
  }

  def "content type by template extension"() {
    when:
    bindings {
      module(MarkupTemplateModule)
    }
    write "templates/t.gtpl", "1"
    write "templates/t.xml", "1"
    write "templates/dir/t.gtpl", "1"
    write "templates/dir/t.xml", "1"

    handlers {
      all {
        render groovyMarkupTemplate(request.path, request.queryParams.type)
      }
    }

    then:
    get("t.gtpl").headers.get(CONTENT_TYPE) == "text/html;charset=UTF-8"
    get("t.xml").headers.get(CONTENT_TYPE) == "application/xml"
    get("dir/t.gtpl").headers.get(CONTENT_TYPE) == "text/html;charset=UTF-8"
    get("dir/t.xml").headers.get(CONTENT_TYPE) == "application/xml"

    get("t.xml?type=foo/bar").headers.get(CONTENT_TYPE) == "foo/bar"
    get("dir/t.xml?type=foo/bar").headers.get(CONTENT_TYPE) == "foo/bar"
  }

  def "templates can access context loader classes"() {
    given:
    bindings {
      module(MarkupTemplateModule)
    }
    contextClassLoader.parseClass """
      package com

      class Foo {
        static value = "foo"
      }
    """, "Foo.groovy"

    write "templates/t.gtpl", """
      import com.Foo
      p Foo.value
    """

    when:
    handlers {
      get {
        render groovyMarkupTemplate("t.gtpl", request.queryParams.type)
      }
    }

    then:
    text == "<p>foo</p>"
  }
}
