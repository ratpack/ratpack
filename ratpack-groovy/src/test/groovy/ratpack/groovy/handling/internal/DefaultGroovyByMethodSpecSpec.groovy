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

package ratpack.groovy.handling.internal

import ratpack.func.Block
import ratpack.groovy.handling.GroovyByMethodSpec
import ratpack.handling.ByMethodSpec
import ratpack.handling.Context
import ratpack.handling.Handler
import spock.lang.Specification
import spock.lang.Unroll

class DefaultGroovyByMethodSpecSpec extends Specification {

  static final METHODS = ["GET", "POST", "PUT", "PATCH", "OPTIONS", "DELETE"]

  Context context = Mock(Context)
  ByMethodSpec delegate = Mock(ByMethodSpec)
  GroovyByMethodSpec byMethodSpec = new DefaultGroovyByMethodSpec(delegate, context)

  @Unroll
  def "handle #method with closure"() {
    when:
    byMethodSpec."${method.toLowerCase()}" {
      render("Test $method with closure")
    }

    then:
    0 * context.render(_)
    1 * delegate.named(method, { it instanceof Handler })

    where:
    method << METHODS
  }

  @Unroll
  def "handle #method with named closure"() {
    when:
    byMethodSpec.named(method) {
      render("Test $method with closure")
    }

    then:
    0 * context.render(_)
    1 * delegate.named(method, { it instanceof Handler })

    where:
    method << METHODS
  }

  @Unroll
  def "handle #method with named block"() {
    given:
    def block = Mock(Block)

    when:
    byMethodSpec.named(method, block)

    then:
    1 * delegate.named(method, block)

    where:
    method << METHODS
  }

  @Unroll
  def "handle #method with block"() {
    setup:
    def block = Mock(Block)

    when:
    byMethodSpec."${method.toLowerCase()}"(block)

    then:
    1 * delegate."${method.toLowerCase()}"(block)

    where:
    method << METHODS
  }

  @Unroll
  def "handle #method with Handler instance"() {
    setup:
    def handler = Mock(Handler)

    when:
    byMethodSpec."${method.toLowerCase()}"(handler)

    then:
    1 * delegate."${method.toLowerCase()}"(handler)

    where:
    method << METHODS
  }

  @Unroll
  def "handle #method with clazz"() {
    setup:
    byMethodSpec = new DefaultGroovyByMethodSpec(delegate, context)

    when:
    byMethodSpec."${method.toLowerCase()}"(TestHandler)

    then:
    1 * delegate."${method.toLowerCase()}"(TestHandler)

    where:
    method << METHODS
  }

  private static class TestHandler implements Handler {
    @Override
    void handle(Context ctx) throws Exception {
    }
  }

}
