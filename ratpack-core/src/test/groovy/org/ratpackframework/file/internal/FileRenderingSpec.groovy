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
