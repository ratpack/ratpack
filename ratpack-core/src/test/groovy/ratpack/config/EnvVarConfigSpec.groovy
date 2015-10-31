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

import ratpack.server.internal.DefaultServerConfigBuilder
import ratpack.server.internal.ServerEnvironment
import io.netty.handler.ssl.util.SelfSignedCertificate;
import spock.lang.Unroll

class EnvVarConfigSpec extends BaseConfigSpec {
  @SuppressWarnings("GroovyAssignabilityCheck")
  @Unroll
  def "support PORT environment variable: #envData to #expectedPort"() {
    when:
    def serverConfig = new DefaultServerConfigBuilder(new ServerEnvironment(envData, new Properties())).env().build()

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

    def ssc = new SelfSignedCertificate()
    def certificate = ssc.certificate().toPath()
    def privateKey = ssc.privateKey().toPath()

    def envData = [
      RATPACK_SERVER__PORT                  : "8080",
      RATPACK_SERVER__ADDRESS               : "localhost",
      RATPACK_SERVER__DEVELOPMENT           : "true",
      RATPACK_SERVER__THREADS               : "3",
      RATPACK_SERVER__PUBLIC_ADDRESS        : "http://localhost:8080",
      RATPACK_SERVER__MAX_CONTENT_LENGTH    : "50000",
      RATPACK_SERVER__TIME_RESPONSES        : "true",
      RATPACK_SERVER__SSL__CERTIFICATE      : certificate.toString(),
      RATPACK_SERVER__SSL__PRIVATE_KEY      : privateKey.toString()
    ]

    when:
    def serverConfig = new DefaultServerConfigBuilder(new ServerEnvironment(envData, new Properties())).env().baseDir(baseDir).build()

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
