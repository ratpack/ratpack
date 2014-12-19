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
import groovy.xml.MarkupBuilder;
import io.netty.util.CharsetUtil;
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.groovy.guice.GroovyBindingsSpec;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.handling.GroovyContext;
import ratpack.groovy.handling.internal.ClosureBackedHandler;
import ratpack.groovy.handling.internal.DefaultGroovyChain;
import ratpack.groovy.handling.internal.DefaultGroovyContext;
import ratpack.groovy.handling.internal.GroovyDslChainActionTransformer;
import ratpack.groovy.internal.ClosureInvoker;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.internal.RatpackScriptBacking;
import ratpack.groovy.template.Markup;
import ratpack.groovy.template.MarkupTemplate;
import ratpack.groovy.template.TextTemplate;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.internal.ChainBuilders;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;

import java.nio.charset.Charset;
import java.util.Map;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * Static methods for specialized Groovy integration.
 */
public abstract class Groovy {

  private Groovy() {

  }

  /**
   * Starts a Ratpack app, defined by the given closure.
   * <p>
   * This method is used in Ratpack scripts as the entry point.
   * <pre class="groovy-ratpack-dsl">
   * import ratpack.session.SessionModule
   * import static ratpack.groovy.Groovy.*
   *
   * ratpack {
   *   bindings {
   *     // example of registering a module
   *     add(new SessionModule())
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
   * then start the application by running `{@code groovy ratpack.groovy}` on the command line.
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
    try {
      RatpackScriptBacking.execute(closure);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * The definition of a Groovy Ratpack application.
   *
   * @see ratpack.groovy.Groovy#ratpack(groovy.lang.Closure)
   */
  public static interface Ratpack {

    /**
     * Registers the closure used to configure the {@link ratpack.groovy.guice.GroovyBindingsSpec} that will back the application.
     *
     * @param configurer The configuration closure, delegating to {@link ratpack.groovy.guice.GroovyBindingsSpec}
     */
    void bindings(@DelegatesTo(value = GroovyBindingsSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    /**
     * Registers the closure used to build the handler chain of the application.
     *
     * @param configurer The configuration closure, delegating to {@link GroovyChain}
     */
    void handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

  }

  /**
   * Builds a handler chain, with no backing registry.
   *
   * @param launchConfig The application launch config
   * @param closure The chain definition
   * @return A handler
   * @throws Exception any exception thrown by the given closure
   */
  public static Handler chain(LaunchConfig launchConfig, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return chain(launchConfig, null, closure);
  }

  /**
   * Creates a specialized Groovy context.
   *
   * @param context The context to convert to a Groovy context
   * @return The original context wrapped in a Groovy context
   */
  public static GroovyContext context(Context context) {
    return context instanceof GroovyContext ? (GroovyContext) context : new DefaultGroovyContext(context);
  }

  /**
   * Builds a chain, backed by the given registry.
   *
   * @param launchConfig The application launch config
   * @param registry The registry.
   * @param closure The chain building closure.
   * @return A handler
   * @throws Exception any exception thrown by the given closure
   */
  public static Handler chain(@Nullable LaunchConfig launchConfig, @Nullable Registry registry, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return ChainBuilders.build(
      launchConfig != null && launchConfig.isDevelopment(),
      new GroovyDslChainActionTransformer(launchConfig, registry),
      new ClosureInvoker<Object, GroovyChain>(closure).toAction(registry, Closure.DELEGATE_FIRST)
    );
  }

  /**
   * Creates a chain action based on the given closure.
   *
   * @param closure The chain building closure.
   * @return A chain action
   * @throws Exception any exception thrown by the given closure
   */
  public static Action<Chain> chainAction(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) throws Exception {
    return chain -> ClosureUtil.configureDelegateFirst(new DefaultGroovyChain(chain), closure);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template, using no model and the default content type.
   *
   * @param id The id/name of the template
   * @return a template
   */
  public static TextTemplate groovyTemplate(String id) {
    return groovyTemplate(id, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using no model and the default content type.
   *
   * @param id The id/name of the template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(String id) {
    return groovyMarkupTemplate(id, (String) null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template, using no model.
   *
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static TextTemplate groovyTemplate(String id, String type) {
    return groovyTemplate(ImmutableMap.<String, Object>of(), id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using no model.
   *
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(String id, String type) {
    return groovyMarkupTemplate(ImmutableMap.<String, Object>of(), id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template, using the default content type.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @return a template
   */
  public static TextTemplate groovyTemplate(Map<String, ?> model, String id) {
    return groovyTemplate(model, id, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using the default content type.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(Map<String, ?> model, String id) {
    return groovyMarkupTemplate(model, id, null);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template, using the default content type.
   *
   * @param id the id/name of the template
   * @param modelBuilder an action the builds a model map
   * @return a template
   * @throws Exception any thrown by {@code modelBuilder}
   */
  public static MarkupTemplate groovyMarkupTemplate(String id, Action<? super ImmutableMap.Builder<String, Object>> modelBuilder) throws Exception {
    return groovyMarkupTemplate(id, null, modelBuilder);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based markup template.
   *
   * @param id the id/name of the template
   * @param type The content type of template
   * @param modelBuilder an action the builds a model map
   * @return a template
   * @throws Exception any thrown by {@code modelBuilder}
   */
  public static MarkupTemplate groovyMarkupTemplate(String id, String type, Action<? super ImmutableMap.Builder<String, Object>> modelBuilder) throws Exception {
    ImmutableMap<String, Object> model = Action.with(ImmutableMap.<String, Object>builder(), Action.noopIfNull(modelBuilder)).build();
    return groovyMarkupTemplate(model, id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static TextTemplate groovyTemplate(Map<String, ?> model, String id, String type) {
    return new TextTemplate(model, id, type);
  }

  /**
   * Creates a {@link ratpack.handling.Context#render(Object) renderable} Groovy based template.
   *
   * @param model The template model
   * @param id The id/name of the template
   * @param type The content type of template
   * @return a template
   */
  public static MarkupTemplate groovyMarkupTemplate(Map<String, ?> model, String id, String type) {
    return new MarkupTemplate(id, type, model);
  }

  /**
   * Creates a handler instance from a closure.
   *
   * @param closure The closure to convert to a handler
   * @return The created handler
   */
  public static Handler groovyHandler(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return new ClosureBackedHandler(closure);
  }

  /**
   * Immediately executes the given {@code closure} against the given {@code chain}, as a {@link GroovyChain}.
   *
   * @param chain the chain to add handlers to
   * @param closure the definition of handlers to add
   * @throws Exception any exception thrown by {@code closure}
   */
  public static void chain(Chain chain, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    GroovyChain groovyChain = chain instanceof GroovyChain ? (GroovyChain) chain : new DefaultGroovyChain(chain);
    new ClosureInvoker<Object, GroovyChain>(closure).invoke(chain.getRegistry(), groovyChain, Closure.DELEGATE_FIRST);
  }

  /**
   * Creates a chain action implementation from the given closure.
   *
   * @param closure the definition of handlers to add
   * @throws Exception any exception thrown by {@code closure}
   * @return The created action
   */
  public static Action<Chain> chain(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) throws Exception {
    return (c) -> Groovy.chain(c, closure);
  }

  /**
   * Shorthand for {@link #markupBuilder(CharSequence, Charset, Closure)} with a content type of {@code "text/html"} and {@code "UTF-8"} encoding.
   *
   * @param closure The html definition
   * @return A renderable object (i.e. to be used with the {@link ratpack.handling.Context#render(Object)} method
   */
  public static Markup htmlBuilder(@DelegatesTo(value = MarkupBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return markupBuilder(HttpHeaderConstants.HTML_UTF_8, CharsetUtil.UTF_8, closure);
  }

  /**
   * Renderable object for markup built using Groovy's {@link MarkupBuilder}.
   *
   * <pre class="groovy-chain-dsl">
   * import static ratpack.groovy.Groovy.markupBuilder
   *
   * get("some/path") {
   *   render markupBuilder("text/html", "UTF-8") {
   *     // MarkupBuilder DSL in here
   *   }
   * }
   * </pre>
   *
   * @param contentType The content type of the markup
   * @param encoding The character encoding of the markup
   * @param closure The definition of the markup
   * @return A renderable object (i.e. to be used with the {@link ratpack.handling.Context#render(Object)} method
   */
  public static Markup markupBuilder(CharSequence contentType, CharSequence encoding, @DelegatesTo(value = MarkupBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return new Markup(contentType, Charset.forName(encoding.toString()), closure);
  }

  public static Markup markupBuilder(CharSequence contentType, Charset encoding, @DelegatesTo(value = MarkupBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    return new Markup(contentType, encoding, closure);
  }

}
