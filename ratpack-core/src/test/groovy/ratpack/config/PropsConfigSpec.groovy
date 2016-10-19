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

import ratpack.server.ServerConfig
import ratpack.server.internal.DefaultServerConfigBuilder
import ratpack.server.internal.ServerEnvironment
import ratpack.impose.Impositions

class PropsConfigSpec extends BaseConfigSpec {
  def "supports properties"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def trustStoreFile = tempFolder.newFile('truststore.jks').toPath()
    def trustStorePassword = 'something'
    createKeystore(trustStoreFile, trustStorePassword)
    def configFile = tempFolder.newFile("file.properties").toPath()
    configFile.text = """
    |# This is a comment
    |server.port: 8080
    |server.address: localhost
    |server.development: true
    |server.threads: 3
    |server.publicAddress: http://localhost:8080
    |server.maxContentLength: 50000
    |server.indexFiles[0]: index.html
    |server.indexFiles[1]: index.htm
    |server.ssl.keystoreFile: ${keyStoreFile.toString().replaceAll("\\\\", "/")}
    |server.ssl.keystorePassword: ${keyStorePassword}
    |server.ssl.truststoreFile: ${trustStoreFile.toString().replaceAll("\\\\", "/")}
    |server.ssl.truststorePassword: ${trustStorePassword}
    |""".stripMargin()

    when:
    def serverConfig = ServerConfig.of { it.baseDir(baseDir).props(configFile) }

    then:
    serverConfig.baseDir.file == baseDir
    serverConfig.port == 8080
    serverConfig.address == InetAddress.getByName("localhost")
    serverConfig.development
    serverConfig.threads == 3
    serverConfig.publicAddress == URI.create("http://localhost:8080")
    serverConfig.maxContentLength == 50000
    serverConfig.sslContext
  }

  @SuppressWarnings(["UnnecessaryObjectReferences"])
  def "supports system properties"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def trustStoreFile = tempFolder.newFile('truststore.jks').toPath()
    def trustStorePassword = 'something'
    createKeystore(trustStoreFile, trustStorePassword)
    def properties = new Properties()
    properties.with {
      setProperty("ratpack.port", "8080")
      setProperty("ratpack.server.address", "localhost")
      setProperty("ratpack.server.development", "true")
      setProperty("ratpack.server.threads", "3")
      setProperty("ratpack.server.publicAddress", "http://localhost:8080")
      setProperty("ratpack.server.maxContentLength", "50000")
      setProperty("ratpack.server.indexFiles[0]", "index.html")
      setProperty("ratpack.server.indexFiles[1]", "index.htm")
      setProperty("ratpack.server.ssl.keystoreFile", keyStoreFile.toString())
      setProperty("ratpack.server.ssl.keystorePassword", keyStorePassword)
      setProperty("ratpack.server.ssl.truststoreFile", trustStoreFile.toString())
      setProperty("ratpack.server.ssl.truststorePassword", trustStorePassword)
    }

    when:
    def serverConfig = new DefaultServerConfigBuilder(new ServerEnvironment([:], properties), Impositions.none()).sysProps().baseDir(baseDir).build()

    then:
    serverConfig.baseDir.file == baseDir
    serverConfig.port == 8080
    serverConfig.address == InetAddress.getByName("localhost")
    serverConfig.development
    serverConfig.threads == 3
    serverConfig.publicAddress == URI.create("http://localhost:8080")
    serverConfig.maxContentLength == 50000
    serverConfig.sslContext
  }
}
