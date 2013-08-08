/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.http

import org.ratpackframework.http.internal.DefaultMediaType
import spock.lang.Specification

class DefaultMediaTypeSpec extends Specification {

  def "parsing"() {
    expect:
    ct(" application/json ").type == "application/json"
    ct(" application/json ").params.isEmpty()
    ct(null).type == null
    ct(null).params.isEmpty()
    ct(" ").type == null
    ct(" ").params.isEmpty()
    ct(" application/json;charset=foo ").params.charset == "foo"
    ct(" application/json;charset ;foo=bar ").params == [charset: "", foo: "bar"]

  }

  def "tostring"() {
    expect:
    cts("") == ""
    cts("  application/json   ") == "application/json"
    cts("application/json;foo=bar") == "application/json;foo=bar"
    cts("application/json;foo") == "application/json;foo"
    cts("application/json;a=1 ; b=2") == "application/json;a=1;b=2"
  }

  private MediaType ct(s) {
    DefaultMediaType.get(s)
  }

  private String cts(s) {
    ct(s).toString()
  }
}
