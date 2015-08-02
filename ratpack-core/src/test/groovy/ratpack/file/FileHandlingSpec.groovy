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

import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.file.FileSystems
import java.nio.file.Files

class FileHandlingSpec extends RatpackGroovyDslSpec {

  void "context resolves files relative to application root"() {
    given:
    def fileInsideBaseDir = path("foo")

    when:
    handlers {
      get { FileSystemBinding fsBinding ->

        assert file("/foo") == fileInsideBaseDir

        assert fsBinding.binding("/foo").file == fileInsideBaseDir
        assert fsBinding.file("/foo") == fileInsideBaseDir

        render "ok"
      }
    }

    then:
    text == "ok"
  }

  void "context returns null value for files that cannot be found in the application root"() {
    given:
    def path = "../../some-file.txt" // outside the base dir

    write path, "foo"

    when:
    handlers {
      get { FileSystemBinding fsBinding ->

        if (fsBinding.file.fileSystem == FileSystems.default) {
          assert Files.exists(fsBinding.file)
        }

        assert fsBinding.binding(path) == null
        assert fsBinding.file(path) == null

        assert file(path) == null

        render "ok"
      }
    }

    then:
    text == "ok"
  }

  void "unresolved files result in statusCode 404"() {
    when:
    handlers {
      get {
        render file("../../etc/passwd")
      }
    }

    then:
    get().statusCode == 404
  }
}
