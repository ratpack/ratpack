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

package org.ratpackframework.groovy;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.util.Action;

public abstract class Util {

  private Util() {
  }

  public static <T, R> R configureDelegateOnly(@DelegatesTo.Target T object, @DelegatesTo(strategy = Closure.DELEGATE_ONLY) Closure<R> configurer) {
    return configure(object, configurer, Closure.DELEGATE_ONLY);
  }

  public static <T, R> R configureDelegateFirst(@DelegatesTo.Target T object, @DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure<R> configurer) {
    return configure(object, configurer, Closure.DELEGATE_FIRST);
  }

  private static <T, R> R configure(T object, Closure<R> configurer, int resolveStrategy) {
    if (configurer == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Closure<R> clone = (Closure<R>) configurer.clone();
    clone.setDelegate(object);
    clone.setResolveStrategy(resolveStrategy);
    if (clone.getMaximumNumberOfParameters() == 0) {
      return clone.call();
    } else {
      return clone.call(object);
    }
  }

  // Type token is here for in the future when @DelegatesTo supports this kind of API
  public static <T> Action<T> action(@SuppressWarnings("UnusedParameters") Class<T> type, final Closure<?> configurer) {
    return new Action<T>() {
      public void execute(T object) {
        configureDelegateFirst(object, configurer);
      }
    };
  }

  public static Action<Object> action(final Closure<?> configurer) {
    return action(Object.class, configurer);
  }
}
