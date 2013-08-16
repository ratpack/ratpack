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

package org.ratpackframework.java.handling

import org.ratpackframework.handling.Chain
import org.ratpackframework.handling.Context
import org.ratpackframework.handling.Handler
import org.ratpackframework.test.DefaultRatpackSpec

import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED
import static io.netty.handler.codec.http.HttpResponseStatus.OK
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND

class HandlersSpec extends DefaultRatpackSpec {

  def "get chain handler"() {
    given:
    app {
      handlers { Chain handlers ->

        handlers.get("myPath", new Handler() {
          @Override
          void handle(Context context) {
            context.getResponse().send("from the myPath handler");
          }
        })

        handlers.get(new Handler() {
          @Override
          void handle(Context context) {
            context.getResponse().send("from the root path handler");
          }
        })

      }
    }

    when:
    get("myPath")

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().contains("from the myPath handler")
    }

    when:
    get()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().contains("from the root path handler")
    }

    when:
    post("myPath")

    then:
    with (response) {
      statusCode == METHOD_NOT_ALLOWED.code()
    }

    when:
    post()

    then:
    with (response) {
      statusCode == METHOD_NOT_ALLOWED.code()
    }
  }

  def "post chain handler"() {
    given:
    app {
      handlers { Chain handlers ->

        handlers.post("myPath", new Handler() {
          @Override
          void handle(Context context) {
            context.getResponse().send("from the myPath handler");
          }
        })

        handlers.post(new Handler() {
          @Override
          void handle(Context context) {
            context.getResponse().send("from the root path handler");
          }
        })

      }
    }

    when:
    post("myPath")

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().contains("from the myPath handler")
    }

    when:
    post()

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().contains("from the root path handler")
    }

    when:
    get("myPath")

    then:
    with (response) {
      statusCode == METHOD_NOT_ALLOWED.code()
    }

    when:
    get()

    then:
    with (response) {
      statusCode == METHOD_NOT_ALLOWED.code()
    }
  }

  def "path chain handler"() {
    given:
    app {
      handlers { Chain handlers ->

        handlers.path("myPath", new Handler() {
          @Override
          void handle(Context context) {
            context.getResponse().send("from the myPath handler");
          }
        })

      }
    }

    when:
    get("myPath")

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().contains("from the myPath handler")
    }

    when:
    get()

    then:
    with (response) {
      statusCode == NOT_FOUND.code()
    }

    when:
    post("myPath")

    then:
    with (response) {
      statusCode == OK.code()
      body.asString().contains("from the myPath handler")
    }

  }

}
