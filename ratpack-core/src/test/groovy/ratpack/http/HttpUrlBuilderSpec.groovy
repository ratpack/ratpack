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

package ratpack.http

import com.google.common.collect.ImmutableMultimap
import ratpack.groovy.internal.ClosureUtil
import ratpack.util.internal.ImmutableDelegatingMultiValueMap
import spock.lang.Specification
import spock.lang.Unroll

class HttpUrlBuilderSpec extends Specification {

  String build(@DelegatesTo(HttpUrlBuilder) Closure<?> closure) {
    def builder = HttpUrlBuilder.http()
    ClosureUtil.configureDelegateFirst(builder, closure)
    builder.build().toString()
  }

  String build(String string) {
    HttpUrlBuilder.base(new URI(string)).build().toString()
  }

  def "empty builder"() {
    expect:
    build {} == "http://localhost"
  }

  def "custom port"() {
    expect:
    build { port(81) } == "http://localhost:81"
  }

  def "secure"() {
    expect:
    build { secure() } == "https://localhost"
  }

  def "secure custom port"() {
    expect:
    build { secure().port(20) } == "https://localhost:20"
  }

  def "host"() {
    expect:
    build { host("foo") } == "http://foo"
    build { host("foo.bar-baz") } == "http://foo.bar-baz"
    build { host("foo.bar-baz.0.Z") } == "http://foo.bar-baz.0.Z"
    build { host("foo").host("bar") } == "http://bar"
  }

  def "invalid host"() {
    when:
    build { host("f o") }

    then:
    thrown IllegalArgumentException
  }

  def "path"() {
    expect:
    build { path("f o/b r") } == "http://localhost/f%20o/b%20r"
    build { path("f o/b r").path("b z") } == "http://localhost/f%20o/b%20r/b%20z"
  }

  def "path segments"() {
    expect:
    build { segment("f o/b r") } == "http://localhost/f%20o%2Fb%20r"
    build { segment("f o/b r").path("b z") } == "http://localhost/f%20o%2Fb%20r/b%20z"
    build { segment("%s", "a/b") } == "http://localhost/a%2Fb"
    build { segment("% a") } == "http://localhost/%25%20a"
    build { segment("%% a %s", "b") } == "http://localhost/%25%20a%20b"
    build { segment("%% a %s", "%") } == "http://localhost/%25%20a%20%25"
  }

  def "params from string"() {
    expect:
    build { params "a" } == "http://localhost?a"
    build { params "a", "b" } == "http://localhost?a=b"
    build { params "a", "b", "a" } == "http://localhost?a=b&a"
    build { params "a", "b", "a", "c" } == "http://localhost?a=b&a=c"
    build { params "a", "1 ?", "a", "2 ?", "b ?" } == "http://localhost?a=1+%3F&a=2+%3F&b+%3F"
  }

  def "params from map"() {
    expect:
    build { params a: "" } == "http://localhost?a"
    build { params a: "b" } == "http://localhost?a=b"
    build { params a: "1 ?", "b ?": "" } == "http://localhost?a=1+%3F&b+%3F"
  }

  def "params from multi map"() {
    expect:
    build { params ImmutableMultimap.of("a", "") } == "http://localhost?a"
    build { params ImmutableMultimap.of("a", "b") } == "http://localhost?a=b"
    build { params ImmutableMultimap.of("a", "b", "a", "c") } == "http://localhost?a=b&a=c"
    build { params ImmutableMultimap.of("a", "1 ?", "b ?", "") } == "http://localhost?a=1+%3F&b+%3F"
  }

  def "params from request multi value map"() {
    expect:
    build { params new ImmutableDelegatingMultiValueMap([a: [""]]) } == "http://localhost?a"
    build { params new ImmutableDelegatingMultiValueMap([a: ["b"]]) } == "http://localhost?a=b"
    build { params new ImmutableDelegatingMultiValueMap([a: ["b", "c"]]) } == "http://localhost?a=b&a=c"
  }

  def "can append already encoded path"() {
    expect:
    build { path "foo%2Fbar" } == "http://localhost/foo%252Fbar"
    build { encodedPath "foo%2Fbar" } == "http://localhost/foo%2Fbar"
  }

  def "custom fragment"() {
    expect:
    build { fragment("a4") } == "http://localhost#a4"
  }

  @Unroll
  def "round trip - #string"() {
    expect:
    build(string) == string

    where:
    string << [
      "http://localhost",
      "http://localhost:90",
      "https://localhost",
      "https://localhost:90",
      "http://foo.bar",
      "http://foo.bar/a/b/c",
      "http://foo.bar/a%3Fb",
      "http://foo.bar/a+b",
      "http://foo.bar/a?a",
      "http://foo.bar/a?a=b&a=c",
      "http://foo.bar/a?a%3F1=b%3F1&a%3F1=c",
      "http://foo.bar/a/",
      "http://foo.com#a1",
      "http://foo.bar/a?a%3F1=b%3F1&a%3F1=c#a1",
      "http://foo.bar/a+b#a1"

    ]
  }

}
