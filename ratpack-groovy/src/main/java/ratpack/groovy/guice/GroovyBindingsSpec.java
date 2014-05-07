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

package ratpack.groovy.guice;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.guice.BindingsSpec;

/**
 * Groovy specific extensions to {@link ratpack.guice.BindingsSpec}.
 */
public interface GroovyBindingsSpec extends BindingsSpec {

  /**
   * Adds a closure based application initializer.
   * <p>
   * The closure can declare parameters, that will be injected.
   * That is, parameters must be typed and implementations of such types must be provided by the modules.
   *
   * @param closure The initializer
   */
  void init(@DelegatesTo(value = Void.class, strategy = Closure.OWNER_ONLY) Closure<?> closure);

}
