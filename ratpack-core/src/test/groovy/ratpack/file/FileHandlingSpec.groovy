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
import spock.lang.Ignore

class FileHandlingSpec extends RatpackGroovyDslSpec {

  void "context resolves files relative to application root"() {
    given:
    app {
      handlers {
        get {
          def f = file("/etc/passwd")
          render f.canonicalPath
        }
      }
    }

    when:
    def resp = getText()

    then:
    resp == "${server.launchConfig.baseDir.canonicalPath}/etc/passwd"
  }

  void "context returns null value for files that cannot be found in the application root"() {
    given:
    app {
      handlers {
        get {
          def f = file('../')
          render f ?: "null-value"
        }
      }
    }

    expect:
    getText() == "null-value"
  }

  @Ignore
  void "unresolved files result in statusCode 404"() {
    given:
    app {
      handlers {
        get {
          render file("../../etc/passwd")
        }
      }
    }

    when:
    getText()

    then:
    response.statusCode == 404
  }
}
