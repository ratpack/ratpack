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

package ratpack.config.server

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.server.ServerConfig
import ratpack.server.internal.ServerConfigData

import java.nio.file.Path
import java.security.KeyStore

class ServerConfigUsageSpec extends ConfigUsageSpec {
  @Rule
  TemporaryFolder temporaryFolder

  def "can get ServerConfig with defaults from no data"() {
    when:
    def config = ServerConfig.of {}

    then:
    !config.hasBaseDir
    config.port == ServerConfig.DEFAULT_PORT
    !config.address
    !config.development
    config.threads == ServerConfig.DEFAULT_THREADS
    !config.publicAddress
    !config.nettySslContext
    config.maxContentLength == ServerConfig.DEFAULT_MAX_CONTENT_LENGTH
  }

  def "can override all ServerConfig fields"() {
    given:
    def baseDir = temporaryFolder.newFolder().toPath()
    def keyStorePassword = "changeit"
    def keyStoreFile = createKeystore(keyStorePassword)
    def data = """
    |---
    |port: 1234
    |address: 1.2.3.4
    |development: true
    |threads: 5
    |publicAddress: http://app.ratpack.com
    |ssl:
    |  keystoreFile: ${keyStoreFile.toString()}
    |  keystorePassword: ${keyStorePassword}
    |maxContentLength: 54321
    """.stripMargin()

    when:
    def config = yamlConfig(baseDir, data).get(ServerConfigData)

    then:
    config.baseDir.file == baseDir
    config.port == 1234
    config.address == InetAddress.getByAddress([1, 2, 3, 4] as byte[])
    config.development
    config.threads == 5
    config.publicAddress == URI.create("http://app.ratpack.com")
    config.sslContext
    config.maxContentLength == 54321L
  }

  private Path createKeystore(String password) {
    def path = temporaryFolder.newFile().toPath()
    def keyStore = KeyStore.getInstance(KeyStore.defaultType)
    keyStore.load(null, password.toCharArray())
    path.withOutputStream { keyStore.store(it, password.toCharArray()) }
    path
  }
}
