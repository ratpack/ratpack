package org.ratpackframework.app.internal.binding

import spock.lang.Specification

class PatternPathBindingTest extends Specification {

  Map<String, String> map(String pattern, String path) {
    new PatternPathBinding(pattern).map(path)
  }

  def "map"() {
    expect:
    map("/a", "/b") == null
    map("/a", "/a") == [:]
    map("/(.+)", "/abc") == ["0": "abc"]
    map("/(.+)/(d.+)", "/abc/def") == ["0": "abc", "1": "def"]
  }

}
