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

package ratpack.registry

import com.google.common.base.Predicate
import groovy.transform.EqualsAndHashCode
import spock.lang.Specification

import static com.google.common.base.Predicates.*
import static ratpack.registry.PredicateCacheability.isCacheable

class PredicateCacheabilitySpec extends Specification {

  def "guava constant predicates are cacheable"() {
    expect:
    isCacheable(alwaysTrue())
    isCacheable(alwaysFalse())
    isCacheable(notNull())
    isCacheable(isNull())
  }

  def "composite predicates are cacheable"() {
    expect:
    isCacheable(and(alwaysTrue(), alwaysTrue()))
    isCacheable(or(alwaysTrue(), alwaysTrue()))
  }

  def "predicate classes without an equals method aren't cacheable"() {
    expect:
    !isCacheable(new NonCacheablePredicate())
  }

  def "predicate classes with an equals method should be cacheable"() {
    expect:
    isCacheable(new CacheablePredicate())
  }

  def "predicates in enum inner class should not throw exception"() {
    given:
    def pred = EnumPredicates.ObjectPredicate.ALWAYS_TRUE.withNarrowedType()

    expect:
    !isCacheable(pred)
  }

  def "closure predicates are cacheable"() {
    given:
    def pred = { String s -> false } as Predicate<String>

    expect:
    isCacheable(pred)
  }

  static class NonCacheablePredicate implements Predicate<Number> {
    @Override
    boolean apply(Number input) {
      return false
    }
  }

  @EqualsAndHashCode
  static class CacheablePredicate implements Predicate<Number> {
    @Override
    boolean apply(Number input) {
      return false
    }
  }

}




