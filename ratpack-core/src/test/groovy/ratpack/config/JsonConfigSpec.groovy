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

class JsonConfigSpec extends BaseConfigSpec {
  def "supports json"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def configFile = tempFolder.newFile("file.json").toPath()
    configFile.text =
      """
{
    "port": 8080,
    "address": "localhost",
    "development": true,
    "threads": 3,
    "publicAddress": "http://localhost:8080",
    "maxContentLength": 50000,
    "timeResponses": true,
    "indexFiles": ["index.html", "index.htm"],
    "ssl": {
        "keystoreFile": "${keyStoreFile.toString().replaceAll("\\\\", "/")}",
        "keystorePassword": "${keyStorePassword}"
    }
}
"""

    when:
    def serverConfig = ConfigData.of { it.baseDir(baseDir).json(configFile) }.get(ServerConfigData)

    then:
    serverConfig.baseDir == baseDir
    serverConfig.port == 8080
    serverConfig.address == InetAddress.getByName("localhost")
    serverConfig.development
    serverConfig.threads == 3
    serverConfig.publicAddress == URI.create("http://localhost:8080")
    serverConfig.maxContentLength == 50000
    serverConfig.sslContext
  }

  def "supports relative path json"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def keyStoreFile = tempFolder.newFile("keystore.jks").toPath()
    def keyStorePassword = "changeit"
    createKeystore(keyStoreFile, keyStorePassword)
    def configFile = tempFolder.newFile("file.json").toPath()
    configFile.text =
      """
{
    "port": 8080,
    "address": "localhost",
    "development": true,
    "threads": 3,
    "publicAddress": "http://localhost:8080",
    "maxContentLength": 50000,
    "timeResponses": true,
    "indexFiles": ["index.html", "index.htm"],
    "ssl": {
        "keystoreFile": "${keyStoreFile.toString().replaceAll("\\\\", "/")}",
        "keystorePassword": "${keyStorePassword}"
    }
}
"""

    when:
    def serverConfig = ServerConfig.of { it.baseDir(baseDir).json("../file.json") }.get(ServerConfigData)

    then:
    serverConfig.baseDir == baseDir
    serverConfig.port == 8080
    serverConfig.address == InetAddress
      .getByName("localhost")
    serverConfig.development
    serverConfig.threads == 3
    serverConfig.publicAddress == URI.create("http://localhost:8080")
    serverConfig.maxContentLength == 50000
    serverConfig.sslContext
  }

  def "cannot set basedir from json config source"() {
    def baseDir = tempFolder.newFolder("baseDir").toPath()
    def configFile = tempFolder.newFile("file.json").toPath()
    configFile.text = '{"baseDir": "/tmp"}'

    when:
    def serverConfig = ServerConfig.of { it.baseDir(baseDir).json(configFile) }.get(ServerConfigData)

    then:
    serverConfig.baseDir == baseDir
  }
}
