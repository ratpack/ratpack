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

import com.google.common.base.Predicates
import com.google.common.reflect.TypeToken
import ratpack.func.Action
import spock.lang.Specification

class SingleEntryRegistrySpec extends Specification {

  def r
  def sameType = TypeToken.of(String.class)
  def differentType = TypeToken.of(Number.class)
  def value = "Something"

  def setup() {
    r = new SingleEntryRegistry(new DefaultRegistryEntry(sameType, value))
  }

  def "find first"() {
    expect:
    r.first(sameType, Predicates.alwaysTrue()) == value
    r.first(sameType, Predicates.alwaysFalse()) == null
    r.first(differentType, Predicates.alwaysTrue()) == null
    r.first(differentType, Predicates.alwaysFalse()) == null
  }

  def "find all"() {
    expect:
    r.all(sameType, Predicates.alwaysTrue()) == [value]
    r.all(sameType, Predicates.alwaysFalse()) == []
    r.all(differentType, Predicates.alwaysTrue()) == []
    r.all(differentType, Predicates.alwaysFalse()) == []
  }

  def "first with action"() {
    given:
    Action action = Mock()

    when:
    r.first(sameType, Predicates.alwaysTrue(), action)

    then:
    1 * action.execute(value)

    when:
    r.first(sameType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)

    when:
    r.first(differentType, Predicates.alwaysTrue(), action)
    r.first(differentType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)
  }

  def "each with action"() {
    given:
    Action action = Mock()

    when:
    r.each(sameType, Predicates.alwaysTrue(), action)

    then:
    1 * action.execute(value)

    when:
    r.each(sameType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)

    when:
    r.each(differentType, Predicates.alwaysTrue(), action)
    r.each(differentType, Predicates.alwaysFalse(), action)

    then:
    0 * action.execute(_)
  }
}
