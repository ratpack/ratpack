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

package org.ratpackframework.guice

import com.google.inject.AbstractModule
import org.ratpackframework.error.PrintingServerErrorHandler
import org.ratpackframework.error.ServerErrorHandler
import org.ratpackframework.handling.Context
import org.ratpackframework.render.ByTypeRenderer
import org.ratpackframework.render.NoSuchRendererException
import org.ratpackframework.test.internal.RatpackGroovyDslSpec

class RendererBindingsSpec extends RatpackGroovyDslSpec {

  static class IntRenderer extends ByTypeRenderer<Integer> {
    public IntRenderer() {
      super(Integer)
    }

    @Override
    void render(Context context, Integer object) {
      context.response.send("text/integer", object.toString())
    }
  }

  static class StringRenderer extends ByTypeRenderer<String> {
    public StringRenderer() {
      super(String)
    }

    @Override
    void render(Context context, String object) {
      context.response.send("text/string", object.toString())
    }
  }

  def "bound renderers are usable"() {
    when:
    app {
      modules {
        register new AbstractModule() {
          protected void configure() {
            bind(IntRenderer)
            bind(StringRenderer)
            bind(ServerErrorHandler).to(PrintingServerErrorHandler)
          }
        }
      }

      handlers {
        get("int") {
          render 1
        }
        get("string") {
          render "abc"
        }
        get("none") {
          render new LinkedList()
        }
      }
    }

    then:
    with(get("int")) {
      body.asString() == "1"
      contentType == "text/integer;charset=UTF-8"
    }
    with(get("string")) {
      body.asString() == "abc"
      contentType == "text/string;charset=UTF-8"
    }
    with(get("none")) {
      statusCode == 500
      body.asString().contains(NoSuchRendererException.name)
    }
  }

}
