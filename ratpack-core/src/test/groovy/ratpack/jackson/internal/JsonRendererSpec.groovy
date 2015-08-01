/*
 * Copyright 2015 the original author or authors.
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

package ratpack.jackson.internal
import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import groovy.transform.Canonical
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import ratpack.handling.Context
import ratpack.http.Response
import spock.lang.Specification
/**
 * @author Raniz
 */
class JsonRendererSpec extends Specification {

  static class Views {
    static class Public {}
    static class Secret extends Public {}
  }

  @Canonical
  static class Model {
    @JsonView(Views.Public)
    String open
    @JsonView(Views.Secret)
    String secret
  }

  def objectMapper = new ObjectMapper()
  def renderer = new JsonRenderer(objectMapper.writer())

  Context context = Mock()
  Response response = Mock()

  def setup() {
    context.get(ByteBufAllocator) >> new UnpooledByteBufAllocator(false)
    context.getResponse() >> response
    response.contentTypeIfNotSet((CharSequence)_) >> response
  }

  def json(ByteBuf json, Closure<?> closure) {
    def object = new JsonSlurper().parse(json.array)
    closure.call(object)
    return true
  }

  def "no view renders all fields"() {
    given: "an object to render"
    def object = new Model("foo", "bar")

    when: "the object is rendered without views"
    renderer.render context, new DefaultJsonRender(object, null, null)

    then: "all fields of the object are rendered"
    1 * response.send({ buffer ->
      json(buffer) {
        assert it["open"] == "foo"
        assert it["secret"] == "bar"
      }
    })
  }

  def "secret view renders all fields"() {
    given: "an object to render"
    def object = new Model("foo", "bar")

    when: "the object is rendered with the secret view"
    renderer.render context, new DefaultJsonRender(object, null, Views.Secret)

    then: "all fields of the object are rendered"
    1 * response.send({ buffer ->
      json(buffer) {
        assert it["open"] == "foo"
        assert it["secret"] == "bar"
      }
    })
  }

  def "public view renders only public field"() {
    given: "an object to render"
    def object = new Model("foo", "bar")

    when: "the object is rendered with the public view"
    renderer.render context, new DefaultJsonRender(object, null, Views.Public)

    then: "only public fields of the object are rendered"
    1 * response.send({ buffer ->
      json(buffer) {
        assert it["open"] == "foo"
        assert !("secret" in it)
      }
    })
  }

}
