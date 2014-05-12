/*
 * Copyright 2013 the original author or authors.
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

package ratpack.groovy.render

import ratpack.groovy.handling.GroovyContext
import ratpack.test.internal.RatpackGroovyDslSpec

class GroovyRendererSupportSpec extends RatpackGroovyDslSpec {

  static class IntegerRenderer extends GroovyRendererSupport<Integer> {
    @Override
    void render(GroovyContext context, Integer object) throws Exception {
      context.render(object.toString())
    }
  }

  def "can implement renderer in groovy"() {
    when:
    bindings {
      bind IntegerRenderer
    }
    handlers {
      get {
        render 10
      }
    }

    then:
    text == "10"
  }

}
