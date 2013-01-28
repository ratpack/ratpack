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

package org.ratpackframework

import org.ratpackframework.templating.internal.InvalidTemplateException
import spock.lang.Unroll

class TemplateRenderingSpec extends RatpackSpec {

  def "can render template"() {
    given:
    templateFile("foo.html") << "a \${model.value} b <% 3.times {  %> a <% } %>"

    and:
    ratpackFile << """
      get("/") {
        render "foo.html", value: "bar"
      }
    """

    when:
    startApp()

    then:
    urlText() == "a bar b  a  a  a "
  }

  def "can render inner template"() {
    given:
    templateFile("outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    templateFile("inner.html") << "inner: \${model.value}"

    and:
    ratpackFile << """
      get("/") {
        render "outer.html", value: "outer"
      }
    """

    when:
    startApp()

    then:
    urlText() == "outer: outer, inner: inner"
  }

  def "can render inner, inner template"() {
    given:
    templateFile("outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    templateFile("inner.html") << "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    templateFile("innerInner.html") << "innerInner: \${model.value}"

    and:
    ratpackFile << """
      get("/") {
        render "outer.html", value: "outer"
      }
    """

    when:
    startApp()

    then:
    urlText() == "outer: outer, inner: inner, innerInner: 1, innerInner: 2, innerInner: 1"
  }

  def "inner template exceptions"() {
    given:
    templateFile("outer.html") << "outer: \${model.value}, <% render 'inner.html', value: 'inner' %>"
    templateFile("inner.html") << "inner: \${model.value}, <% render 'innerInner.html', value: 1 %>, <% render 'innerInner.html', value: 2 %>, <% render 'innerInner.html', value: 1 %>"
    templateFile("innerInner.html") << "\${throw new Exception(model.value.toString())}"

    and:
    ratpackFile << """
      get("/") {
        render "outer.html", value: "outer"
      }
    """

    when:
    startApp()

    then:
    errorText().contains('org.ratpackframework.templating.internal.CompositeException')
  }

  def "nested templates inherit the outer model"() {
    given:
    templateFile("outer.html") << "outer: \${model.a}\${model.b}, <% render 'inner.html', b: 'B' %>"
    templateFile("inner.html") << "inner: \${model.a}\${model.b}, <% render 'innerInner.html', a: 'A' %>"
    templateFile("innerInner.html") << "innerInner: \${model.a}\${model.b}"

    and:
    ratpackFile << """
      get("/") {
        render "outer.html", a: "a", b: "b"
      }
    """

    when:
    startApp()

    then:
    urlText() == "outer: ab, inner: aB, innerInner: AB"
  }

  @Unroll "error when using render in output section - #template"() {
    given:
    templateFile("outer.html") << template

    and:
    ratpackFile << """
      get("/") {
        render "outer.html"
      }
    """

    when:
    startApp()

    then:
    errorText().contains(InvalidTemplateException.name)

    where:
    template << ["\${render 'foo'}", "<%= render 'foo.html' %>"]
  }

  @Unroll "error when using render in output section in nested - #template"() {
    given:
    templateFile("outer.html") << "<% render 'inner.html' %>"
    templateFile("inner.html") << template

    and:
    ratpackFile << """
      get("/") {
        render "outer.html"
      }
    """

    when:
    startApp()

    then:
    errorText().contains(InvalidTemplateException.name)

    where:
    template << ["\${render 'foo'}", "<%= render 'foo.html' %>"]
  }

}
