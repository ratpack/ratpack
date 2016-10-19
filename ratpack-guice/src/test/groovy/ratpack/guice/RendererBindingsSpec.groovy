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

package ratpack.guice

import com.google.inject.AbstractModule
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.handling.Context
import ratpack.render.NoSuchRendererException
import ratpack.render.RendererSupport
import ratpack.test.internal.RatpackGroovyDslSpec

import java.time.Instant
import java.time.format.DateTimeFormatter

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE

class RendererBindingsSpec extends RatpackGroovyDslSpec {

  static class IntRenderer extends RendererSupport<Integer> {
    @Override
    void render(Context ctx, Integer object) {
      ctx.response.send("text/integer", object.toString())
    }
  }

  static class InstantRenderer extends RendererSupport<Instant> {
    @Override
    void render(Context ctx, Instant object) {
      ctx.response.send("text/instant", DateTimeFormatter.ISO_INSTANT.format(object))
    }
  }

  def "bound renderers are usable"() {
    when:
    bindings {
      module new AbstractModule() {
        protected void configure() {
          bind(IntRenderer)
          bind(InstantRenderer)
          bind(ServerErrorHandler).to(DefaultDevelopmentErrorHandler)
        }
      }
    }

    handlers {
      get("int") {
        render 1
      }
      get("instant") {
        render Instant.EPOCH
      }
      get("none") {
        render new LinkedList()
      }
    }

    then:
    with(get("int")) {
      body.text == "1"
      headers.get(CONTENT_TYPE) == "text/integer"
    }
    with(get("instant")) {
      body.text == "1970-01-01T00:00:00Z"
      headers.get(CONTENT_TYPE) == "text/instant"
    }
    with(get("none")) {
      statusCode == 500
      body.text.contains(NoSuchRendererException.name)
    }
  }

}
