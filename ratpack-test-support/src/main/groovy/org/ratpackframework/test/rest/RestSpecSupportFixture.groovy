package org.ratpackframework.test.rest

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ratpackframework.groovy.RatpackScript

class RestSpecSupportFixture extends RestSpecSupport {

  @Rule
  TemporaryFolder tmp

  def setup() {
    ratpackScript = tmp.newFile("ratpack.groovy")
  }

  void script(String content) {
    ratpackScript.text = "import static ${RatpackScript.name}.ratpack\n$content"
  }

}
