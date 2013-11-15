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

package ratpack.groovy;

import com.google.common.collect.ImmutableMap;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.handling.internal.ClosureBackedHandler;
import ratpack.groovy.internal.RatpackScriptBacking;
import ratpack.groovy.templating.Template;
import ratpack.groovy.templating.internal.DefaultTemplate;
import ratpack.guice.ModuleRegistry;
import ratpack.handling.Handler;

import java.util.Map;

public abstract class Groovy {

  private Groovy() {

  }

  /**
   * Starts a Ratpack app, defined by the given closure.
   * <p>
   * This method is used is Ratpack scripts as the entry point.
   * <pre class="tested">
   * import ratpack.session.store.MapSessionsModule
   * import static ratpack.groovy.Groovy.*
   *
   * ratpack {
   *   modules {
   *     // example of registering a module
   *     register(new MapSessionsModule(100, 60))
   *   }
   *   handlers {
   *     // define the application handlers
   *     get("foo") {
   *       render "bar"
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
   * The {@link ratpack.groovy.launch.GroovyRatpackMain} class provides such an entry point.
   * In such a mode, a script like above is still used to define the application, but the script is no longer the entry point.
   * Ratpack will manage the compilation and execution of the script internally.
   *
   * @param closure The definition closure, delegating to {@link ratpack.groovy.Groovy.Ratpack}
   */
  public static void ratpack(@DelegatesTo(value = Ratpack.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    RatpackScriptBacking.getBacking().execute(closure);
  }

  /**
   * The definition of a Groovy Ratpack application.
   *
   * @see ratpack.groovy.Groovy#ratpack(groovy.lang.Closure)
   */
  public static interface Ratpack {
    /**
     * Registers the closure used to configure the {@link ratpack.guice.ModuleRegistry} that will back the application.
     *
     * @param configurer The configuration closure, delegating to {@link ratpack.guice.ModuleRegistry}
     */
    void modules(@DelegatesTo(value = ModuleRegistry.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    /**
     * Registers the closure used to build the handler chain of the application.
     *
     * @param configurer The configuration closure, delegating to {@link ratpack.groovy.handling.GroovyChain}
     */
    void handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);
  }

  public static Template groovyTemplate(String id) {
    return groovyTemplate(id, null);
  }

  public static Template groovyTemplate(String id, String type) {
    return groovyTemplate(ImmutableMap.<String, Object>of(), id, type);
  }

  public static Template groovyTemplate(Map<String, ?> model, String id) {
    return groovyTemplate(model, id, null);
  }

  public static Template groovyTemplate(Map<String, ?> model, String id, String type) {
    return new DefaultTemplate(id, model, type);
  }

  public static Handler groovyHandler(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return new ClosureBackedHandler(closure);
  }

}
