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

/**
 * A service participates in the application lifecycle.
 * <p>
 * This interface denotes a “Ratpack service”, which is something that gets notified of server start and stop.
 * It has no further application level semantics (e.g. the database service).
 * <p>
 * When the application starts, all services in the server registry will be notified.
 * Similarly when the application stops.
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.server.ServerConfig;
 * import ratpack.server.Service;
 * import ratpack.server.StartEvent;
 * import ratpack.server.StopEvent;
 * import ratpack.test.http.TestHttpClient;
 *
 * import java.util.List;
 * import java.util.LinkedList;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   static class RecordingService implements Service {
 *     public List<String> events = new LinkedList<>();
 *     public void onStart(StartEvent startEvent) { events.add("start"); }
 *     public void onStop(StopEvent startEvent) { events.add("stop"); }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     RecordingService service = new RecordingService();
 *
 *     RatpackServer server = RatpackServer.of(s -> s
 *       .config(ServerConfig.embedded())
 *       .registryOf(r -> r.add(service))
 *       .handler(r ->
 *         ctx -> ctx.render("ok")
 *       )
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
 */
public interface Service {

  // TODO define semantics of on* methods throwing exceptions, and test coverage

  /**
   * Server startup event.
   * Executed after the root registry and server instance are constructed and before the server begins accepting requests.
   *
   * @param event meta information about the startup event
   * @throws Exception any
   */
  default void onStart(StartEvent event) throws Exception { }

  /**
   * Server stop event.
   * Executed after the root handler stops accepting requests and before the server closes the channel and thread pool.
   *
   * @param event meta information about the stop event
   * @throws Exception any
   */
  default void onStop(StopEvent event) throws Exception { }
}
