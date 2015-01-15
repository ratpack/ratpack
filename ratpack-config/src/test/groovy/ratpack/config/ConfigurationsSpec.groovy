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

import com.google.common.base.Charsets
import com.google.common.io.ByteSource
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.handling.Handler
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import ratpack.test.ApplicationUnderTest
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
    def server = RatpackServer.of {
      def configData = Configurations.config().props(propsFile).build()
      it.config(configData.get("/server", ServerConfig))
        .registryOf {
          it.add(MyAppConfig, configData.get("/app", MyAppConfig))
        }
        .handler {
          return {
            it.render("Hi, my name is ${it.get(MyAppConfig).name}")
          } as Handler
        }
    }
    def client = ApplicationUnderTest.of(server).httpClient
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

  def "supports initially null children config objects"() {
    def configInput = """
    |service:
    |  url: http://example.com
    |""".stripMargin()

    when:
    def configData = Configurations.config().yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))).build()
    def config = configData.get(MyAppConfig)

    then:
    config.service.url == "http://example.com"
  }

  def "by default, doesn't fail on unexpected properties"() {
    def configInput = """
    |name: Ratpack
    |port: 8080
    |url: http://example.com
    |""".stripMargin()

    when:
    def configData = Configurations.config().yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))).build()
    def appConfig = configData.get(MyAppConfig)
    def serverConfig = configData.get(ServerConfig)
    def serviceConfig = configData.get(ServiceConfig)

    then:
    appConfig.name == "Ratpack"
    serverConfig.port == 8080
    serviceConfig.url == "http://example.com"
  }

  static class MyAppConfig {
    String name
    ServiceConfig service
  }

  private static class ServiceConfig {
    String url
  }
}
