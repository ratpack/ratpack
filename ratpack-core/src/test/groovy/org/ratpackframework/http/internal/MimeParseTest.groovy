package org.ratpackframework.http.internal

import spock.lang.Specification

class MimeParseTest extends Specification {

  def "matching"() {
    expect:
    match("a/a", "a/b") == ""
    match("a/a", "a/b", "a/a") == "a/a"
    match("a/a;q=0.5,a/b;q=1", "a/b", "a/a") == "a/b"
    match("a/a;q=1;a/b;q=0.5,*", "a/c") == "a/c"
    match("*", "a/c", "a/b") == "a/c"
  }

  String match(String header, String... supported) {
    MimeParse.bestMatch(supported.toList(), header)
  }
}
