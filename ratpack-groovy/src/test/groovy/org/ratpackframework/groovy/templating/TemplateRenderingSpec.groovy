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

package org.ratpackframework.groovy.templating

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec
import spock.lang.Unroll

class TemplateRenderingSpec extends RatpackGroovyDslSpec {

  def setup() {
    modules << new TemplatingModule()
  }

  def "can render template"() {
    given:
    file("templates/foo.html") << "a \${model.value} b <% 3.times {  %> a <% } %>"

    when:
    app {
      routing {
        get {
          get(TemplateRenderer).render "foo.html", value: "bar"
        }
      }
    }

    then:
    urlGetText() == "a bar b  a  a  a "
  }

  def "can render inner template"() {
    given:
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value}"

    when:
    app {
      routing {
        get {
          get(TemplateRenderer).render "outer.html", value: "outer"
        }
      }
    }

    then:
    urlGetText() == "outer: outer, inner: inner"
  }

  def "can render inner, inner template"() {
    given:
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    file("templates/innerInner.html") << "innerInner: \${model.value}"

    when:
    app {
      routing {
        get {
          get(TemplateRenderer).render "outer.html", value: "outer"
        }
      }
    }

    then:
    urlGetText() == "outer: outer, inner: inner, innerInner: 1, innerInner: 2, innerInner: 1"
  }

  def "inner template exceptions"() {
    given:
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    file("templates/innerInner.html") << "\${throw new Exception(model.value.toString())}"

    when:
    app {
      routing {
        get {
          get(TemplateRenderer).render "outer.html", value: "outer"
        }
      }
    }

    then:
    errorGetText().contains('[innerInner.html] template execution failed')
  }

  def "nested templates inherit the outer model"() {
    given:
    file("templates/outer.html") << "outer: \${model.a}\${model.b}, <% render 'inner.html', b: 'B' %>"
    file("templates/inner.html") << "inner: \${model.a}\${model.b}, <% render 'innerInner.html', a: 'A' %>"
    file("templates/innerInner.html") << "innerInner: \${model.a}\${model.b}"

    when:
    app {
      routing {
        get {
          get(TemplateRenderer).render "outer.html", a: "a", b: "b"
        }
      }
    }

    then:
    urlGetText() == "outer: ab, inner: aB, innerInner: AB"
  }

  @Unroll
  "can use render in output section - #template"() {
    given:
    file("templates/outer.html") << template
    file("templates/foo.html") << "foo"

    when:
    app {
      routing {
        get {
          get(TemplateRenderer).render "outer.html"
        }
      }
    }

    then:
    urlGetText() == "foo"

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
    app {
      routing {
        get {
          get(TemplateRenderer).render "outer.html"
        }
      }
    }

    then:
    urlGetText() == "foo"

    where:
    template << ["\${render 'foo.html'}", "<%= render 'foo.html' %>"]
  }

  def "compile error in inner template"() {
    given:
    file("templates/outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    file("templates/inner.html") << "inner: \${model.value.toInteger()}"

    when:
    app {
      modules {
        get(TemplatingModule).config.staticallyCompile = true
      }

      routing {
        get {
          get(TemplateRenderer).render "outer.html", value: "outer"
        }
      }
    }

    then:
    errorGetText().contains "[inner.html] compilation failure"
  }

}
