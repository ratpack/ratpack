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

package ratpack.server

import com.google.common.reflect.TypeToken
import com.google.inject.Provides
import ratpack.guice.ConfigurableModule
import ratpack.test.internal.RatpackGroovyDslSpec

class RequiredServerConfigSpec extends RatpackGroovyDslSpec {

  static class ConfigItem {
    String value
  }

  def "can declare required config data"() {
    when:
    serverConfig {
      props("config.value": "foo")
      require("/config", ConfigItem)
    }
    handlers {
      get {
        render get(ConfigItem).value
      }
    }

    then:
    text == "foo"
  }

  def "can declare required config as parameterised type"() {
    when:
    def type = new TypeToken<HashMap<String, Integer>>() {}
    serverConfig {
      props("config.v1": "1")
      props("config.v2": "2")
      require("/config", type)
    }
    handlers {
      get {
        assert serverConfig.requiredConfig.toList().first().typeToken == type
        render serverConfig.requiredConfig.toList().first().object.toString()
      }
    }

    then:
    text == [v1: 1, v2: 2].toString()
  }

  static class ConfigModule extends ConfigurableModule<ConfigItem> {
    @Override
    protected void configure() {

    }

    @Provides
    String value(ConfigItem configItem) {
      configItem.value
    }
  }

  def "can use required config as module config object"() {
    when:
    serverConfig {
      props("config.value": "foo")
      require("/config", ConfigItem)
    }
    bindings {
      module ConfigModule
    }
    handlers {
      get {
        render get(String)
      }
    }

    then:
    text == "foo"
  }
}
