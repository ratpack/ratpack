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
import ratpack.api.UncheckedException
import ratpack.func.Action
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.registry.RegistrySpec
import ratpack.server.RatpackServer
import ratpack.server.ReloadInformant
import ratpack.server.ServerConfig
import ratpack.server.ServerLifecycleListener
import ratpack.server.StartEvent
import ratpack.test.ApplicationUnderTest
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@SuppressWarnings(["MethodName"])
class ConfigurationsSpec extends Specification {
  @SuppressWarnings("GroovyUnusedDeclaration")
  PollingConditions polling = new PollingConditions()

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
      def configData = ConfigurationData.of().props(propsFile).build()
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

  def "auto-reload"() {
    given:
    def props = new Properties()
    props.setProperty("name", "Ratpack")
    def propsFile = temporaryFolder.newFile("config.properties").toPath()
    propsFile.withOutputStream { props.store(it, null) }
    def listener = new StartCountServerLifecycleListener()

    when:
    def server = RatpackServer.of {
      def configData = ConfigurationData.of().props(propsFile).build()
      it.config(ServerConfig.embedded())
        .registryOf { RegistrySpec registrySpec ->
          registrySpec.add(ServerLifecycleListener, listener)
          registrySpec.add(MyAppConfig, configData.get(MyAppConfig))
          registrySpec.add(ReloadInformant, configData.reloadInformant.interval(Duration.ofSeconds(1)))
        }
        .handler {
          return { Context context ->
            context.render("Hi, my name is ${it.get(MyAppConfig).name}")
          } as Handler
        }
    }
    def client = ApplicationUnderTest.of(server).httpClient
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
    def configData = ConfigurationData.of().yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))).build()
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
    def configData = ConfigurationData.of().yaml(ByteSource.wrap(configInput.getBytes(Charsets.UTF_8))).build()
    def appConfig = configData.get(MyAppConfig)
    def serverConfig = configData.get(ServerConfig)
    def serviceConfig = configData.get(ServiceConfig)

    then:
    appConfig.name == "Ratpack"
    serverConfig.port == 8080
    serviceConfig.url == "http://example.com"
  }

  def "by default, throws errors on configuration load"() {
    def yamlFile = temporaryFolder.newFolder().toPath().resolve("config.yaml")

    when:
    ConfigurationData.of().yaml(yamlFile).props([port: "8080"]) build()

    then:
    def ex = thrown(UncheckedException)
    ex.cause instanceof NoSuchFileException
  }

  def "can override errorHandler"() {
    def folder = temporaryFolder.newFolder().toPath()
    def yamlFile = folder.resolve("config.yaml")
    def jsonFile = folder.resolve("config.json")

    when:
    def configData = ConfigurationData.of().onError(Action.noop()).yaml(yamlFile).json(jsonFile).props([port: "8080"]).build()
    def serverConfig = configData.get(ServerConfig)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
  }

  def "can alternate between error handlers"() {
    def folder = temporaryFolder.newFolder().toPath()
    def yamlFile = folder.resolve("config.yaml")
    def jsonFile = folder.resolve("config.json")

    when:
    ConfigurationData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()

    then:
    def ex = thrown(UncheckedException)
    ex.cause instanceof NoSuchFileException

    when:
    jsonFile.text = '{"threads":7}'
    def configData = ConfigurationData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()
    def serverConfig = configData.get(ServerConfig)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
    serverConfig.threads == 7

    when:
    yamlFile.text = 'publicAddress: http://example.com'
    configData = ConfigurationData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()
    serverConfig = configData.get(ServerConfig)

    then:
    notThrown(Exception)
    serverConfig.port == 8080
    serverConfig.threads == 7
    serverConfig.publicAddress == URI.create("http://example.com")

    when:
    Files.delete(jsonFile)
    ConfigurationData.of().onError(Action.noop()).yaml(yamlFile).onError(Action.throwException()).json(jsonFile).props([port: "8080"]).build()

    then:
    ex = thrown(UncheckedException)
    ex.cause instanceof NoSuchFileException
  }

  static class MyAppConfig {
    String name
    ServiceConfig service
  }

  private static class ServiceConfig {
    String url
  }

  private static class StartCountServerLifecycleListener implements ServerLifecycleListener {
    AtomicInteger count = new AtomicInteger()

    @Override
    void onStart(StartEvent event) throws Exception {
      count.incrementAndGet()
    }
  }
}
