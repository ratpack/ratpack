/*
 * Copyright 2012 the original author or authors.
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

package ratpack.file

import ratpack.file.internal.CachingFileSystemChecksumService
import ratpack.file.internal.FileSystemChecksumServices

import java.nio.file.NoSuchFileException
import java.nio.file.InvalidPathException

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.AutoCleanup

import ratpack.server.ServerConfig
import ratpack.file.internal.DefaultFileSystemChecksumService

import ratpack.test.embed.EphemeralBaseDir

class FileSystemChecksumServicesSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  @AutoCleanup
  @Delegate
  EphemeralBaseDir baseDir

  ServerConfig serverConfig

  def setup() {
    baseDir = EphemeralBaseDir.dir(temporaryFolder.newFolder("asset"))
  }

  def "requesting checksum service in development mode"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()

    when:
    def service = FileSystemChecksumServices.service(serverConfig)

    then:
    service != null
    service instanceof DefaultFileSystemChecksumService
  }

  def "requesting checksum service in production mode"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(false).build()

    when:
    def service = FileSystemChecksumServices.service(serverConfig)

    then:
    service != null
    service instanceof CachingFileSystemChecksumService
  }

  def "calculate checksum for the file"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.write("test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig)
    String checksum = service.checksum("test.js")

    then:
    checksum
    checksum == "1f6f04b0"
  }

  def "exception when non existing file"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()

    when:
    def service = FileSystemChecksumServices.service(serverConfig)
    service.checksum("test.js")

    then:
    thrown NoSuchFileException
  }

  def "noop checksummer returns empty string"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.write("test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig, null)
    String checksum = service.checksum("test.js")

    then:
    checksum == ""
  }

  def "custom checksummer function"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.write("test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"})
    String checksum = service.checksum("test.js")

    then:
    checksum == "A123B"
  }

  def "calculate checksum with Adler32 method"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.write("test.js", "function(){}")
    baseDir.mkdir("js")
    baseDir.write("js/test2.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.adler32(serverConfig, null)
    String checksum = service.checksum("test.js")

    then:
    checksum == "1f6f04b0"

    when:
    service = FileSystemChecksumServices.adler32(serverConfig, "js")
    checksum = service.checksum("test2.js")

    then:
    checksum == "1f6f04b0"

    when:
    service = FileSystemChecksumServices.adler32(serverConfig, "js", "js", "css", "png")
    checksum = service.checksum("test2.js")

    then:
    checksum == "1f6f04b0"
  }

  def "calculate checksum with MD5 method"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.write("test.js", "function(){}")
    baseDir.mkdir("js")
    baseDir.write("js/test2.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.md5(serverConfig, null)
    String checksum = service.checksum("test.js")

    System.out.println("==> CHECKSUM: $checksum")

    then:
    checksum == "7c1368cdbc23988b15934bf7655c765b"

    when:
    service = FileSystemChecksumServices.md5(serverConfig, "js")
    checksum = service.checksum("test2.js")

    then:
    checksum == "7c1368cdbc23988b15934bf7655c765b"

    when:
    service = FileSystemChecksumServices.md5(serverConfig, "js", "js", "css", "png")
    checksum = service.checksum("test2.js")

    then:
    checksum == "7c1368cdbc23988b15934bf7655c765b"
  }

  def "calculate checksum for file in path relative to server's base dir"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.mkdir("js")
    baseDir.write("js/test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"}, "js")
    String checksum = service.checksum("test.js")

    then:
    checksum
    checksum == "A123B"
  }

  def "return null checksum if file path is not provided"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"})
    def checksum = service.checksum()

    then:
    checksum == null
  }

  def "throw exception when additional path cannot be resolved"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()

    when:
    FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"}, "css\0")

    then:
    thrown InvalidPathException
  }

  def "throw exception when additional path is not a folder"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"}, "test.js")
    service.checksum("test.js")

    then:
    thrown IllegalArgumentException
  }

  def "throw exception when file does not exist in additional path"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.mkdir("css")
    baseDir.write("test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"}, "css")
    service.checksum("test.js")

    then:
    thrown NoSuchFileException
  }

  def "throw exception when file does not exist in cache for additional path"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(false).build()
    baseDir.mkdir("css")
    baseDir.write("test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"}, "css")
    service.checksum("test.js")

    then:
    thrown NoSuchFileException
  }

  def "calculate checksum for file with given extension"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.mkdir("css")
    baseDir.write("test.js", "function(){}")
    baseDir.write("css/test.css", ".blue{}")
    def service = FileSystemChecksumServices.service(serverConfig, {is -> "A123B" }, null, "js", "css", "png")

    when:
    String checksum = service.checksum("test.js")

    then:
    checksum
    checksum == "A123B"

    when:
    checksum = service.checksum("css/test.css")

    then:
    checksum
    checksum == "A123B"
  }

  def "calculate checksum for file with given extension in production mode"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(false).build()
    baseDir.mkdir("css")
    baseDir.write("test.js", "function(){}")
    baseDir.write("css/test.css", ".blue{}")
    def service = FileSystemChecksumServices.service(serverConfig, {is -> "A123B" }, null, "js", "css", "png")

    when:
    String checksum = service.checksum("test.js")

    then:
    checksum
    checksum == "A123B"

    when:
    checksum = service.checksum("css/test.css")

    then:
    checksum
    checksum == "A123B"
  }

  def "throw exception when file no match list of file extenstions"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(true).build()
    baseDir.write("test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"}, null, "css", "html")
    service.checksum("test.js")

    then:
    thrown NoSuchFileException
  }

  def "throw exception when file no match list of file extenstions in production mode"() {
    given:
    ServerConfig serverConfig = ServerConfig.builder().baseDir(this.baseDir.root).development(false).build()
    baseDir.write("test.js", "function(){}")

    when:
    def service = FileSystemChecksumServices.service(serverConfig, {is -> return "A123B"}, null, "css", "html")
    service.checksum("test.js")

    then:
    thrown NoSuchFileException
  }
}
