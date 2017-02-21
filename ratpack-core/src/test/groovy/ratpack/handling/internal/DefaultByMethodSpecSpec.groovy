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

package ratpack.handling.internal

import com.google.common.collect.Maps
import ratpack.func.Block
import ratpack.handling.ByMethodSpec
import ratpack.handling.Context
import ratpack.handling.Handler
import spock.lang.Specification
import spock.lang.Unroll

class DefaultByMethodSpecSpec extends Specification {

  static final METHODS = ["GET", "POST", "PUT", "PATCH", "OPTIONS", "DELETE"]

  TestHandler handler = new TestHandler()
  Context context = Mock(Context)
  Map<String, Block> blocks = Maps.newLinkedHashMap()
  ByMethodSpec byMethodSpec = new DefaultByMethodSpec(blocks, context)

  @Unroll
  def "handle #method with named block"() {
    given:
    def block = Mock(Block)

    when:
    byMethodSpec.named(method, block)

    then:
    blocks.get(method) == block

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
    blocks.get(method) == block

    where:
    method << METHODS
  }

  @Unroll
  def "handle #method with Handler instance"() {
    when:
    byMethodSpec."${method.toLowerCase()}"(handler)

    then:
    blocks.get(method) != null

    when:
    blocks.get(method).execute()

    then:
    1 * context.insert(handler)

    where:
    method << METHODS
  }

  @Unroll
  def "handle #method with clazz"() {
    setup:
    context = Mock(Context) {
      1 * get(TestHandler) >> handler
    }
    byMethodSpec = new DefaultByMethodSpec(blocks, context)

    when:
    byMethodSpec."${method.toLowerCase()}"(TestHandler)

    then:
    blocks.get(method) != null

    when:
    blocks.get(method).execute()

    then:
    1 * context.insert(handler)

    where:
    method << METHODS
  }

  private static class TestHandler implements Handler {
    @Override
    void handle(Context ctx) throws Exception {
    }
  }

}
