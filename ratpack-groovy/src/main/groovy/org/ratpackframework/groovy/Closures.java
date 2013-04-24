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

package org.ratpackframework.groovy;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.Action;

public abstract class Closures {

  public static <T, R> R configure(T object, @DelegatesTo(target = "object") Closure<R> configurer) {
    if (configurer == null) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Closure<R> clone = (Closure<R>) configurer.clone();
    clone.setDelegate(object);
    if (clone.getMaximumNumberOfParameters() == 0) {
      return clone.call();
    } else {
      return clone.call(object);
    }
  }

  // Type token is here for in the future when @DelegatesTo supports this kind of API
  public static <T> Action<T> handler(Class<T> type, final Closure<?> configurer) {
    return new Action<T>() {
      @Override
      public void execute(T object) {
        configure(object, configurer);
      }
    };
  }

}
