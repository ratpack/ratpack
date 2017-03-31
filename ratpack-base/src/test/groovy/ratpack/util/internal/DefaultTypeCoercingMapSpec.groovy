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

import com.google.common.collect.ImmutableMap
import spock.lang.Specification

class DefaultTypeCoercingMapSpec extends Specification {

  def "can retrieve #type token"() {
    given:
    def tokens = new DefaultTypeCoercingMap(ImmutableMap.of("a", tokenValue))

    expect:
    tokens."$method"("a") == coercedValue

    where:
    tokenValue | method    || coercedValue
    "a"        | "get"     || "a"
    "1"        | "asInt"   || 1i
    "TRUE"     | "asBool"  || true
    "1"        | "asByte"  || (byte) 1
    "1"        | "asShort" || (short) 1
    "1"        | "asLong"  || 1L

    type = coercedValue.getClass().simpleName
  }

  def "throws exception if token value cannot be parsed as a #type"() {
    given:
    def tokens = new DefaultTypeCoercingMap(ImmutableMap.of("a", "a"))

    when:
    tokens."$method"("a")

    then:
    thrown NumberFormatException

    where:
    method << ["asInt", "asByte", "asShort", "asLong"]
    type = DefaultTypeCoercingMap.getMethod(method, Object).returnType.simpleName
  }

  def "#method returns null if delegate map does not contain the key"() {
    given:
    def tokens = new DefaultTypeCoercingMap(ImmutableMap.of())

    expect:
    tokens."$method"("a") is null

    where:
    method << ["get", "asInt", "asBool", "asByte", "asShort", "asLong"]
  }

}
