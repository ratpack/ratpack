/*
 * Copyright 2013 the original author or authors.
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

package ratpack.util.internal

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

@Subject(TypeCoercingProperties)
class TypeCoercingPropertiesSpec extends Specification {

  def delegateProperties = new Properties()

  @Subject
  def properties = new TypeCoercingProperties(delegateProperties)

  def "can get a property as a boolean"() {
    given:
    if (propertyValue) {
      delegateProperties.setProperty("key", propertyValue)
    }

    expect:
    properties.asBoolean("key", defaultValue) == expected

    where:
    propertyValue | defaultValue | expected
    "true"        | false        | true
    "false"       | true         | false
    null          | false        | false
    null          | true         | true
  }

  def "can get a property as an int"() {
    given:
    if (propertyValue) {
      delegateProperties.setProperty("key", propertyValue)
    }

    expect:
    properties.asInt("key", defaultValue) == expected

    where:
    propertyValue | defaultValue | expected
    "1"           | -1           | 1
    "0"           | -1           | 0
    null          | -1           | -1
    "010"         | -1           | 10
  }

  def "can get a property as an long"() {
    given:
    if (propertyValue) {
      delegateProperties.setProperty("key", propertyValue)
    }

    expect:
    properties.asLong("key", defaultValue) == expected

    where:
    propertyValue | defaultValue | expected
    "1"           | -1           | 1
    "0"           | -1           | 0
    null          | -1           | -1
    "010"         | -1           | 10
  }

  def "can get a property as a URI"() {
    given:
    delegateProperties.setProperty("key", uriString)

    expect:
    properties.asURI("key") == uriString.toURI()

    where:
    uriString = "http://ratpackframework.org/"
  }

  def "asURI returns null when there is no property"() {
    expect:
    properties.asURI("key") == null
  }

  def "asURI throws an exception if the property value is not a valid URI"() {
    given:
    delegateProperties.setProperty("key", "RATPACK IS AWESOME")

    when:
    properties.asURI("key")

    then:
    thrown URISyntaxException
  }

  def "can get a property as an InetAddress"() {
    given:
    delegateProperties.setProperty("key", "127.0.0.1")

    expect:
    properties.asInetAddress("key") == InetAddress.getByName("127.0.0.1")
  }

  def "asInetAddress returns null when there is no property"() {
    expect:
    properties.asInetAddress("key") == null
  }

  def "asInetAddress throws an exception if the property value cannot be resolved as a host"() {
    given:
    delegateProperties.setProperty("key", "dev.null.host.com")

    when:
    properties.asInetAddress("key")

    then:
    thrown UnknownHostException
  }

  def "can get a property as a List<String>"() {
    if (propertyValue != null) {
      delegateProperties.setProperty("key", propertyValue)
    }

    expect:
    properties.asList("key") == expected

    where:
    propertyValue               | expected
    null                        | []
    ""                          | []
    "ratpack"                   | ["ratpack"]
    "ratpack,groovy,spock"      | ["ratpack", "groovy", "spock"]
    " ratpack, groovy , spock " | ["ratpack", "groovy", "spock"]
    "ratpack,"                  | ["ratpack"]
  }

  @Shared
  File file = new File(getClass().getResource("key.txt").toURI())

  def "can get a property that is a file path as an InputStream"() {
    given:
    delegateProperties.setProperty("key", propertyValue)

    expect:
    properties.asStream("key").text == content

    where:
    propertyValue                   | description
    file.absolutePath               | "a file path"
    file.toURI().toString()         | "a URI"
    "ratpack/util/internal/key.txt" | "a resource path"

    content = file.text
  }

  def "asStream throws an exception if the property value does not point to a valid resource"() {
    given:
    delegateProperties.setProperty("key", "some/invalid/path/foo.txt")

    when:
    properties.asStream("key")

    then:
    thrown FileNotFoundException
  }

  def "can get a property as a Class"() {
    given:
    delegateProperties.setProperty("key", ArrayList.name)

    expect:
    properties.asClass("key", List) == ArrayList
  }

  def "asClass throws an exception if the property value is not a valid class name"() {
    given:
    delegateProperties.setProperty("key", "com.enterprise.fizzbuzz.Strategy")

    when:
    properties.asClass("key", Serializable)

    then:
    thrown ClassNotFoundException
  }

  def "asClass throws an exception if the property value is the name of a class of the wrong type"() {
    given:
    delegateProperties.setProperty("key", HashMap.name)

    when:
    properties.asClass("key", List)

    then:
    thrown ClassCastException
  }

  def "asStream returns null if there is no property"() {
    expect:
    properties.asStream("key") == null
  }

}
