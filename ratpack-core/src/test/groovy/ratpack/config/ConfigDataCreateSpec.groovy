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
import groovy.transform.Canonical
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.func.Action
import ratpack.server.ServerConfig
import ratpack.server.Service
import ratpack.server.StartEvent
import ratpack.server.internal.ServerConfigData
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.util.concurrent.atomic.AtomicInteger

@SuppressWarnings(["MethodName"])
class ConfigDataCreateSpec extends RatpackGroovyDslSpec {
  @SuppressWarnings("GroovyUnusedDeclaration")
  PollingConditions polling = new PollingConditions()

  @Rule
  TemporaryFolder temporaryFolder

  def "supports initially null children config objects"() {
    def configInput = """
    |service:
    |  url: http://example.com
    |""".stripMargin()

    when:
    def configData = ConfigData.of { it.yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))) }
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
    def configData = ServerConfig.of { it.yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))) }
    def appConfig = configData.get(MyAppConfig)
    def serverConfig = configData.get(ServerConfigData)
    def serviceConfig = configData.get(ServiceConfig)

    then:
    appConfig.name == "Ratpack"
    serverConfig.port == 8080
    serviceConfig.url == "http://example.com"
  }

  def "by default, throws errors on configuration load"() {
    def yamlFile = temporaryFolder.newFolder().toPath().resolve("config.yaml")

    when:
    ConfigData.of { it.yaml(yamlFile).props([port: "8080"]) }

    then:
    def ex = thrown(UncheckedIOException)
    ex.cause instanceof NoSuchFileException
  }

  def "can override errorHandler"() {
    def folder = temporaryFolder.newFolder().toPath()
    def yamlFile = folder.resolve("config.yaml")
    def jsonFile = folder.resolve("config.json")

    when:
    def configData = ServerConfig.of { it.onError(Action.noop()).yaml(yamlFile).json(jsonFile).props([port: "8080"]) }
    def serverConfig = configData.get(ServerConfigData)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
  }

  def "can alternate between error handlers"() {
    def folder = temporaryFolder.newFolder().toPath()
    def yamlFile = folder.resolve("config.yaml")
    def jsonFile = folder.resolve("config.json")

    when:
    ServerConfig.of { it.onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]) }

    then:
    def ex = thrown(UncheckedIOException)
    ex.cause instanceof NoSuchFileException

    when:
    jsonFile.text = '{"threads":7}'
    def configData = ServerConfig.of { it.onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]) }
    def serverConfig = configData.get(ServerConfigData)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
    serverConfig.threads == 7

    when:
    yamlFile.text = 'publicAddress: http://example.com'
    configData = ServerConfig.of { it.onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]) }
    serverConfig = configData.get(ServerConfigData)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
    serverConfig.threads == 7
    serverConfig.publicAddress == URI.create("http://example.com")

    when:
    Files.delete(jsonFile)
    ConfigData.of { it.onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]) }

    then:
    ex = thrown(UncheckedIOException)
    ex.cause instanceof NoSuchFileException
  }

  def "can load config from an object"() {
    when:
    def serviceConfigObj = new ServiceConfig(url: "http://example.com")
    def serviceConfigData = ConfigData.of { it.object("myService", serviceConfigObj) }
    def serviceConfig = serviceConfigData.get("/myService", ServiceConfig)

    then:
    serviceConfig == serviceConfigObj

    when:
    def appConfigObj = new MyAppConfig(name: "app", service: serviceConfigObj)
    def configData = ConfigData.of { it
      .object("app", appConfigObj)
      .object("app.service", new ServiceConfig(url: "changed"))
    }
    def appConfig = configData.get("/app", MyAppConfig)

    then:
    appConfig.name == "app"
    appConfig.service.url == "changed"
  }

  @Canonical
  static class MyAppConfig {
    String name
    ServiceConfig service
  }

  @Canonical
  private static class ServiceConfig {
    String url
  }

  private static class StartCountService implements Service {
    AtomicInteger count = new AtomicInteger()

    @Override
    void onStart(StartEvent event) throws Exception {
      count.incrementAndGet()
    }
  }
}
