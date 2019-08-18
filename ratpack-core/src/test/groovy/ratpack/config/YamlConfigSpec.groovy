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
import ratpack.server.internal.ServerConfigData

class YamlConfigSpec extends BaseConfigSpec {
  def "supports yaml"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def trustStoreFile = tempFolder.newFile('truststore.jks').toPath()
    def trustStorePassword = 'something'
    createKeystore(trustStoreFile, trustStorePassword)
    def configFile = tempFolder.newFile("baseDir/file.yaml").toPath()
    configFile.text = """
    |---
    |# This is a comment
    |server:
    |  port: 8080
    |  address: localhost
    |  development: true
    |  threads: 3
    |  publicAddress: http://localhost:8080
    |  maxContentLength: 50000
    |  indexFiles:
    |      - index.html
    |      - index.htm
    |  ssl:
    |      keystoreFile: ${keyStoreFile.toString()}
    |      keystorePassword: ${keyStorePassword}
    |      truststoreFile: ${trustStoreFile.toString()}
    |      truststorePassword: ${trustStorePassword}
    |...
    |""".stripMargin()

    when:
    def serverConfig = ServerConfig.of { it.baseDir(baseDir).yaml(configFile) }

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

  def "supports relative path yaml"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def trustStoreFile = tempFolder.newFile('truststore.jks').toPath()
    def trustStorePassword = 'something'
    createKeystore(trustStoreFile, trustStorePassword)
    def configFile = tempFolder.newFile("baseDir/file.yaml").toPath()
    configFile.text = """
    |---
    |# This is a comment
    |port: 8080
    |address: localhost
    |development: true
    |threads: 3
    |publicAddress: http://localhost:8080
    |maxContentLength: 50000
    |indexFiles:
    |    - index.html
    |    - index.htm
    |ssl:
    |    keystoreFile: ${keyStoreFile.toString()}
    |    keystorePassword: ${keyStorePassword}
    |    truststoreFile: ${trustStoreFile.toString()}
    |    truststorePassword: ${trustStorePassword}
    |...
    |""".stripMargin()

    when:
    def serverConfig = ServerConfig.of { it.baseDir(baseDir).yaml("file.yaml") }.get(ServerConfigData)

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

  def "cannot set basedir from yaml config source"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def configFile = tempFolder.newFile("baseDir/file.yaml").toPath()
    configFile.text = 'server:\n  baseDir: /tmp'

    when:
    ServerConfig.of { it.baseDir(baseDir).yaml(configFile) }

    then:
    thrown IllegalStateException
  }

  def "handles empty yaml file"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def configFile = tempFolder.newFile("baseDir/file.yaml").toPath()
    configFile.text = ''

    when:
    ServerConfig.of { it.baseDir(baseDir).yaml(configFile) }

    then:
    noExceptionThrown()
  }
}
