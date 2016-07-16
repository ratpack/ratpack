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

package ratpack.service;

import ratpack.api.NonBlocking;
import ratpack.exec.Blocking;
import ratpack.func.Action;
import ratpack.server.RatpackServer;

/**
 * A service participates in the application lifecycle.
 * <p>
 * When the application starts, all services in the server registry will be notified.
 * Similarly when the application stops.
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.service.Service;
 * import ratpack.service.StartEvent;
 * import ratpack.service.StopEvent;
 *
 * import java.util.List;
 * import java.util.LinkedList;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   static class RecordingService implements Service {
 *     public final List<String> events = new LinkedList<>();
 *
 *     public void onStart(StartEvent event) {
 *       events.add("start");
 *     }
 *
 *     public void onStop(StopEvent event) {
 *       events.add("stop");
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     RecordingService service = new RecordingService();
 *
 *     RatpackServer server = RatpackServer.of(s -> s
 *       .serverConfig(ServerConfig.embedded())
 *       .registryOf(r -> r.add(service))
 *       .handler(r -> ctx -> ctx.render("ok"))
 *     );
 *
 *     assertEquals("[]", service.events.toString());
 *     server.start();
 *     assertEquals("[start]", service.events.toString());
 *     server.reload();
 *     assertEquals("[start, stop, start]", service.events.toString());
 *     server.stop();
 *     assertEquals("[start, stop, start, stop]", service.events.toString());
 *   }
 * }
 * }</pre>
 *
 * <h3>Ordering</h3>
 * <p>
 * Services can be ordered by the {@link DependsOn} and {@link ServiceDependencies} mechanisms.
 *
 * <h3>Async services</h3>
 * <p>
 * The {@link #onStart} and {@link #onStop} methods are always executed within a distinct {@link ratpack.exec.Execution}, for each service.
 * This means that implementations of these methods are free to perform async ops (e.g. use the {@link ratpack.http.client.HttpClient HTTP client}, or {@link Blocking block}).
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.service.Service;
 * import ratpack.service.StartEvent;
 * import ratpack.service.StopEvent;
 * import ratpack.exec.Promise;
 *
 * import java.util.List;
 * import java.util.LinkedList;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   static class RecordingService implements Service {
 *     public final List<String> events = new LinkedList<>();
 *
 *     public void onStart(StartEvent event) {
 *       Promise.value("start").map(String::toUpperCase).then(events::add);
 *     }
 *
 *     public void onStop(StopEvent event) {
 *       Promise.value("stop").map(String::toUpperCase).then(events::add);
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     RecordingService service = new RecordingService();
 *
 *     RatpackServer server = RatpackServer.of(s -> s
 *       .serverConfig(ServerConfig.embedded())
 *       .registryOf(r -> r.add(service))
 *       .handler(r -> ctx -> ctx.render("ok"))
 *     );
 *
 *     server.start();
 *     assertEquals("[START]", service.events.toString());
 *     server.stop();
 *     assertEquals("[START, STOP]", service.events.toString());
 *   }
 * }
 * }</pre>
 * <p>
 * There is no need to catch promise errors.
 * An error handler for the execution is installed that effectively treats any exceptions as if they were thrown by the method.
 *
 * <h3>Relationship with “business-logic”</h3>
 * <p>
 * This interface does need to be used for business-logic type services, unless such services need to participate in the application lifecycle.
 * Even in such a case, it is generally better to decouple the business-logic type service from Ratpack (i.e. this interface) and have a
 * {@link Service} implementation that drives the business-logic service.
 *
 * <h3>Accessing the server registry</h3>
 * <p>
 * The event objects given to the start/stop methods provide access to the server registry.
 * This can be used, for example, to get hold of a database connection that was added to the server registry as part of the server definition.
 * <p>
 * Alternatively, when using the Guice support the service can be injected by Guice.
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.service.Service;
 * import ratpack.service.StartEvent;
 * import ratpack.service.StopEvent;
 * import ratpack.guice.Guice;
 * import ratpack.util.Types;
 * import javax.inject.Inject;
 *
 * import java.util.List;
 * import java.util.LinkedList;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   static class RecordingService implements Service {
 *     public final List<String> events;
 *
 *     {@literal @}Inject
 *     public RecordingService(List<String> events) {
 *       this.events = events;
 *     }
 *
 *     public void onStart(StartEvent event) {
 *       events.add("start");
 *     }
 *
 *     public void onStop(StopEvent event) {
 *       events.add("stop");
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     List<String> list = new LinkedList<>();
 *
 *     RatpackServer server = RatpackServer.of(s -> s
 *       .serverConfig(ServerConfig.embedded())
 *       .registry(Guice.registry(b -> b
 *         .bindInstance(Types.listOf(String.class), list)
 *         .bind(RecordingService.class)
 *       ))
 *       .handler(r -> ctx -> ctx.render("ok"))
 *     );
 *
 *     server.start();
 *     assertEquals("[start]", list.toString());
 *     server.stop();
 *     assertEquals("[start, stop]", list.toString());
 *   }
 * }
 * }</pre>
 *
 * <h3>Errors</h3>
 * <p>
 * If an {@code onStart} method errors, it will prevent the “application” from launching.
 * In {@link ratpack.server.ServerConfig#isDevelopment() development mode}, the application will start and display the error page for all requests with the error.
 * When not in development mode, the exception will be thrown from the {@link RatpackServer#start()} method.
 * If starting the app in a “main method”, this will prevent the JVM from starting.
 * <p>
 * If an {@code onStop} method errors, the error will be logged and then the next service invoked.
 * That is, a failed {@code onStop} method is not considered fatal.
 * <p>
 * When a startup failure occurs, the server effectively shuts down which includes executing all the {@code onStop} methods.
 *
 * @since 1.3
 */
public interface Service {

  /**
   * The name of this service, used for display purposes.
   * <p>
   * The default implementation is to return {@code getClass().getName()}.
   *
   * @return the name of this service, used for display purposes
   */
  default String getName() {
    return this.getClass().getName();
  }

  /**
   * Server startup event.
   * Executed after the root registry and server instance are constructed and before the server begins accepting requests.
   *
   * @param event meta information about the startup event
   * @throws Exception any
   */
  @NonBlocking
  default void onStart(StartEvent event) throws Exception { }

  /**
   * Server stop event.
   * Executed after the root handler stops accepting requests and before the server closes the channel and thread pool.
   *
   * @param event meta information about the stop event
   * @throws Exception any
   */
  @NonBlocking
  default void onStop(StopEvent event) throws Exception { }

  /**
   * Creates a service that executes the given action as the {@link #onStart(StartEvent)} implementation.
   * <p>
   * This can be used to create a service implementation from a lambda expression,
   * instead of creating an anonymous impl of {@code Service}.
   *
   * @param name the name of the service
   * @param action the action to execute on start
   * @return the service implementation
   * @since 1.4
   */
  static Service startup(String name, Action<? super StartEvent> action) {
    return new Service() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public void onStart(StartEvent event) throws Exception {
        action.execute(event);
      }
    };
  }

  /**
   * Creates a service that executes the given action as the {@link #onStop(StopEvent)} implementation.
   * <p>
   * This can be used to of a service implementation from a lambda expression,
   * instead of creating an anonymous impl of {@code Service}.
   *
   * @param name the name of the service
   * @param action the action to execute on stop
   * @return the service implementation
   * @since 1.4
   */
  static Service shutdown(String name, Action<? super StopEvent> action) {
    return new Service() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public void onStop(StopEvent event) throws Exception {
        action.execute(event);
      }
    };
  }
}
