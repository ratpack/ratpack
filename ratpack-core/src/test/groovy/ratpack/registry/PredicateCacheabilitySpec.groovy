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
import com.google.common.base.Predicates
import groovy.transform.EqualsAndHashCode
import spock.lang.Specification

class PredicateCacheabilitySpec extends Specification {
  def "isCacheable should work with Predicates.alwaysTrue/False methods"() {
    expect:
      PredicateCacheability.isCacheable(Predicates.alwaysTrue()) == true
      PredicateCacheability.isCacheable(Predicates.alwaysFalse()) == true
  }

  def "Predicate classes without an equals method aren't cacheable"() {
    expect:
      PredicateCacheability.isCacheable(new NonCacheablePredicate()) == false
  }

  def "Predicate classes with an equals method should be cacheable"() {
    expect:
    PredicateCacheability.isCacheable(new CacheablePredicate()) == true
  }

  def "Predicates in enum inner class should not throw exception"() {
    given:
      def pred = EnumPredicates.ObjectPredicate.ALWAYS_TRUE.withNarrowedType()
    expect:
      PredicateCacheability.isCacheable(pred) == false
  }

  def "closure predicates are cacheable"() {
     given:
      def pred = {String s -> false} as Predicate<String>
     expect:
      PredicateCacheability.isCacheable(pred) == true
  }
}

class NonCacheablePredicate implements Predicate<Number> {
  @Override
  boolean apply(Number input) {
    return false
  }
}

@EqualsAndHashCode
class CacheablePredicate implements Predicate<Number> {
  @Override
  boolean apply(Number input) {
    return false
  }
}



