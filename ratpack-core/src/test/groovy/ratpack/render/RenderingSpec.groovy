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

import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.handling.Context
import ratpack.registry.Registry
import ratpack.test.internal.RatpackGroovyDslSpec

class RenderingSpec extends RatpackGroovyDslSpec {

  def "rendering null produces 404"() {
    when:
    handlers {
      get {
        render null
      }
    }

    then:
    get().statusCode == 404
  }

  static class Thing {
    final String name

    Thing(String name) {
      this.name = name
    }

  }

  static class ThingRenderer extends RendererSupport<Thing> {
    @Override
    void render(Context ctx, Thing object) throws Exception {
      ctx.render("thing: $object.name")
    }
  }

  def "can use available renderers"() {
    when:
    bindings {
      bindInstance ServerErrorHandler, new DefaultDevelopmentErrorHandler()
    }
    handlers {
      register(Registry.single(new ThingRenderer())) {
        get {
          render new Thing("foo")
        }
      }
      get("not-registered") {
        render new Thing("foo")
      }
    }

    then:
    getText() == "thing: foo"
    with(get("not-registered")) {
      statusCode == 500
      body.text.contains NoSuchRendererException.name
    }
  }
}
