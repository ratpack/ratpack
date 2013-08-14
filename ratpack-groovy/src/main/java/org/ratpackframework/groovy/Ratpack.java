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
import org.ratpackframework.groovy.handling.Chain;
import org.ratpackframework.guice.ModuleRegistry;

/**
 * The definition of a Groovy Ratpack application.
 *
 * @see RatpackScript#ratpack(groovy.lang.Closure)
 */
public interface Ratpack {

  /**
   * Registers the closure used to configure the {@link ModuleRegistry} that will back the application.
   *
   * @param configurer The configuration closure, delegating to {@link ModuleRegistry}
   */
  void modules(@DelegatesTo(value = ModuleRegistry.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

  /**
   * Registers the closure used to build the handler chain of the application.
   *
   * @param configurer The configuration closure, delegating to {@link Chain}
   */
  void handlers(@DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

}
