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
import ratpack.func.Action
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

  def "reload server"() {
    given:
    def props = new Properties()
    props.setProperty("server.port", "0")
    props.setProperty("app.name", "Ratpack")
    def propsFile = temporaryFolder.newFile("config.properties").toPath()
    propsFile.withOutputStream { props.store(it, null) }

    when:
    serverConfig {
      it.props(props)
    }
    bindings {
      bindInstance(MyAppConfig, serverConfig.get("/app", MyAppConfig))
    }
    handlers {
      handler {
        render("Hi, my name is ${get(MyAppConfig).name}")
      }
    }

    server.start()

    then:
    server.isRunning()
    client.text == "Hi, my name is Ratpack"

    when:
    props.setProperty("app.name", "Ratpack!")
    propsFile.withOutputStream { props.store(it, null) }
    server.reload()

    then:
    server.isRunning()
    client.text == "Hi, my name is Ratpack!"
  }

  def "auto-reload"() {
    given:
    def props = new Properties()
    props.setProperty("name", "Ratpack")
    def propsFile = temporaryFolder.newFile("config.properties").toPath()
    propsFile.withOutputStream { props.store(it, null) }
    def listener = new StartCountService()

    when:
    serverConfig {
      development true
      threads 4
    }
    bindings {
      def configData = ConfigData.of().props(propsFile).build()
      bindInstance(listener)
      bindInstance(configData.get(MyAppConfig))
      bindInstance(configData)
    }
    handlers {
      handler {
        render("Hi, my name is ${get(MyAppConfig).name}")
      }
    }

    server.start()

    then:
    client.text == "Hi, my name is Ratpack"
    listener.count.get() == 1

    when: "the config changes"
    props.setProperty("name", "Ratpack!")
    propsFile.withOutputStream { props.store(it, null) }
    then: "a subsequent request results in a reload"
    polling.within(2) {
      client.text == "Hi, my name is Ratpack!"
      listener.count.get() == 2
    }

    when: "the config doesn't change"
    then: "requests don't result in a reload"
    // Need more than one, because the first request after a reload may not check reload informers
    client.text == "Hi, my name is Ratpack!"
    client.text == "Hi, my name is Ratpack!"
    listener.count.get() == 2
  }

  def "supports initially null children config objects"() {
    def configInput = """
    |service:
    |  url: http://example.com
    |""".stripMargin()

    when:
    def configData = ConfigData.of().yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))).build()
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
    def configData = ConfigData.of().yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))).build()
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
    ConfigData.of().yaml(yamlFile).props([port: "8080"]) build()

    then:
    def ex = thrown(UncheckedIOException)
    ex.cause instanceof NoSuchFileException
  }

  def "can override errorHandler"() {
    def folder = temporaryFolder.newFolder().toPath()
    def yamlFile = folder.resolve("config.yaml")
    def jsonFile = folder.resolve("config.json")

    when:
    def configData = ConfigData.of().onError(Action.noop()).yaml(yamlFile).json(jsonFile).props([port: "8080"]).build()
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
    ConfigData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()

    then:
    def ex = thrown(UncheckedIOException)
    ex.cause instanceof NoSuchFileException

    when:
    jsonFile.text = '{"threads":7}'
    def configData = ConfigData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()
    def serverConfig = configData.get(ServerConfigData)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
    serverConfig.threads == 7

    when:
    yamlFile.text = 'publicAddress: http://example.com'
    configData = ConfigData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()
    serverConfig = configData.get(ServerConfigData)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
    serverConfig.threads == 7
    serverConfig.publicAddress == URI.create("http://example.com")

    when:
    Files.delete(jsonFile)
    ConfigData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()

    then:
    ex = thrown(UncheckedIOException)
    ex.cause instanceof NoSuchFileException
  }

  static class MyAppConfig {
    String name
    ServiceConfig service
  }

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
