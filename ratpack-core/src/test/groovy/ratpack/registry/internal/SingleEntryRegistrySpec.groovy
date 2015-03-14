/*
 * Copyright 2014 the original author or authors.
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

package ratpack.registry.internal

import com.google.common.reflect.TypeToken
import ratpack.func.Function
import spock.lang.Specification

class SingleEntryRegistrySpec extends Specification {

  def r
  def sameType = TypeToken.of(String)
  def differentType = TypeToken.of(Number)
  def value = "Something"

  def setup() {
    r = new SingleEntryRegistry(new DefaultRegistryEntry(sameType, value))
  }

  def "find first"() {
    expect:
    r.first(sameType, Function.identity()).get() == value
    !r.first(sameType, Function.constant(null)).present
    !r.first(differentType, Function.identity()).present
    !r.first(differentType, Function.constant(null)).present
  }

}
