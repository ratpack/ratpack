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

package ratpack.config.internal.module

import com.fasterxml.jackson.databind.JsonNode
import ratpack.config.internal.DefaultConfigDataBuilder
import ratpack.file.FileSystemBinding
import ratpack.server.internal.ServerConfigData
import ratpack.server.internal.ServerEnvironment
import ratpack.test.embed.EphemeralBaseDir
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.function.Supplier

class ServerConfigDataDeserializerSpec extends Specification {
  @AutoCleanup
  def b1 = EphemeralBaseDir.tmpDir()
  def originalClassLoader
  def classLoader = new GroovyClassLoader()
  def serverEnvironment = new ServerEnvironment([:], new Properties())
  def deserializer = new ServerConfigDataDeserializer(serverEnvironment.address, serverEnvironment.port, serverEnvironment.development, serverEnvironment.publicAddress, { -> FileSystemBinding.of(b1.root) } as Supplier)
  def objectMapper = DefaultConfigDataBuilder.newDefaultObjectMapper()

  def setup() {
    originalClassLoader = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = classLoader
  }

  def cleanup() {
    Thread.currentThread().contextClassLoader = originalClassLoader
  }

  def "can specify baseDir"() {
    def dir = b1.mkdir("p1")

    when:
    deserialize(objectMapper.createObjectNode().put("baseDir", dir.toString()))

    then:
    thrown IllegalStateException
  }

  def "without baseDir results in no base dir"() {
    when:
    def serverConfig = deserialize(objectMapper.createObjectNode())

    then:
    serverConfig.baseDir.file == b1.root
  }

  def "without any config uses default from server config builder"() {
    when:
    def serverConfig = deserialize(objectMapper.createObjectNode())

    then:
    !serverConfig.address
    serverConfig.port == 5050
    !serverConfig.development
    !serverConfig.publicAddress
  }

  private ServerConfigData deserialize(JsonNode node) {
    deserializer.deserialize(node.traverse(objectMapper), objectMapper.deserializationContext)
  }
}
