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

package org.ratpackframework.path.internal

import spock.lang.Specification

class TokenPathBinderTest extends Specification {

  Map<String, String> map(String pattern, String path) {
    new TokenPathBinder(pattern, true).bind(path, null)?.tokens
  }

  def "map"() {
    expect:
    map("a", "b") == null
    map("a", "a") == [:]
    map("(.+)", "abc") == null
    map(":a", "abc") == [a: "abc"]
    map(":a/:b", "abc/def") == [a: "abc", b: "def"]
    map(":a/:b?", "abc/def") == [a: "abc", b: "def"]
    map(":a/:b?", "abc") == [a: "abc"]
    map(":a/:b?/somepath", "abc") == null
    map(":a/:b?/somepath", "abc/somepath") == [a: "abc"]
    map(":a/:b?/somepath", "abc/def/somepath") == [a: "abc", b:"def"]
    map(":a/:b?/somepath", "abc/def/") == null
    map(":a/:b?:c?", "abc") == [a: "abc"]
    map(":a/:b?:c?", "abc/def") == [a: "abc", b:"def"]
    map(":a/:b?:c?", "abc/def/ghi") == [a: "abc", b:"def", c:"ghi"]

    when:
    map(":a/:b?:c", "abc/def/ghi")
    then:
    def e = thrown(java.lang.IllegalArgumentException)
    e.message == "path :a/:b?:c should not define mandatory parameters after an optional parameter"
  }

}
