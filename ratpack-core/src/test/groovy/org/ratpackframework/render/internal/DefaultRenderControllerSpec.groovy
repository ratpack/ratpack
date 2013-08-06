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

package org.ratpackframework.render.internal

import com.google.common.collect.ImmutableList
import org.ratpackframework.handling.Context
import org.ratpackframework.http.Response
import org.ratpackframework.render.NoSuchRendererException
import org.ratpackframework.render.Renderer
import spock.lang.Specification

class DefaultRenderControllerSpec extends Specification {

  def response = Mock(Response)
  def context = Mock(Context) {
    getResponse() >> response
  }

  def "has suitable tostring"() {
    when:
    def r1 = renderer("r1")
    def r2 = renderer("r2")
    def r3 = renderer("r3")
    def c1 = new DefaultRenderController(null, r(r1, r2))
    def c2 = new DefaultRenderController(c1, r(r3))

    then:
    c1.toString() == "RenderController[r1, r2]"
    c2.toString() == "RenderController[r1, r2] -> RenderController[r3]"
  }

  def "can resolve renderers"() {
    given:
    def r1 = renderer("r1")
    def r2 = renderer("r2")
    def r3 = renderer("r3")
    def c1 = new DefaultRenderController(null, r(r1, r2))
    def c2 = new DefaultRenderController(c1, r(r3))

    when:
    c2.render(context, "r1")

    then:
    1 * response.send("R1")

    when:
    c2.render(context, "r3")

    then:
    1 * response.send("R3")

    when:
    c2.render(context, "unknown")

    then:
    thrown NoSuchRendererException
  }

  def "operations"() {

  }

  Renderer renderer(String s) {
    Mock(Renderer) {
      toString() >> s
      accept(_) >> { String arg -> arg == s ? s.toUpperCase() : null }
      render(_, _) >> { Context context, String value -> context.response.send(value) }
    }
  }

  ImmutableList<Renderer<?>> r(Renderer<?>... renderers) {
    ImmutableList.<Renderer> copyOf(Arrays.asList(renderers))
  }
}
