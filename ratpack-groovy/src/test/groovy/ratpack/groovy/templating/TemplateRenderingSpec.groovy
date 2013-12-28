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

package ratpack.groovy.templating

import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static ratpack.groovy.Groovy.groovyTemplate

class TemplateRenderingSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new TemplatingModule()
  }

  def "can render template"() {

    given:
    file("templates/foo.html") << "a \${model.value} b <% 3.times {  %> a <% } %>"

    when:
    handlers {
      get {
        render groovyTemplate("foo.html", value: "bar")
      }
    }

    then:
    text == "a bar b  a  a  a "
  }

  def "off thread errors are rendered"() {
    given:
    when:
    handlers {
      get {
        withErrorHandling Thread.start {
          throw new Exception("nested!")
        }
      }
    }

    then:
    text.contains "<title>java.lang.Exception</title>"
  }

  def "can render inner template"() {
    given:
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value}"

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
    file("templates/head.html") << "head"
    file("templates/middle.html") << '<% render "head.html" %>-middle-<% render "footer.html" %>'
    file("templates/footer.html") << "footer"

    when:
    handlers {
      get { render groovyTemplate("middle.html") }
    }

    then:
    getText() == "head-middle-footer"
  }

  def "can render inner, inner template"() {
    given:
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    file("templates/innerInner.html") << "innerInner: \${model.value}"

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
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    file("templates/innerInner.html") << "\${throw new Exception(model.value.toString())}"

    when:
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
    file("templates/outer.html") << "outer: \${model.a}\${model.b}, <% render 'inner.html', b: 'B' %>"
    file("templates/inner.html") << "inner: \${model.a}\${model.b}, <% render 'innerInner.html', a: 'A' %>"
    file("templates/innerInner.html") << "innerInner: \${model.a}\${model.b}"

    when:
    handlers {
      get {
        render groovyTemplate("outer.html", a: "a", b: "b")
      }
    }

    then:
    text == "outer: ab, inner: aB, innerInner: AB"
  }

  @Unroll
  "can use render in output section - #template"() {
    given:
    file("templates/outer.html") << template
    file("templates/foo.html") << "foo"

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

  @Unroll
  "can use render in output section in nested - #template"() {
    given:
    file("templates/outer.html") << "<% render 'inner.html' %>"
    file("templates/inner.html") << template
    file("templates/foo.html") << "foo"

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
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value.toInteger()}"

    when:
    modules {
      get(TemplatingModule).staticallyCompile = true
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
    file("templates/template.html") << "value: \${model.get('value', String).toInteger()}"

    when:
    modules {
      get(TemplatingModule).staticallyCompile = true
    }

    handlers {
      get {
        render groovyTemplate("template.html", value: "2")
      }
    }

    then:
    text.contains "value: 2"
  }

  def "client errors are rendered with the template renderer"() {
    when:
    handlers {
      handler {
        clientError(404)
      }
    }

    then:
    text.contains "<title>Not Found</title>"
    get().statusCode == 404
  }

  def "templates are reloadable in reload mode"() {
    given:
    launchConfig { reloadable(true) }
    file("templates/t") << "1"

    when:
    handlers {
      get { render groovyTemplate("t") }
    }

    then:
    text == "1"

    when:
    sleep 1000
    file("templates/t").text = "2"

    then:
    text == "2"
  }

  def "templates are not reloadable in reload false mode"() {
    given:
    launchConfig { reloadable(false) }
    file("templates/t") << "1"

    when:
    handlers {
      get { render groovyTemplate("t") }
    }

    then:
    text == "1"

    when:
    file("templates/t").text = "2"

    then:
    text == "1"
  }

  def "templates are reloadable if reloading is forced"() {
    given:
    file("templates/t") << "1"

    when:
    modules {
      get(TemplatingModule).reloadable = true
    }
    handlers {
      get { render groovyTemplate("t") }
    }

    then:
    text == "1"

    when:
    sleep 1000
    file("templates/t").text = "2"

    then:
    text == "2"
  }

  def "content type by template extension"() {
    when:
    file("templates/t.html") << "1"
    file("templates/t.xml") << "1"
    file("templates/dir/t.html") << "1"
    file("templates/dir/t.xml") << "1"
    file("templates/dir/t") << "1"

    handlers {
      handler {
        render groovyTemplate(request.path, request.queryParams.type)
      }
    }

    then:
    get("t.html").contentType == "text/html;charset=UTF-8"
    get("t.xml").contentType == "application/xml"
    get("dir/t.html").contentType == "text/html;charset=UTF-8"
    get("dir/t.xml").contentType == "application/xml"
    get("dir/t").contentType == "application/octet-stream"

    get("t.xml?type=foo/bar").contentType == "foo/bar"
    get("dir/t.xml?type=foo/bar").contentType == "foo/bar"
  }

  def "error in error template produces empty response and right error code"() {
    given:
    file("templates/error.html") << "a a a \${-==}" // invalid syntax

    when:
    handlers {
      get("server") {
        throw new Exception("!")
      }
      get("client") {
        clientError 400
      }
    }

    then:
    getText("server") == ""
    response.statusCode == 500
    getText("client") == ""
    response.statusCode == 500
  }
}
