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

package ratpack.launch

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.handling.Context
import ratpack.handling.Handler
import spock.lang.Specification
import spock.lang.Subject

import java.nio.file.Path

import static ratpack.launch.LaunchConfig.*
import static ratpack.launch.LaunchConfigs.Environment.PORT as E_PORT
import static ratpack.launch.LaunchConfigs.Property.*

@Subject(LaunchConfigs)
class LaunchConfigsSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  Path baseDir
  def classLoader = this.class.classLoader
  def properties = new Properties()
  def env = [:] as Map<String, String>

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
    createWithBaseDir()

    then:
    thrown(LaunchException)
  }

  def "creating a LaunchConfig with no port setting uses default port"() {
    expect:
    createWithBaseDir().port == DEFAULT_PORT
  }

  def "creating a LaunchConfig with only PORT env var specified uses it"() {
    expect:
    createWithBaseDir(properties, e(E_PORT, "1234")).port == 1234
  }

  def "creating a LaunchConfig with only port property specified uses it"() {
    expect:
    createWithBaseDir(p(PORT, "5678")).port == 5678
  }

  def "creating a LaunchConfig with both PORT env var and port property specified uses the property"() {
    expect:
    createWithBaseDir(p(PORT, "5678"), e(E_PORT, "1234")).port == 5678
  }

  def "creating a LaunchConfig with a non-numeric PORT env var throws an exception"() {
    when:
    createWithBaseDir(properties, e(E_PORT, "abc"))

    then:
    def ex = thrown(LaunchException)
    ex.cause instanceof NumberFormatException
  }

  def "creating a LaunchConfig with a non-numeric port property throws an exception"() {
    when:
    createWithBaseDir(p(PORT, "abc"))

    then:
    def ex = thrown(LaunchException)
    ex.cause instanceof NumberFormatException
  }

  def "address is respected"() {
    given:
    def address = "10.11.12.13"

    expect:
    !createWithBaseDir().address
    createWithBaseDir(p(ADDRESS, address)).address.hostAddress == address
  }

  def "publicAddress is respected"() {
    given:
    def url = "http://app.example.com"

    expect:
    !createWithBaseDir().publicAddress
    createWithBaseDir(p(PUBLIC_ADDRESS, url)).publicAddress.toString() == url
  }

  def "reloadable is respected"() {
    expect:
    !createWithBaseDir().reloadable
    createWithBaseDir(p(RELOADABLE, "true")).reloadable
  }

  def "threads is respected"() {
    given:
    def threads = 10

    expect:
    createWithBaseDir().threads == DEFAULT_THREADS
    createWithBaseDir(p(THREADS, threads.toString())).threads == threads
  }

  def "indexFiles is respected"() {
    expect:
    createWithBaseDir().indexFiles.empty
    createWithBaseDir(p(INDEX_FILES, "index.html")).indexFiles == ["index.html"]
    createWithBaseDir(p(INDEX_FILES, "index.html, index.htm, index.txt")).indexFiles == ["index.html", "index.htm", "index.txt"]
  }

  def "maxContentLength is respected"() {
    expect:
    createWithBaseDir().maxContentLength == DEFAULT_MAX_CONTENT_LENGTH
    createWithBaseDir(p(MAX_CONTENT_LENGTH, "20")).maxContentLength == 20
  }

  def "timeResponses is respected"() {
    expect:
    !createWithBaseDir().timeResponses
    createWithBaseDir(p(TIME_RESPONSES, "true")).timeResponses
  }

  def "compressResponses is respected"() {
    expect:
    !createWithBaseDir().compressResponses
    createWithBaseDir(p(COMPRESS_RESPONSES, "true")).compressResponses
  }

  def "compressionMinSize is respected"() {
    given:
    def minSize = 12345L

    expect:
    createWithBaseDir().compressionMinSize == DEFAULT_COMPRESSION_MIN_SIZE
    createWithBaseDir(p(COMPRESSION_MIN_SIZE, minSize.toString())).compressionMinSize == minSize
  }

  def "compressionMimeTypeWhiteList is respected"() {
    expect:
    createWithBaseDir().compressionMimeTypeWhiteList.empty
    createWithBaseDir(p(COMPRESSION_MIME_TYPE_WHITE_LIST, "text/plain")).compressionMimeTypeWhiteList == ImmutableSet.of("text/plain")
    createWithBaseDir(p(COMPRESSION_MIME_TYPE_WHITE_LIST, "text/plain, text/html, application/json")).compressionMimeTypeWhiteList == ImmutableSet.of("text/plain", "text/html", "application/json")
  }

  def "compressionMimeTypeBlackList is respected"() {
    expect:
    createWithBaseDir().compressionMimeTypeBlackList.empty
    createWithBaseDir(p(COMPRESSION_MIME_TYPE_BLACK_LIST, "application/gzip")).compressionMimeTypeBlackList == ImmutableSet.of("application/gzip")
    createWithBaseDir(p(COMPRESSION_MIME_TYPE_BLACK_LIST, "application/compress, application/zip, application/gzip")).compressionMimeTypeBlackList == ImmutableSet.of("application/compress", "application/zip", "application/gzip")
  }

  def "ssl properties are respected"() {
    expect:
    !createWithBaseDir().SSLContext
    createWithBaseDir(p((SSL_KEYSTORE_FILE): "ratpack/launch/keystore.jks", (SSL_KEYSTORE_PASSWORD): "password")).SSLContext
  }

  def "other properties are supported"() {
    expect:
    createWithBaseDir().getOtherPrefixedWith("") == [:]

    when:
    def config = createWithBaseDir(p("other.db.username": "myuser", "other.db.password": "mypass", "other.servicea.url": "http://servicea.example.com"))

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
    createWithBaseDir().baseDir.file == baseDir
  }

  Properties p(String key, String value) {
    def p = new Properties(properties)
    p.setProperty(key, value)
    return p
  }

  Properties p(Map<Object, Object> m) {
    def p = new Properties(properties)
    p.putAll(m)
    return p
  }

  Map<String, String> e(String key, String value) {
    return ImmutableMap.builder().putAll(env).put(key, value).build()
  }

  LaunchConfig createWithBaseDir(Properties p = properties, Map<String, String> e = env) {
    return LaunchConfigs.createWithBaseDir(classLoader, baseDir, p, e)
  }

}
