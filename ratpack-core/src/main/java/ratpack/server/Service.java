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

package ratpack.server;

import ratpack.api.NonBlocking;

/**
 * A service participates in the application lifecycle.
 * <p>
 * When the application starts, all services in the server registry will be notified.
 * Similarly when the application stops.
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.server.Service;
 * import ratpack.server.StartEvent;
 * import ratpack.server.StopEvent;
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
 * <h3>Execution order</h3>
 * <p>
 * Services are notified of start events in the order returned by the {@link ratpack.registry.Registry#getAll} method.
 * They are notified in reverse order for stop events.
 * The order in that services are returned from the server registry is dependent on the registry implementation.
 * Generally, most implementations return objects in the order in which they are added to the registry.
 *
 * <h3>Async services</h3>
 * <p>
 * The {@link #onStart} and {@link #onStop} methods are always executed within a distinct {@link ratpack.exec.Execution}, for each service.
 * This means that implementations of these methods are free to perform async ops (e.g. use the {@link ratpack.http.client.HttpClient}, or {@link ratpack.exec.ExecControl#blocking block}).
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.server.Service;
 * import ratpack.server.StartEvent;
 * import ratpack.server.StopEvent;
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
 *       event.getExecControl().promiseOf("start").map(String::toUpperCase).then(events::add);
 *     }
 *
 *     public void onStop(StopEvent event) {
 *       event.getExecControl().promiseOf("stop").map(String::toUpperCase).then(events::add);
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
 * <h3>Dependencies</h3>
 * <p>
 * The event objects given to the start/stop methods provide access to the server registry.
 * This can be used, for example, to get hold of a database connection that was added to the server registry as part of the server definition.
 * <p>
 * Alternatively, when using the Guice support the service can be injected by Guice.
 * <pre class="java"><code>
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.server.Service;
 * import ratpack.server.StartEvent;
 * import ratpack.server.StopEvent;
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
 *     public final {@code List<String>} events;
 *
 *     {@literal @}Inject
 *     public RecordingService({@code List<String>} events) {
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
 *     {@code List<String>} list = new {@code LinkedList<>()};
 *
 *     RatpackServer server = RatpackServer.of(s {@code ->} s
 *       .serverConfig(ServerConfig.embedded())
 *       .registry(Guice.registry(b {@code ->} b
 *         .bindInstance(Types.listOf(String.class), list)
 *         .bind(RecordingService.class)
 *       ))
 *       .handler(r {@code ->} ctx {@code ->} ctx.render("ok"))
 *     );
 *
 *     server.start();
 *     assertEquals("[start]", list.toString());
 *     server.stop();
 *     assertEquals("[start, stop]", list.toString());
 *   }
 * }
 * </code></pre>
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
}
