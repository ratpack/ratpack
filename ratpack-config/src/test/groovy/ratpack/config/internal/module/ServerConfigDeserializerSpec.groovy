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

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.config.internal.source.env.MapEnvironment
import ratpack.server.ServerConfig
import ratpack.test.embed.BaseDirBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Subject

class ServerConfigDeserializerSpec extends Specification {
  @AutoCleanup
  def b1 = BaseDirBuilder.tmpDir()
  def originalSysProps
  def originalClassLoader
  def classLoader = new GroovyClassLoader()
  @Subject
  def deserializer = new ServerConfigDeserializer(new MapEnvironment([:]))
  def objectMapper = new ObjectMapper()

  def setup() {
    originalSysProps = System.properties
    System.properties = new Properties(originalSysProps)
    originalClassLoader = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = classLoader
  }

  def cleanup() {
    Thread.currentThread().contextClassLoader = originalClassLoader
    System.properties = originalSysProps
  }

  def "can specify baseDir"() {
    def dir = b1.dir("p1")

    when:
    def serverConfig = deserialize(objectMapper.createObjectNode().put("baseDir", dir.toString()))

    then:
    serverConfig.hasBaseDir
    serverConfig.baseDir.file == dir
  }

  def "non-text baseDir results in error"() {
    when:
    deserialize(objectMapper.createObjectNode().set("baseDir", objectMapper.createArrayNode()))

    then:
    thrown(JsonMappingException)
  }

  def "can specify baseDirProps"() {
    def propsFileName = "foo.properties"
    def dir = b1.build { it.file(propsFileName, "") }
    classLoader.addURL(dir.toUri().toURL())

    when:
    def serverConfig = deserialize(objectMapper.createObjectNode().put("baseDirProps", propsFileName))

    then:
    serverConfig.hasBaseDir
    serverConfig.baseDir.file == dir
  }

  def "empty baseDirProps uses default"() {
    def dir = b1.build { it.file(ServerConfig.Builder.DEFAULT_PROPERTIES_FILE_NAME, "") }
    classLoader.addURL(dir.toUri().toURL())

    when:
    def serverConfig = deserialize(objectMapper.createObjectNode().put("baseDirProps", ""))

    then:
    serverConfig.hasBaseDir
    serverConfig.baseDir.file == dir
  }

  def "non-text baseDirProps results in error"() {
    when:
    deserialize(objectMapper.createObjectNode().set("baseDirProps", objectMapper.createArrayNode()))

    then:
    thrown(JsonMappingException)
  }

  def "neither baseDir nor baseDirProps results in no base dir"() {
    when:
    def serverConfig = deserialize(objectMapper.createObjectNode())

    then:
    !serverConfig.hasBaseDir
  }

  def "supports ratpack.development system property as an override to development field"() {
    when:
    System.clearProperty("ratpack.development")

    then:
    !deserialize(objectMapper.createObjectNode()).development
    !deserialize(objectMapper.createObjectNode().put("development", false)).development
    deserialize(objectMapper.createObjectNode().put("development", true)).development

    when:
    System.setProperty("ratpack.development", "true")

    then:
    deserialize(objectMapper.createObjectNode()).development
    deserialize(objectMapper.createObjectNode().put("development", false)).development
    deserialize(objectMapper.createObjectNode().put("development", true)).development

    when:
    System.setProperty("ratpack.development", "false")

    then:
    !deserialize(objectMapper.createObjectNode()).development
    !deserialize(objectMapper.createObjectNode().put("development", false)).development
    !deserialize(objectMapper.createObjectNode().put("development", true)).development
  }

  private ServerConfig deserialize(JsonNode node) {
    deserializer.deserialize(node.traverse(objectMapper), objectMapper.deserializationContext)
  }
}
