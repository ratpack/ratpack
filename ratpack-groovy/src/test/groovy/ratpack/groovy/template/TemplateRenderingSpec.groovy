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

package ratpack.groovy.template

import ratpack.error.ServerErrorHandler
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static ratpack.groovy.Groovy.groovyTemplate

class TemplateRenderingSpec extends RatpackGroovyDslSpec {

  def "can render template"() {
    given:
    bindings { module(TextTemplateModule) }
    write "templates/foo.html", "a \${model.value} b <% 3.times {  %> a <% } %>"

    when:
    handlers {
      get {
        render groovyTemplate("foo.html", value: "bar")
      }
    }

    then:
    text == "a bar b  a  a  a "
  }

  def "can render inner template"() {
    given:
    bindings { module(TextTemplateModule) }
    write "templates/outer.html", "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    write "templates/inner.html", "inner: \${model.value}"

    when:
    handlers {
      get {
        render groovyTemplate("outer.html", value: "outer")
      }
    }

    then:
    text == "outer: outer, inner: inner"
  }

  def "inner templates are rendered in order"() {
    given:
    bindings { module(TextTemplateModule) }
    write "templates/head.html", "head"
    write "templates/middle.html", '<% render "head.html" %>-middle-<% render "footer.html" %>'
    write "templates/footer.html", "footer"

    when:
    handlers {
      get { render groovyTemplate("middle.html") }
    }

    then:
    getText() == "head-middle-footer"
  }

  def "can render inner, inner template"() {
    given:
    bindings { module(TextTemplateModule) }
    write "templates/outer.html", "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    write "templates/inner.html", "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    write "templates/innerInner.html", "innerInner: \${model.value}"

    when:
    handlers {
      get {
        render groovyTemplate("outer.html", value: "outer")
      }
    }

    then:
    text == "outer: outer, inner: inner, innerInner: 1, innerInner: 2, innerInner: 1"
  }

  def "inner template exceptions"() {
    given:
    write "templates/outer.html", "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    write "templates/inner.html", "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    write "templates/innerInner.html", "\${throw new Exception(model.value.toString())}"

    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
      module(TextTemplateModule)
    }
    handlers {
      get {
        render groovyTemplate("outer.html", value: "outer")
      }
    }

    then:
    text.contains('[innerInner.html] template execution failed')
  }

  def "nested templates inherit the outer model"() {
    given:
    bindings { module(TextTemplateModule) }
    write "templates/outer.html", "outer: \${model.a}\${model.b}, <% render 'inner.html', b: 'B' %>"
    write "templates/inner.html", "inner: \${model.a}\${model.b}, <% render 'innerInner.html', a: 'A' %>"
    write "templates/innerInner.html", "innerInner: \${model.a}\${model.b}"

    when:
    handlers {
      get {
        render groovyTemplate("outer.html", a: "a", b: "b")
      }
    }

    then:
    text == "outer: ab, inner: aB, innerInner: AB"
  }

  def "can use render in output section - #template" ( ) {
    given:
    write "templates/outer.html", template
    write "templates/foo.html", "foo"
    bindings { module(TextTemplateModule) }

    when:
    handlers {
      get {
        render groovyTemplate("outer.html")
      }
    }

    then:
    text == "foo"

    where:
    template << ["\${render 'foo.html'}", "<%= render 'foo.html' %>"]
  }

  def "can use render in output section in nested - #template"() {
    given:
    bindings { module(TextTemplateModule) }
    write "templates/outer.html", "<% render 'inner.html' %>"
    write "templates/inner.html", template
    write "templates/foo.html", "foo"

    when:
    handlers {
      get {
        render groovyTemplate("outer.html")
      }
    }

    then:
    text == "foo"

    where:
    template << ["\${render 'foo.html'}", "<%= render 'foo.html' %>"]
  }

  def "compile error in inner template"() {
    given:
    write "templates/outer.html", "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    write "templates/inner.html", "inner: \${model.value.toInteger()}"

    when:
    bindings {
      module(TextTemplateModule) { it.staticallyCompile = true }
      bind ServerErrorHandler, SimpleErrorHandler
    }

    handlers {
      get {
        render groovyTemplate("outer.html", value: "outer")
      }
    }

    then:
    text.contains "[inner.html] compilation failure"
  }

  def "can get model object via type"() {
    given:
    write "templates/template.html", "value: \${model.get('value', String).toInteger()}"

    when:
    bindings {
      module(TextTemplateModule) { it.staticallyCompile = true }
    }

    handlers {
      get {
        render groovyTemplate("template.html", value: "2")
      }
    }

    then:
    text.contains "value: 2"
  }

  def "can get model object via generic type"() {
    given:
    write "templates/template.html", "value: \${model.get('value', new com.google.common.reflect.TypeToken<List<Integer>>() {})}"

    when:
    bindings {
      module(TextTemplateModule) { it.staticallyCompile = true }
    }

    handlers {
      get {
        render groovyTemplate("template.html", value: [1, 2])
      }
    }

    then:
    text.contains "value: [1, 2]"

    when:
    write "templates/template.html", "value: \${model.get('value', new com.google.common.reflect.TypeToken<List<Thread>>() {})}"

    then:
    text.contains "value: [1, 2]" // thank you erasure
  }

  def "templates are reloadable in development mode"() {
    given:
    bindings { module(TextTemplateModule) }
    serverConfig { development(true) }
    write "templates/t", "1"

    when:
    handlers {
      get { render groovyTemplate("t") }
    }

    then:
    text == "1"

    when:
    sleep 1000
    write "templates/t", "2"

    then:
    text == "2"
  }

  def "templates are not reloadable in development false mode"() {
    given:
    bindings { module(TextTemplateModule) }
    serverConfig { development(false) }
    write "templates/t", "1"

    when:
    handlers {
      get { render groovyTemplate("t") }
    }

    then:
    text == "1"

    when:
    write "templates/t", "2"

    then:
    text == "1"
  }

  def "content type by template extension"() {
    when:
    bindings { module(TextTemplateModule) }
    write "templates/t.html", "1"
    write "templates/t.xml", "1"
    write "templates/dir/t.html", "1"
    write "templates/dir/t.xml", "1"
    write "templates/dir/t", "1"

    handlers {
      all {
        render groovyTemplate(request.path, request.queryParams.type)
      }
    }

    then:
    get("t.html").headers.get(CONTENT_TYPE) == "text/html"
    get("t.xml").headers.get(CONTENT_TYPE) == "application/xml"
    get("dir/t.html").headers.get(CONTENT_TYPE) == "text/html"
    get("dir/t.xml").headers.get(CONTENT_TYPE) == "application/xml"
    get("dir/t").headers.get(CONTENT_TYPE) == "application/octet-stream"

    get("t.xml?type=foo/bar").headers.get(CONTENT_TYPE) == "foo/bar"
    get("dir/t.xml?type=foo/bar").headers.get(CONTENT_TYPE) == "foo/bar"
  }

  def "can escape in template"() {
    given:
    write "templates/tpl.html", "\${html '<>'} \${urlPathSegment 'a/b'} \${urlParam 'a b'}"
    bindings { module(TextTemplateModule) }

    when:
    handlers {
      get {
        render groovyTemplate("tpl.html")
      }
    }

    then:
    text == "&lt;&gt; a%2Fb a+b"
  }
}
