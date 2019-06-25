/*
 * Copyright 2015 the original author or authors.
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

package ratpack.handling;

import ratpack.func.Action;
import ratpack.registry.Registry;

/**
 * Decorates a handler, effectively wrapping it.
 * <p>
 * Handler decorators can be used to contribute to the handler chain from the {@link ratpack.server.RatpackServerSpec#registry server registry}.
 * It is often used by libraries/extensions to participate in request handling in order to set up infrastructure for downstream handlers.
 * <p>
 * The {@link #prepend(Handler)} method is a convenient way to create a decorator that simply prepends a given handler to the application handlers.
 * <pre class="java">{@code
 * import ratpack.handling.Handler;
 * import ratpack.handling.HandlerDecorator;
 * import ratpack.handling.Context;
 * import ratpack.util.Types;
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.*;
 *
 * import java.util.LinkedList;
 * import java.util.Arrays;
 *
 * public class Example {
 *   public static class AddListToRequestRegistry implements Handler {
 *     public void handle(Context context) {
 *       context.getRequest().add(Types.listOf(String.class), new LinkedList<String>());
 *       context.next();
 *     }
 *   }
 *
 *   public static class AddStringToList implements Handler {
 *     private final String string;
 *     public AddStringToList(String string) { this.string = string; }
 *     public void handle(Context ctx) {
 *       ctx.getRequest().get(Types.listOf(String.class)).add(string);
 *       ctx.next();
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registryOf(r -> r
 *         .add(HandlerDecorator.prepend(new AddListToRequestRegistry()))
 *         .add(HandlerDecorator.prepend(new AddStringToList("foo")))
 *         .add(HandlerDecorator.prepend(new AddStringToList("bar")))
 *       )
 *       .handler(r ->
 *         ctx -> ctx.render(ctx.getRequest().get(Types.listOf(String.class)).toString())
 *       )
 *     ).test(httpClient ->
 *       assertEquals(Arrays.asList("foo", "bar").toString(), httpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 * <p>
 * Handler decorators can't do anything that handlers can't do.
 * In the above example, the same result could have been achieved by just using the handler implementations.
 * Decorators are usually more useful when using something like Guice and where it is desired for a module to contribute handlers.
 * <pre class="java">{@code
 * import ratpack.handling.Handler;
 * import ratpack.handling.HandlerDecorator;
 * import ratpack.handling.Context;
 * import ratpack.registry.Registry;
 * import ratpack.test.embed.EmbeddedApp;

 * import ratpack.guice.Guice;
 * import com.google.inject.AbstractModule;
 * import com.google.inject.Inject;
 * import com.google.inject.multibindings.Multibinder;
 *
 * import static org.junit.Assert.*;
 *
 * import java.util.LinkedList;
 * import java.util.Arrays;
 *
 * public class Example {
 *
 *   public static class MaintenanceWindow {
 *     public boolean active = true;
 *   }
 *
 *   public static class HandlerDecoratorImpl implements HandlerDecorator {
 *     private final MaintenanceWindow maintenanceWindow;
 *
 *     {@literal @}Inject
 *     public HandlerDecoratorImpl(MaintenanceWindow maintenanceWindow) {
 *       this.maintenanceWindow = maintenanceWindow;
 *     }
 *
 *     public Handler decorate(Registry serverRegistry, Handler rest) {
 *       return ctx -> {
 *         if (maintenanceWindow.active) {
 *           ctx.render("down for maintenance!");
 *         } else {
 *           ctx.insert(rest);
 *         }
 *       };
 *     }
 *   }
 *
 *   public static class MaintenanceWindowModule extends AbstractModule {
 *     protected void configure() {
 *       bind(MaintenanceWindow.class);
 *       // Important to use a multi binding to allow other modules to also contribute handlers
 *       Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().to(HandlerDecoratorImpl.class);
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b -> b
 *         .module(MaintenanceWindowModule.class)
 *       ))
 *       .handler(r ->
 *         ctx -> ctx.render("ok!")
 *       )
 *     ).test(httpClient -> {
 *       assertEquals("down for maintenance!", httpClient.getText());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * Handler decorators are invoked in the <i>reverse</i> order in which they are defined in the server registry.
 * That is, the reverse of the order that they were added to the server registry.
 * This means that earlier decorators decorate the handlers returned by later decorators.
 * Said another way, the handler returned by the <i>first</i> decorator returned from the server registry will be the first handler to handle requests.
 */
public interface HandlerDecorator {

  /**
   * A handler decorator implementation that does not decorate the handler.
   *
   * That is, it just returns the given handler.
   *
   * @return a handler decorator that does not decorate the handler
   */
  static HandlerDecorator noop() {
    return (registry, rest) -> rest;
  }

  /**
   * Creates a new handler that decorates the application handlers, given as the {@code rest} argument.
   * <p>
   * As the {@code rest} argument encapsulates the application handlers, the returned handler should generally delegate to it (via {@link Context#insert(Handler...)}).
   *
   * @param serverRegistry the server registry
   * @param rest the rest of the handlers of the application
   * @return a new handler
   * @throws Exception any
   */
  Handler decorate(Registry serverRegistry, Handler rest) throws Exception;

  /**
   * A factory for decorator impls that effectively inserts the given handler before the “rest” of the handlers.
   * <p>
   * The given handler should call {@link Context#next()} to delegate to the rest of the handlers.
   *
   * @param handler the handler to prependHandlers
   * @return a handler decorator implementation
   */
  static HandlerDecorator prepend(Handler handler) {
    return (registry, rest) -> Handlers.chain(handler, rest);
  }

  /**
   * A factory for decorator impls that effectively inserts the given handler before the “rest” of the handlers.
   * <p>
   * This is useful for cases when a handler is installed from the registry outside of the
   * usual {@link ratpack.server.RatpackServerSpec#handlers(Action)}} method, e.g. from an external module.
   * <p>
   * The given handler should call {@link Context#next()} to delegate to the rest of the handlers.
   *
   * @param handler a class defining the handler
   * @return a handler decorator implementation
   * @since 1.5
   */
  static HandlerDecorator prepend(Class<? extends Handler> handler) {
    return (registry, rest) -> Handlers.chain(registry.get(handler), rest);
  }

  /**
   * A factory for decorator impls that effectively inserts the given chain before the “rest” of the handlers.
   * The chain may define handlers that render results or call {@link Context#next()} to delegate to the
   * rest of the handlers.
   *
   * @param handlers an action defining a handler chain
   * @return a handler decorator implementation
   * @since 1.5
   */
  static HandlerDecorator prependHandlers(Action<? super Chain> handlers) {
    return (registry, rest) -> Handlers.chain(Handlers.chain(registry, handlers), rest);
  }

  /**
   * A factory for decorator impls that effectively inserts the given chain before the “rest” of the handlers.
   * The chain may define handlers that render results or call {@link Context#next()} to delegate to the
   * rest of the handlers.
   * <p>
   * This is useful for cases when a group of handlers is installed from the registry outside of the
   * usual {@link ratpack.server.RatpackServerSpec#handlers(Action)}} method, e.g. from an external module.
   *
   * @param handlers a class defining an action for a handler chain
   * @return a handler decorator implementation
   * @since 1.5
   */
  static HandlerDecorator prependHandlers(Class<? extends Action<? super Chain>> handlers) {
    return (registry, rest) -> Handlers.chain(Handlers.chain(registry, registry.get(handlers)), rest);
  }

}
