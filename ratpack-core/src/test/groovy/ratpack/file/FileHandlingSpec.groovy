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

class FileHandlingSpec extends RatpackGroovyDslSpec {

  void "context resolves files relative to application root"() {
    given:
    def basePath = file('.').canonicalPath
    app {
      handlers {
        get {
          def f = file("/etc/passwd")
          render f.canonicalPath
        }
      }
    }

    expect:
    getText() == "$basePath/etc/passwd"
  }

  void "context returns null value for files that cannot be found"() {
    given:
    app {
      handlers {
        get {
          def f = file('../ratpack.groovy')
          if (!f)
            render "null value"
          else
            render "non-null value"
        }
      }
    }

    expect:
    getText() == "null value"
  }

  void "unresolved files result in statusCode 404"() {
    given:
    app {
      handlers {
        get(":path") {
          render file(pathTokens.path)
        }
      }
    }

    when:
    getText("../../etc/passwd")

    then:
    response.statusCode == 404
  }

}
