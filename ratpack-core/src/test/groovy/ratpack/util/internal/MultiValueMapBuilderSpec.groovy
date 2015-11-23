/*
 * Copyright 2015 the original author or authors.
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

import ratpack.util.MultiValueMap
import ratpack.util.MultiValueMapBuilder
import spock.lang.Specification


class MultiValueMapBuilderSpec extends Specification {

  MultiValueMapBuilder<String, String> builder = MultiValueMap.builder()

  def "can add values to map"() {
    when:
    builder.put("foo", "bar")
    builder.put("bar", "baz")
    def map = builder.build()

    then:
    map.get("foo") == "bar"
    map.getAll("foo") == ["bar"]

    map.get("bar") == "baz"
    map.getAll("bar") == ["baz"]
  }

  def "can add multiple values to key"() {
    when:
    builder.put("foo", "bar")
    builder.put("foo", "baz")
    def map = builder.build()

    then:
    map.get("foo") == "bar"
    map.getAll("foo") == ["bar", "baz"]
  }

  def "can add multiple values at once"() {
    when:
    builder.putAll("foo", ["bar", "baz"])
    builder.putAll("foo", ["bar2", "baz2"])
    def map = builder.build()

    then:
    map.get("foo") == "bar"
    map.getAll("foo") == ["bar", "baz", "bar2", "baz2"]
  }

  def "can add a map"() {
    when:
    builder.put("foo", "bar")
    builder.putAll([
      "foo": ["baz"],
      "bar": ["foobar"]
    ])
    def map = builder.build()

    then:
    map.get("foo") == "bar"
    map.getAll("foo") == ["bar", "baz"]
    map.getAll("bar") == ["foobar"]

  }
}
