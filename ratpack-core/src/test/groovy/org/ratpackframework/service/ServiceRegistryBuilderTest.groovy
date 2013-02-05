package org.ratpackframework.service

import spock.lang.Specification

class ServiceRegistryBuilderTest extends Specification {

  def "can build"() {
    given:
    def r = new ServiceRegistryBuilder().add("foo").build()

    expect:
    r.get(CharSequence) == "foo"
    r.get("string") == "foo"
  }

}
