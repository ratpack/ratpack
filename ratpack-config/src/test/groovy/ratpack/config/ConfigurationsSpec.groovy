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

package ratpack.config

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import ratpack.test.ServerBackedApplicationUnderTest
import ratpack.test.http.TestHttpClients
import spock.lang.Specification

class ConfigurationsSpec extends Specification {
  @Rule
  TemporaryFolder temporaryFolder

  def "reload server"() {
    given:
    def props = new Properties()
    props.setProperty("server.port", "5050")
    props.setProperty("app.name", "Ratpack")
    def propsFile = temporaryFolder.newFile("config.properties").toPath()
    propsFile.withOutputStream { props.store(it, null) }

    when:
    def server = RatpackServer.of { serverDef ->
      def configData = Configurations.config().props(propsFile).build()
      serverDef.config(configData.get("/server", ServerConfig)).registry({ RegistrySpec registrySpec ->
        registrySpec.add(MyAppConfig, configData.get("/app", MyAppConfig))
      }).build { Registry registry ->
        { Context ctx ->
          ctx.render("Hi, my name is ${ctx.get(MyAppConfig).name}")
        } as Handler
      }
    }
    def client = TestHttpClients.testHttpClient(new ServerBackedApplicationUnderTest({ server }))
    server.start()

    then:
    server.isRunning()
    server.bindPort == 5050
    client.text == "Hi, my name is Ratpack"

    when:
    props.setProperty("server.port", "5051")
    props.setProperty("app.name", "Ratpack!")
    propsFile.withOutputStream { props.store(it, null) }
    server.reload()

    then:
    server.isRunning()
    server.bindPort == 5051
    client.text == "Hi, my name is Ratpack!"
  }

  static class MyAppConfig {
    String name
  }
}
