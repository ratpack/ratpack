package org.ratpackframework.app.internal.binding

import spock.lang.Specification

class TokenPathBindingTest extends Specification {

  Map<String, String> map(String pattern, String path) {
    new TokenPathBinding(pattern).map(path)
  }

  def "map"() {
    expect:
    map("/a", "/b") == null
    map("/a", "/a") == [:]
    map("/(.+)", "/abc") == null
    map("/:a/:b", "/abc/def") == [a: "abc", b: "def"]
  }

}
