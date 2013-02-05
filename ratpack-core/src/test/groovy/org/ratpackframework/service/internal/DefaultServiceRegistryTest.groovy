package org.ratpackframework.service.internal

import spock.lang.Specification

class DefaultServiceRegistryTest extends Specification {

  Map<String, Object> s = [:]

  DefaultServiceRegistry r() {
    new DefaultServiceRegistry(s)
  }

  def "simple retrieve"() {
    final value = "value"

    given:
    s.name = value

    expect:
    r().get(String).is(value)
    r().get(CharSequence).is(value)
    r().get("name").is(value)
    r().get(Integer) == null
  }

  def "error on multiple for type"() {
    given:
    s.s1 = "a"
    s.s2 = "b"

    when:
    r().get(String)

    then:
    thrown(IllegalArgumentException)
  }

  def "duplicate for type by name"() {
    given:
    s.s1 = "a"
    s.s2 = "b"

    expect:
    r().get("s1", String) == "a"
    r().get("s2", String) == "b"
  }

}
