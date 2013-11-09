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

package ratpack.http.internal

import spock.lang.Specification

class MimeParseSpec extends Specification {

  def "matching"() {
    expect:
    match("a/a", "a/b") == ""
    match("a/a", "a/b", "a/a") == "a/a"
    match("a/a;q=0.5,a/b;q=1", "a/b", "a/a") == "a/b"
    match("a/a;q=0.9,a/b", "a/b", "a/a") == "a/b"     // Verify default value of 'q' is 1.0
    match("a/a;q=1;a/b;q=0.5,*", "a/c") == "a/c"
    match("*", "a/c", "a/b") == "a/b"
  }

  String match(String header, String... supported) {
    MimeParse.bestMatch(supported.toList(), header)
  }
}
