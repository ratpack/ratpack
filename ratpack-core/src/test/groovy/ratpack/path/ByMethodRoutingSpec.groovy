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

package ratpack.path

import ratpack.func.Block
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.test.internal.RatpackGroovyDslSpec

class ByMethodRoutingSpec extends RatpackGroovyDslSpec {

  def "can use method chain"() {
    when:
    handlers {
      path("foo") {
        byMethod {
          get {
            response.send(": get")
          }
          post {
            response.send(": post")
          }
        }
      }
    }

    then:
    getText("foo") == ": get"
    postText("foo") == ": post"
    put("foo").statusCode == 405
    head("foo").statusCode == 200
    head("foo").headers."content-length" == "5" // length of GET response
    head("foo").body.buffer.readableBytes() == 0
  }

  class SendHandler implements Handler {
    @Override
    void handle(Context ctx) throws Exception {
      ctx.render(ctx.request.method.name.toLowerCase())
    }
  }

  def "can use #method"() {
    when:
    bindings {
      bindInstance(SendHandler, new SendHandler())
    }
    handlers {
      path("closure") {
        byMethod {
          delegate."$method" { response.send(method) }
        }
      }
      path("block") {
        byMethod {
          delegate."$method"({ response.send(method) } as Block)
        }
      }
      path("handler") {
        byMethod {
          delegate."$method"({ response.send(method) } as Handler)
        }
      }
      path("class") {
        byMethod {
          delegate."$method"(SendHandler)
        }
      }
    }

    then:
    request("closure") { it.method(method) }.body.text == method
    request("block") { it.method(method) }.body.text == method
    request("handler") { it.method(method) }.body.text == method
    request("class") { it.method(method) }.body.text == method

    where:
    method << [
      HttpMethod.GET,
      HttpMethod.POST,
      HttpMethod.PUT,
      HttpMethod.DELETE,
      HttpMethod.PATCH,
      HttpMethod.OPTIONS,
    ]*.name*.toLowerCase()
  }

}
