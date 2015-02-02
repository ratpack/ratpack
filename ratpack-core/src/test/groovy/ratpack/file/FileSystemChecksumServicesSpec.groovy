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

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.AutoCleanup

import ratpack.server.ServerConfig
import ratpack.file.FileSystemChecksumServices
import ratpack.test.embed.BaseDirBuilder

class FileSystemChecksumServicesSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  @AutoCleanup
  @Delegate
  BaseDirBuilder baseDir

  ServerConfig serverConfig

  def setup() {
    baseDir = dir(temporaryFolder.newFolder("asset"))
    serverConfig = ServerConfig.baseDir(this.baseDir.build()).build()
  }

  def "requesting checksum service with baseDir defined"() {
    given:

    when:
    def service = FileSystemChecksumServices.service(serverConfig)

    then:
    service
  }
}
