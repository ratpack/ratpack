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

package ratpack.path.internal

import ratpack.path.PathBinding
import spock.lang.Specification

class TokenPathBinderSpec extends Specification {

  Map<String, String> bind(String pattern, String path, PathBinding parent = null, boolean exact = true) {
    new TokenPathBinder(pattern, exact).bind(path, parent)?.tokens
  }

  def bind() {
    expect:
    bind("a", "b") == null
    bind("a", "a") == [:]
    bind("(.+)", "abc") == null
    bind(":a", "abc") == [a: "abc"]
    bind(":a", "abc/") == [a: "abc"]
    bind(":a/:b", "abc/def") == [a: "abc", b: "def"]
    bind(":a/:b?", "abc/def") == [a: "abc", b: "def"]
    bind(":a/:b?", "abc") == [a: "abc"]
    bind(":a/:b?/somepath", "abc") == null
    bind(":a/:b?/somepath", "abc/somepath") == [a: "abc"]
    bind(":a/:b?/somepath", "abc/def/somepath") == [a: "abc", b: "def"]
    bind(":a/:b?/somepath", "abc/def/") == null
    bind(":a/:b?/:c?", "abc") == [a: "abc"]
    bind(":a/:b?/:c?", "abc/def") == [a: "abc", b: "def"]
    bind(":a/:b?/:c?", "abc/def/ghi") == [a: "abc", b: "def", c: "ghi"]
    bind(":a/:b", "foo") == null
    bind("a/:b?", "a") == [:]
    bind(":a", "%231") == [a: "#1"]
    bind(":a", "foo%20bar") == [a: "foo bar"]
    bind(":a", "foo%2Bbar") == [a: "foo+bar"]
    bind(":a", "foo+bar") == [a: "foo+bar"]

    when:
    bind(":a/:b?/:c", "abc/def/ghi")

    then:
    def e = thrown(IllegalArgumentException)
    e.message == "path :a/:b?/:c should not define mandatory parameters after an optional parameter"
  }


}
