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

import spock.lang.Specification

class MutableMediaTypeTest extends Specification {

  def "init"() {
    expect:
    new MutableMediaType().toString() == ""
    new MutableMediaType().base == null
    new MutableMediaType().params.isEmpty()
  }

  def "mutability"() {
    expect:
    m("foo/bar").base("application/json").base == "application/json"
    m("foo/bar").base("application/json").params(a: 1).toString() == "application/json;a=1"
    m("foo/bar").utf8("application/json").toString() == "application/json;charset=utf-8"
  }

  private MutableMediaType m(s) {
    new MutableMediaType(s)
  }

  private String cts(s) {
    m(s).toString()
  }
}
