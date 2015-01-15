package ratpack.config.internal.module

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.config.internal.source.env.MapEnvironment
import ratpack.server.ServerConfig
import ratpack.test.embed.BaseDirBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

class ServerConfigDeserializerSpec extends Specification {
  @AutoCleanup
  def b1 = BaseDirBuilder.tmpDir()
  def originalClassLoader
  def classLoader = new GroovyClassLoader()
  def deserializer = new ServerConfigDeserializer(new MapEnvironment([:]))
  def objectMapper = new ObjectMapper()

  def setup() {
    originalClassLoader = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = classLoader
  }

  def cleanup() {
    Thread.currentThread().contextClassLoader = originalClassLoader
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

  private ServerConfig deserialize(JsonNode node) {
    deserializer.deserialize(node.traverse(objectMapper), objectMapper.deserializationContext)
  }
}
