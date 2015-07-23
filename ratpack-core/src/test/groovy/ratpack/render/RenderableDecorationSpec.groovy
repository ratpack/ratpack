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

package ratpack.render

import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec

class RenderableDecorationSpec extends RatpackGroovyDslSpec {

  def "can decorate renderables"() {
    when:
    handlers {
      register {
        with(RenderableDecorator.of(String) { c, i -> i + "1" }.register())
        with(RenderableDecorator.of(String) { c, i -> i + "2" }.register())
      }
      register {
        with(RenderableDecorator.of(String) { c, i -> i + "3" }.register())
        with(RenderableDecorator.of(String) { c, i -> i + "4" }.register())
      }
      get { render("a") }
    }

    then:
    text == "a4321"
  }

  def "decorator can be async"() {
    when:
    handlers {
      register {
        with(RenderableDecorator.ofAsync(String) { c, i -> Blocking.get { i + "1" } }.register())
        with(RenderableDecorator.ofAsync(String) { c, i -> Blocking.get { i + "2" } }.register())
      }
      get { render("a") }
    }

    then:
    text == "a21"
  }
}
