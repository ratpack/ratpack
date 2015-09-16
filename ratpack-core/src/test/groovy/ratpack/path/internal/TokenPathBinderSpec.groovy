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

import ratpack.path.PathBinder
import ratpack.path.PathBinding
import spock.lang.Specification

class TokenPathBinderSpec extends Specification {

  PathBinding bind(String pattern, String path, boolean exact = false, PathBinding parent = null) {
    PathBinder.parse(pattern, exact).bind(Optional.ofNullable(parent).orElse(new RootPathBinding(path))).orElse(null)
  }

  Map<String, String> tokens(String pattern, String path, boolean exact = false, PathBinding parent = null) {
    bind(pattern, path, exact, parent)?.tokens
  }

  def "binding parts"() {
    when:
    true

    then:
    bind("a/b", "a/b/c").boundTo == "a/b"
    bind("a/b", "a/b/c").pastBinding == "c"
  }

  def tokens() {
    expect:
    tokens("a", "b") == null
    tokens("a", "a") == [:]
    tokens("a/::[bc]", "a/a") == null
    tokens("a/::[bc]", "a/b") == [:]
    tokens("a/::[bc]", "a/c") == [:]
    tokens("(.+)", "abc") == null
    tokens(":a", "abc") == [a: "abc"]
    tokens(":a", "abc/") == [a: "abc"]
    tokens(":a/:b", "abc/def") == [a: "abc", b: "def"]
    tokens(":a/:b?", "abc/def") == [a: "abc", b: "def"]
    tokens(":a/:b?", "abc") == [a: "abc"]
    tokens(":a/:b?/somepath", "abc") == null
    tokens(":a/:b?/somepath", "abc/somepath") == [a: "abc"]
    tokens(":a/:b?/somepath", "abc/def/somepath") == [a: "abc", b: "def"]
    tokens(":a/:b?/somepath", "abc/def/") == null
    tokens(":a/:b?/:c?", "abc") == [a: "abc"]
    tokens(":a/:b?/:c?", "abc/def") == [a: "abc", b: "def"]
    tokens(":a/:b?/:c?", "abc/def/ghi") == [a: "abc", b: "def", c: "ghi"]
    tokens(":a/:b", "foo") == null
    tokens("a/:b?", "a") == [:]
    tokens(":a", "%231") == [a: "#1"]
    tokens(":a", "foo%20bar") == [a: "foo bar"]
    tokens(":a", "foo%2Bbar") == [a: "foo+bar"]
    tokens(":a", "foo+bar") == [a: "foo+bar"]
    tokens(":a?", "") == [a: ""]
    tokens(":a?/b", "/b") == [a: ""]
    tokens(":a?/b/:c?", "/b/3") == [a: "", c: "3"]
    tokens("a/:b?", "a") == [:]
    tokens("a/:b?", "a/") == [b: ""]
    tokens(":a/:b?/:c?", "1//3") == [a: "1", b: "", c: "3"]

    when:
    tokens(":a/:b?/:c", "abc/def/ghi")

    then:
    def e = thrown(IllegalArgumentException)
    e.message == "Cannot add mandatory parameter c after optional parameters"
  }


}
