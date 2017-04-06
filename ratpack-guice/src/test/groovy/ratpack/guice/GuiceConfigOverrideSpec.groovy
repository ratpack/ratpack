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

package ratpack.guice

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import ratpack.server.ServerConfig
import ratpack.test.internal.RatpackGroovyDslSpec

class GuiceConfigOverrideSpec extends RatpackGroovyDslSpec {

  def "can override config with a guice binding"() {
    when:
    serverConfig {
      props(foo: "bar")
      require("/foo", String)
    }
    bindings {
      bindInstance(String, "baz")
    }
    handlers {
      get {
        render get(String)
      }
    }

    then:
    text == "baz"
  }

  static class Thing {
    final String value

    Thing(@JsonProperty("value") String value) {
      this.value = value
    }
  }

  def "can override config with a specific module"() {
    when:
    serverConfig {
      object("thing", new Thing("foo"))
      require("/thing", Thing)
    }
    bindings {
      module new AbstractModule() {
        @Override
        protected void configure() {

        }

        @Singleton
        @Provides
        Thing thing(ServerConfig serverConfig) {
          Thing fromConf = serverConfig.requiredConfig.find { it.type == Thing }.object as Thing
          new Thing(fromConf.value + ":bar")
        }
      }
    }
    handlers {
      get {
        render get(Thing).value
      }
    }

    then:
    text == "foo:bar"
  }

}
