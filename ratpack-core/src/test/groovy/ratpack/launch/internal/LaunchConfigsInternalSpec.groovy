/*
 * Copyright 2013 the original author or authors.
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

package ratpack.launch.internal

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.launch.HandlerFactory
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchException
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static ratpack.launch.LaunchConfig.*
import static ratpack.launch.LaunchConfigs.CONFIG_RESOURCE_PROPERTY
import static ratpack.launch.LaunchConfigs.Environment.PORT as E_PORT
import static ratpack.launch.LaunchConfigs.Property.*
import static ratpack.launch.LaunchConfigs.SYSPROP_PREFIX_PROPERTY

@Subject(LaunchConfigsInternal)
class LaunchConfigsInternalSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  Path baseDir
  def classLoader = this.class.classLoader
  def properties = new Properties()
  def env = [:] as Map<String, String>

  List<LaunchConfig> launchConfigs = []

  static class TestHandlerFactory implements HandlerFactory {
    @Override
    Handler create(LaunchConfig launchConfig) {
      new Handler() {
        void handle(Context context) throws Exception {

        }
      }
    }
  }

  def setup() {
    baseDir = temporaryFolder.newFolder().toPath()
    properties.setProperty(HANDLER_FACTORY, TestHandlerFactory.name)
  }

  def "creating a LaunchConfig without a handler factory throws an exception"() {
    given:
    properties.remove(HANDLER_FACTORY)

    when:
    createLaunchConfig()

    then:
    thrown(LaunchException)
  }

  def "creating a LaunchConfig with no port setting uses default port"() {
    expect:
    createLaunchConfig().port == DEFAULT_PORT
  }

  def "creating a LaunchConfig with only PORT env var specified uses it"() {
    expect:
    createLaunchConfig(properties, e(E_PORT, "1234")).port == 1234
  }

  def "creating a LaunchConfig with only port property specified uses it"() {
    expect:
    createLaunchConfig(p(PORT, "5678")).port == 5678
  }

  def "creating a LaunchConfig with both PORT env var and port property specified uses the property"() {
    expect:
    createLaunchConfig(p(PORT, "5678"), e(E_PORT, "1234")).port == 5678
  }

  def "creating a LaunchConfig with a non-numeric PORT env var throws an exception"() {
    when:
    createLaunchConfig(properties, e(E_PORT, "abc"))

    then:
    def ex = thrown(LaunchException)
    ex.cause instanceof NumberFormatException
  }

  def "creating a LaunchConfig with a non-numeric port property throws an exception"() {
    when:
    createLaunchConfig(p(PORT, "abc"))

    then:
    def ex = thrown(LaunchException)
    ex.cause instanceof NumberFormatException
  }

  def "address is respected"() {
    given:
    def address = "10.11.12.13"

    expect:
    !createLaunchConfig().address
    createLaunchConfig(p(ADDRESS, address)).address.hostAddress == address
  }

  def "publicAddress is respected"() {
    given:
    def url = "http://app.example.com"

    expect:
    !createLaunchConfig().publicAddress
    createLaunchConfig(p(PUBLIC_ADDRESS, url)).publicAddress.toString() == url
  }

  def "reloadable is respected"() {
    expect:
    !createLaunchConfig().reloadable
    createLaunchConfig(p(RELOADABLE, "true")).reloadable
  }

  def "threads is respected"() {
    given:
    def threads = 10

    expect:
    createLaunchConfig().threads == DEFAULT_THREADS
    createLaunchConfig(p(THREADS, threads.toString())).threads == threads
  }

  def "indexFiles is respected"() {
    expect:
    createLaunchConfig().indexFiles.empty
    createLaunchConfig(p(INDEX_FILES, "index.html")).indexFiles == ["index.html"]
    createLaunchConfig(p(INDEX_FILES, "index.html, index.htm, index.txt")).indexFiles == ["index.html", "index.htm", "index.txt"]
  }

  def "maxContentLength is respected"() {
    expect:
    createLaunchConfig().maxContentLength == DEFAULT_MAX_CONTENT_LENGTH
    createLaunchConfig(p(MAX_CONTENT_LENGTH, "20")).maxContentLength == 20
  }

  def "timeResponses is respected"() {
    expect:
    !createLaunchConfig().timeResponses
    createLaunchConfig(p(TIME_RESPONSES, "true")).timeResponses
  }

  def "compressResponses is respected"() {
    expect:
    !createLaunchConfig().compressResponses
    createLaunchConfig(p(COMPRESS_RESPONSES, "true")).compressResponses
  }

  def "compressionMinSize is respected"() {
    given:
    def minSize = 12345L

    expect:
    createLaunchConfig().compressionMinSize == DEFAULT_COMPRESSION_MIN_SIZE
    createLaunchConfig(p(COMPRESSION_MIN_SIZE, minSize.toString())).compressionMinSize == minSize
  }

  def "compressionMimeTypeWhiteList is respected"() {
    expect:
    createLaunchConfig().compressionMimeTypeWhiteList.empty
    createLaunchConfig(p(COMPRESSION_MIME_TYPE_WHITE_LIST, "text/plain")).compressionMimeTypeWhiteList == ImmutableSet.of("text/plain")
    createLaunchConfig(p(COMPRESSION_MIME_TYPE_WHITE_LIST, "text/plain, text/html, application/json")).compressionMimeTypeWhiteList == ImmutableSet.of("text/plain", "text/html", "application/json")
  }

  def "compressionMimeTypeBlackList is respected"() {
    expect:
    createLaunchConfig().compressionMimeTypeBlackList.empty
    createLaunchConfig(p(COMPRESSION_MIME_TYPE_BLACK_LIST, "application/gzip")).compressionMimeTypeBlackList == ImmutableSet.of("application/gzip")
    createLaunchConfig(p(COMPRESSION_MIME_TYPE_BLACK_LIST, "application/compress, application/zip, application/gzip")).compressionMimeTypeBlackList == ImmutableSet.of("application/compress", "application/zip", "application/gzip")
  }

  def "ssl properties are respected"() {
    expect:
    !createLaunchConfig().SSLContext
    createLaunchConfig(p((SSL_KEYSTORE_FILE): "ratpack/launch/internal/keystore.jks", (SSL_KEYSTORE_PASSWORD): "password")).SSLContext
  }

  def "other properties are supported"() {
    expect:
    createLaunchConfig().getOtherPrefixedWith("") == [:]

    when:
    def config = createLaunchConfig(p("other.db.username": "myuser", "other.db.password": "mypass", "other.servicea.url": "http://servicea.example.com"))

    then:
    config.getOtherPrefixedWith("") == ["db.username": "myuser", "db.password": "mypass", "servicea.url": "http://servicea.example.com"]
    config.getOtherPrefixedWith("db.") == [username: "myuser", password: "mypass"]
    config.getOtherPrefixedWith("servicea.") == [url: "http://servicea.example.com"]
    config.getOther("db.username", null) == "myuser"
    config.getOther("db.password", null) == "mypass"
    config.getOther("servicea.url", null) == "http://servicea.example.com"
  }

  def "baseDir is passed on to LaunchConfig"() {
    expect:
    createLaunchConfig().baseDir.file == baseDir
  }

  def "createWithBaseDir with envVars specified creates data based on arguments"() {
    when:
    def data = LaunchConfigsInternal.createWithBaseDir(classLoader, baseDir, properties, env)

    then:
    data.classLoader == classLoader
    data.baseDir == baseDir
    data.properties == properties
    data.envVars == env
  }

  def "createWithBaseDir without envVars specified creates data based on arguments plus system environment"() {
    when:
    def data = LaunchConfigsInternal.createWithBaseDir(classLoader, baseDir, properties)

    then:
    data.classLoader == classLoader
    data.baseDir == baseDir
    data.properties == properties
    data.envVars == System.getenv()
  }

  def "createFromFile creates data based on arguments"() {
    given:
    def defaultProps = new Properties([(PORT): "1234", (ADDRESS): "10.11.12.13", (THREADS): "3"])
    def fileProps = new Properties([(PORT): "3456", (ADDRESS): "10.11.12.14", (INDEX_FILES): "index.html"])
    def overrideProps = new Properties([(PORT): "5678", (THREADS): "5", (PUBLIC_ADDRESS): "http://app.example.com"])

    def nonExistingPath = temporaryFolder.newFile().toPath()
    Files.delete(nonExistingPath)

    def existingPath = temporaryFolder.newFile().toPath()
    existingPath.withWriter { fileProps.store(it, null) }

    def expectedNoFileData = new LaunchConfigData(classLoader, baseDir, newProps(
      [(PORT): "5678", (THREADS): "5", (PUBLIC_ADDRESS): "http://app.example.com", (ADDRESS): "10.11.12.13"]
    ), System.getenv())
    def expectedFileData = new LaunchConfigData(classLoader, baseDir, newProps(
      [(PORT): "5678", (THREADS): "5", (PUBLIC_ADDRESS): "http://app.example.com", (ADDRESS): "10.11.12.14", (INDEX_FILES): "index.html"]
    ), System.getenv())

    expect:
    LaunchConfigsInternal.createFromFile(classLoader, baseDir, null, overrideProps, defaultProps) == expectedNoFileData
    LaunchConfigsInternal.createFromFile(classLoader, baseDir, nonExistingPath, overrideProps, defaultProps) == expectedNoFileData
    LaunchConfigsInternal.createFromFile(classLoader, baseDir, existingPath, overrideProps, defaultProps) == expectedFileData
  }

  def "createFromProperties supports loading config from a resource"() {
    when:
    def data = LaunchConfigsInternal.createFromProperties(currentDir.toFile().canonicalPath, classLoader, newProps([(CONFIG_RESOURCE_PROPERTY): "ratpack/launch/internal/config.properties"]), newProps([:]))

    then:
    data.classLoader == classLoader
    data.baseDir == Paths.get(getClass().getResource("config.properties").toURI()).parent
    data.properties.getProperty(PORT) == "3456"
    data.envVars == System.getenv()
  }

  def "createFromProperties supports loading config from an absolute file path"() {
    given:
    def configFile = temporaryFolder.newFile().toPath()
    configFile.withOutputStream { properties.store(it, null) }

    when:
    def data = LaunchConfigsInternal.createFromProperties(currentDir.toFile().canonicalPath, classLoader, newProps([(CONFIG_RESOURCE_PROPERTY): configFile.toAbsolutePath().toString()]), newProps([:]))

    then:
    data.classLoader == classLoader
    data.baseDir == configFile.parent
    data.properties.getProperty(HANDLER_FACTORY)
    data.envVars == System.getenv()
  }

  def "createFromProperties supports loading config from a relative path"() {
    given:
    def configFile = temporaryFolder.newFile().toPath()
    configFile.withOutputStream { properties.store(it, null) }

    when:
    def data = LaunchConfigsInternal.createFromProperties(currentDir.toFile().canonicalPath, classLoader, newProps([(CONFIG_RESOURCE_PROPERTY): currentDir.relativize(configFile).toString()]), newProps([:]))

    then:
    data.classLoader == classLoader
    data.baseDir.normalize().toFile().canonicalPath == configFile.parent.toFile().canonicalPath
    data.properties.getProperty(HANDLER_FACTORY)
    data.envVars == System.getenv()
  }

  def "createFromGlobalProperties without a specified prefix uses a prefix derived from the global properties to load properties"() {
    when:
    def currentDir = currentDir
    def workingDir = currentDir.toFile().canonicalPath
    def data = LaunchConfigsInternal.createFromGlobalProperties(workingDir, classLoader, newProps(["ratpack.port": "3456"]), newProps([:]))

    then:
    data.classLoader == classLoader
    data.baseDir.toFile().canonicalPath == currentDir.toFile().canonicalPath
    data.properties.getProperty(PORT) == "3456"
    data.envVars == System.getenv()

    when:
    data = LaunchConfigsInternal.createFromGlobalProperties(workingDir, classLoader, newProps([(SYSPROP_PREFIX_PROPERTY): "myapp.", "myapp.port": "3456"]), newProps([:]))

    then:
    data.classLoader == classLoader
    data.baseDir.toFile().canonicalPath == currentDir.toFile().canonicalPath
    data.properties.getProperty(PORT) == "3456"
    data.envVars == System.getenv()
  }

  def "createFromGlobalProperties with a specified prefix uses the prefix to load properties"() {
    when:
    def currentDir = currentDir
    def workingDir = currentDir.toFile().canonicalPath
    def data = LaunchConfigsInternal.createFromGlobalProperties(workingDir, classLoader, "myapp.", newProps(["myapp.port": "3456"]), newProps([:]))

    then:
    data.classLoader == classLoader
    data.baseDir.toFile().canonicalPath == currentDir.toFile().canonicalPath
    data.properties.getProperty(PORT) == "3456"
    data.envVars == System.getenv()
  }

  private Properties p(String key, String value) {
    def p = new Properties(properties)
    p.setProperty(key, value)
    return p
  }

  private Properties p(Map<Object, Object> m) {
    def p = new Properties(properties)
    p.putAll(m)
    return p
  }

  private Map<String, String> e(String key, String value) {
    return ImmutableMap.builder().putAll(env).put(key, value).build()
  }

  private LaunchConfig createLaunchConfig(Properties p = properties, Map<String, String> e = env) {
    def launchConfig = LaunchConfigsInternal.createLaunchConfig(new LaunchConfigData(classLoader, baseDir, p, e))
    launchConfigs << launchConfig
    launchConfig
  }

  private static Properties newProps(Map<String, String> values) {
    def properties = new Properties()
    properties.putAll(values)
    return properties
  }

  private Path getCurrentDir() {
    return Paths.get(temporaryFolder.newFolder().toURI())
  }

  def cleanup() {
    launchConfigs.each { it.execController.close() }
  }

}
