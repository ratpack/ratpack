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

import ratpack.config.internal.DefaultConfigDataSpec
import ratpack.server.ServerConfig
import ratpack.server.ServerEnvironment

class PropsConfigSpec extends BaseConfigSpec {
  def "supports properties"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def configFile = tempFolder.newFile("file.properties").toPath()
    configFile.text = """
    |# This is a comment
    |baseDir: ${baseDir.toString().replaceAll("\\\\", "/")}
    |port: 8080
    |address: localhost
    |development: true
    |threads: 3
    |publicAddress: http://localhost:8080
    |maxContentLength: 50000
    |indexFiles[0]: index.html
    |indexFiles[1]: index.htm
    |ssl.keyStorePath: ${keyStoreFile.toString().replaceAll("\\\\", "/")}
    |ssl.keyStorePassword: ${keyStorePassword}
    |""".stripMargin()

    when:
    def serverConfig = ConfigData.of().props(configFile).build().get(ServerConfig)

    then:
    serverConfig.hasBaseDir
    serverConfig.baseDir.file == baseDir
    serverConfig.port == 8080
    serverConfig.address == InetAddress.getByName("localhost")
    serverConfig.development
    serverConfig.threads == 3
    serverConfig.publicAddress == URI.create("http://localhost:8080")
    serverConfig.maxContentLength == 50000
    serverConfig.SSLContext
  }

  @SuppressWarnings(["UnnecessaryObjectReferences"])
  def "supports system properties"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def properties = new Properties()
    properties.with {
      setProperty("ratpack.baseDir", baseDir.toString())
      setProperty("ratpack.port", "8080")
      setProperty("ratpack.address", "localhost")
      setProperty("ratpack.development", "true")
      setProperty("ratpack.threads", "3")
      setProperty("ratpack.publicAddress", "http://localhost:8080")
      setProperty("ratpack.maxContentLength", "50000")
      setProperty("ratpack.indexFiles[0]", "index.html")
      setProperty("ratpack.indexFiles[1]", "index.htm")
      setProperty("ratpack.ssl.keyStorePath", keyStoreFile.toString())
      setProperty("ratpack.ssl.keyStorePassword", keyStorePassword)
    }

    when:
    def serverConfig = new DefaultConfigDataSpec(new ServerEnvironment([:], properties)).sysProps().build().get(ServerConfig)

    then:
    serverConfig.hasBaseDir
    serverConfig.baseDir.file == baseDir
    serverConfig.port == 8080
    serverConfig.address == InetAddress.getByName("localhost")
    serverConfig.development
    serverConfig.threads == 3
    serverConfig.publicAddress == URI.create("http://localhost:8080")
    serverConfig.maxContentLength == 50000
    serverConfig.SSLContext
  }
}
