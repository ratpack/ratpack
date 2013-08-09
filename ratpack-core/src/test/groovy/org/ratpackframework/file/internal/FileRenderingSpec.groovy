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

package org.ratpackframework.file.internal

import org.ratpackframework.test.groovy.RatpackGroovyDslSpec

class FileRenderingSpec extends RatpackGroovyDslSpec {

  private static final String FILE_CONTENTS = "hello!"

  def "can render file"() {
    given:
    def myFile = file("myFile.text") << FILE_CONTENTS

    and:
    app {
      handlers {
        get("path") {render myFile}
      }
    }

    when:
    get("path")

    then:
    with (response) {
      statusCode == 200
      response.body.asString().contains(FILE_CONTENTS)
      response.contentType.equals("text/plain;charset=UTF-8")
      response.header("Content-Length").equals(FILE_CONTENTS.length().toString())
    }
  }

}
