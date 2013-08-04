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
import org.ratpackframework.groovy.internal.RatpackScriptBacking;

/**
 * Entry point for script based applications.
 */
abstract public class RatpackScript {

  private RatpackScript() {}

  /**
   * Starts a Ratpack app, defined by the given closure.
   * <p>
   * This method is used is Ratpack scripts as the entry point.
   * <pre class="tested">
   * import org.ratpackframework.session.store.MapSessionsModule
   * import static org.ratpackframework.groovy.RatpackScript.ratpack
   *
   * ratpack {
   *   modules {
   *     // example of registering a module
   *     register(new MapSessionsModule(100, 60))
   *   }
   *   handlers {
   *     // define the application handlers
   *     get("foo") {
   *       getResponse().send "bar"
   *     }
   *   }
   * }
   * </pre>
   * <h3>Standalone Scripts</h3>
   * <p>
   * This method can be used by standalone scripts to start a Ratpack application.
   * That is, you could save the above content in a file named “{@code ratpack.groovy}” (the name is irrelevant),
   * then start application by running `{@code groovy ratpack.groovy}` on the command line.
   * <h3>Full Applications</h3>
   * <p>
   * It's also possible to build Groovy Ratpack applications with a traditional class based entry point.
   * The {@link org.ratpackframework.groovy.launch.RatpackMain} class provides such an entry point.
   * In such a mode, a script like above is still used to define the application, but the script is no longer the entry point.
   * Ratpack will manage the compilation and execution of the script internally.
   *
   * @param closure The definition closure, delegating to {@link Ratpack}
   */
  public static void ratpack(@DelegatesTo(value = Ratpack.class, strategy = Closure.DELEGATE_ONLY) Closure<?> closure) {
    RatpackScriptBacking.getBacking().execute(closure);
  }

}
