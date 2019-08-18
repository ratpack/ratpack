/*
 * Copyright 2016 the original author or authors.
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

package ratpack.consul

import com.google.common.net.HostAndPort
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

import static ratpack.jackson.Jackson.json


class ConsulConfigSpec extends Specification {

  @AutoCleanup
  EmbeddedApp consul = GroovyEmbeddedApp.of {
    handlers {
      prefix("v1") {
        get("agent/self") {
          render "ok"
        }
        get("kv/:key") {
          render json([
            [
              CreateIndex: 100,
              ModifyIndex: 200,
              LockIndex  : 200,
              Key        : getPathTokens().get("key"),
              Flags      : 0,
              Value      : Base64.encoder.encodeToString(data[getPathTokens().get("key")].bytes),
              Session    : "adf4238a-882b-9ddc-4a9d-5b6758e4159e"
            ]
          ])
        }
      }
    }
  }

  Map<String, String> data = [
    "yaml": 'name: app-yaml',
    "json": '{"name": "app-json"}',
    "props": 'name=app-properties'
  ]

  def "can read yaml config from Consul"() {
    given:
    EmbeddedApp app = GroovyEmbeddedApp.of {
        serverConfig {
          port 0
          yaml RatpackConsulConfig.value("yaml") { builder -> builder
            builder.withHostAndPort(HostAndPort.fromParts("localhost", consul.address.port))
          }
          require("", AppConfig)
        }
      handlers {
        get { AppConfig config ->
          render json(config)
        }
      }
    }

    when:
    String c = app.httpClient.getText()

    then:
    c == '{"name":"app-yaml"}'

    cleanup:
    app.close()
  }

  def "can read json config from Consul"() {
    given:
    EmbeddedApp app = GroovyEmbeddedApp.of {
      serverConfig {
        port 0
        it.json RatpackConsulConfig.value("json") { builder ->
          builder.withHostAndPort(HostAndPort.fromParts("localhost", consul.address.port))
        }
        require("", AppConfig)
      }
      handlers {
        get { AppConfig config ->
          render json(config)
        }
      }
    }

    when:
    String c = app.httpClient.getText()

    then:
    c == '{"name":"app-json"}'

    cleanup:
    app.close()
  }

  def "can read properties config from Consul"() {
    given:
    EmbeddedApp app = GroovyEmbeddedApp.of {
      serverConfig {
        port 0
        props RatpackConsulConfig.value("props") { builder ->
          builder.withHostAndPort(HostAndPort.fromParts("localhost", consul.address.port))
        }
        require("", AppConfig)
      }
      handlers {
        get { AppConfig config ->
          render json(config)
        }
      }
    }

    when:
    String c = app.httpClient.getText()

    then:
    c == '{"name":"app-properties"}'

    cleanup:
    app.close()
  }

  static class AppConfig {
    String name
  }

}
