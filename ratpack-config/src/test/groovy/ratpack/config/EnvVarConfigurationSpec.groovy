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

import ratpack.config.internal.DefaultConfigurationDataSpec
import ratpack.server.ServerConfig
import ratpack.server.ServerEnvironment
import spock.lang.Unroll

class EnvVarConfigurationSpec extends BaseConfigurationSpec {
  @SuppressWarnings("GroovyAssignabilityCheck")
  @Unroll
  def "support PORT environment variable: #envData to #expectedPort"() {
    when:
    def serverConfig = new DefaultConfigurationDataSpec(new ServerEnvironment(envData, new Properties())).env().build().get(ServerConfig)

    then:
    serverConfig.port == expectedPort

    where:
    expectedPort | envData
    5432         | [PORT: "5432"]
    8080         | [PORT: "5432", RATPACK_PORT: "8080"]
    8080         | [RATPACK_PORT: "8080"]
  }

  def "supports environment variables"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def envData = [
      RATPACK_BASE_DIR: baseDir.toString(),
      RATPACK_PORT: "8080",
      RATPACK_ADDRESS: "localhost",
      RATPACK_DEVELOPMENT: "true",
      RATPACK_THREADS: "3",
      RATPACK_PUBLIC_ADDRESS: "http://localhost:8080",
      RATPACK_MAX_CONTENT_LENGTH: "50000",
      RATPACK_TIME_RESPONSES: "true",
      RATPACK_COMPRESS_RESPONSES: "true",
      RATPACK_COMPRESSION_MIN_SIZE: "100",
      RATPACK_SSL__KEY_STORE_PATH: keyStoreFile.toString(),
      RATPACK_SSL__KEY_STORE_PASSWORD: keyStorePassword,
      RATPACK_OTHER__A: "1",
      RATPACK_OTHER__B: "2",
    ]

    when:
    def serverConfig = new DefaultConfigurationDataSpec(new ServerEnvironment(envData, new Properties())).env().build().get(ServerConfig)

    then:
    serverConfig.hasBaseDir
    serverConfig.baseDir.file == baseDir
    serverConfig.port == 8080
    serverConfig.address == InetAddress.getByName("localhost")
    serverConfig.development
    serverConfig.threads == 3
    serverConfig.publicAddress == URI.create("http://localhost:8080")
    serverConfig.maxContentLength == 50000
    serverConfig.timeResponses
    serverConfig.compressResponses
    serverConfig.compressionMinSize == 100
    serverConfig.SSLContext
    serverConfig.getOtherPrefixedWith("") == [a:"1", b:"2"]
  }
}
