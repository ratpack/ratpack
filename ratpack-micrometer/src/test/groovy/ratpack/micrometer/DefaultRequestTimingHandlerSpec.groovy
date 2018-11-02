/*
 * Copyright 2018 the original author or authors.
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
package ratpack.micrometer

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.groovy.Groovy
import ratpack.groovy.test.handling.GroovyRequestFixture
import ratpack.handling.Context
import ratpack.micrometer.internal.DefaultRequestTimingHandler
import spock.lang.Specification
import spock.lang.Subject

class DefaultRequestTimingHandlerSpec extends Specification {

  def meterRegistry = new SimpleMeterRegistry()
  def dummyHandler = { Context ctx -> ctx.response.send("Dummy endpoint") }

  @Subject
  RequestTimingHandler timingHandler = new DefaultRequestTimingHandler(meterRegistry, new MicrometerConfig("my-app", [:]))

  def "measure '/' path to root metric and track status code and method use"() {
    given:
    def handlers = Groovy.chain {
      all timingHandler
      get dummyHandler
    }

    when:
    def result = GroovyRequestFixture.handle(handlers) {}

    then: "measure endpoint timing"
    result.status.code == HttpResponseStatus.OK.code()
    result.bodyText.contains("Dummy endpoint")

    meterRegistry.meters.size() == 2
    meterRegistry.get("http.requests").meter().id.tags.containsAll([
      Tag.of("status", "200"),
      Tag.of("path", "root"),
      Tag.of("method", "get")
    ])

    and: "count all http requests received by status code"
    meterRegistry.get("http.server.requests").meter().id.tags == [Tag.of("status", "200")]
  }

  def "a path uses its own metric tag"() {
    given:
    def handlers = Groovy.chain {
      all(timingHandler)
      get("my-own-path", dummyHandler)
    }

    when:
    GroovyRequestFixture.handle(handlers) { uri "my-own-path" }

    then:
    meterRegistry.get("http.requests").meter().id.tags.contains(Tag.of("path", "my-own-path"))
  }

  def "we can group metrics using regex"() {
    given:
    timingHandler = new DefaultRequestTimingHandler(meterRegistry,
      new MicrometerConfig("my-app", ["id": "investor/[^/.]+/product"]))

    def handlers = Groovy.chain {
      all(timingHandler)
      get("investor/123/product", dummyHandler)
    }

    when:
    GroovyRequestFixture.handle(handlers) { uri "investor/123/product" }

    then:
    meterRegistry.get("http.requests").meter().id.tags.contains(Tag.of("path", "id"))
  }

  def "we can group metrics using regex and groups on the key"() {
    given:
    timingHandler = new DefaultRequestTimingHandler(meterRegistry,
      new MicrometerConfig("my-app", [
        'investor/id/$1'             : 'investor/[^/.]+/([^/.]*)$',
        'investor/id/$1/productId/$2': 'investor/[^/.]+/([^/.]+)/[^/.]+/([^/.]+)'
      ]))

    def handlers = Groovy.chain {
      all(timingHandler)
      get("investor/:id/group", dummyHandler)
      get("investor/:id/product/:productId/shipping", dummyHandler)
    }

    when:
    GroovyRequestFixture.handle(handlers) { uri "investor/12345/product/54321/shipping" }

    then:
    meterRegistry.get("http.requests").meter().id.tags.contains(Tag.of("path", "investor/id/product/productId/shipping"))
  }

  def "wrong pattern group should not crash and return dummy"() {
    given:
    timingHandler = new DefaultRequestTimingHandler(meterRegistry,
      new MicrometerConfig("my-app", [
        'investor/id/$1': 'investor/[^/.]+/[^/.]*)$',
      ]))

    def handlers = Groovy.chain {
      all(timingHandler)
      get("investor/:id/group", dummyHandler)
    }

    when:
    def result = GroovyRequestFixture.handle(handlers) { uri "investor/12345/group" }

    then:
    result.bodyText.contains("Dummy endpoint")
  }
}
