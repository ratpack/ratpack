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

package ratpack.registry;

import com.google.common.base.Predicate;


/**
 * Class for testing special cases of PredicateCacheability.isCacheable (in PredicateCacheabilitySpec)
 *
 * Created by lari on 02/09/14.
 */
public class EnumPredicates {

  // code copied from private com.google.common.base.Predicates.ObjectPredicates class, for testing special case Predicates
  enum ObjectPredicate implements Predicate<Object> {
    /** @see com.google.common.base.Predicates#alwaysTrue() */
    ALWAYS_TRUE {
      @Override public boolean apply(Object o) {
        return true;
      }
      @Override public String toString() {
        return "Predicates.alwaysTrue()";
      }
    },
    /** @see com.google.common.base.Predicates#alwaysFalse() */
    ALWAYS_FALSE {
      @Override public boolean apply(Object o) {
        return false;
      }
      @Override public String toString() {
        return "Predicates.alwaysFalse()";
      }
    },
    /** @see com.google.common.base.Predicates#isNull() */
    IS_NULL {
      @Override public boolean apply(Object o) {
        return o == null;
      }
      @Override public String toString() {
        return "Predicates.isNull()";
      }
    },
    /** @see com.google.common.base.Predicates#notNull() */
    NOT_NULL {
      @Override public boolean apply(Object o) {
        return o != null;
      }
      @Override public String toString() {
        return "Predicates.notNull()";
      }
    };

    @SuppressWarnings("unchecked") // safe contravariant cast
    <T> Predicate<T> withNarrowedType() {
      return (Predicate<T>) this;
    }
  }

}
