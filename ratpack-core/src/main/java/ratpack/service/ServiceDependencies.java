/*
 * Copyright 2016 the original author or authors.
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

/**
 * Specifies dependencies between services.
 * <p>
 * When starting a server, Ratpack will extract all instances of {@link ServiceDependencies} from the server registry.
 * Each will be called with a {@link ServiceDependenciesSpec spec} that they can use to define the dependencies between services.
 * <p>
 * Services are guaranteed to start after and stop before their dependencies.
 * Services that do not have a dependency relationship may start and stop concurrently.
 * If a depended on service fails to start, the dependent service will not be started.
 * <p>
 * <pre class="java">{@code
 * import ratpack.server.ServerConfig;
 * import ratpack.server.RatpackServer;
 * import ratpack.service.Service;
 * import ratpack.service.ServiceDependencies;
 * import ratpack.service.StartEvent;
 * import ratpack.service.StopEvent;
 *
 * import java.util.ArrayList;
 * import java.util.List;
 *
 * import static java.util.Arrays.asList;
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   private static final List<String> EVENTS = new ArrayList<>();
 *
 *   private static class MyService implements Service {
 *
 *     private final String label;
 *
 *     public MyService(String label) {
 *       this.label = label;
 *     }
 *
 *     public String getLabel() {
 *       return label;
 *     }
 *
 *     {@literal @}Override
 *     public void onStart(StartEvent event) throws Exception {
 *       EVENTS.add(label + "-start");
 *     }
 *
 *     {@literal @}Override
 *     public void onStop(StopEvent event) throws Exception {
 *       EVENTS.add(label + "-stop");
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     RatpackServer server = RatpackServer.of(s -> s
 *       .serverConfig(ServerConfig.embedded())
 *       .registryOf(r -> r
 *         .add(new MyService("one"))
 *         .add(new MyService("two"))
 *         // service two depends on service one
 *         .add(ServiceDependencies.class, d -> d.dependsOn(
 *           MyService.class, service -> service.getLabel().equals("two"),
 *           MyService.class, service -> service.getLabel().equals("one")
 *         ))
 *       )
 *     );
 *
 *     server.start();
 *     assertEquals(asList("one-start", "two-start"), EVENTS);
 *
 *     server.stop();
 *     assertEquals(asList("one-start", "two-start", "two-stop", "one-stop"), EVENTS);
 *   }
 * }
 * }</pre>
 * <p>
 * If dependencies between services can be declared purely via types, consider using the {@link DependsOn} annotation instead which is more concise yet equivalent.
 *
 * @see DependsOn
 * @see ServiceDependenciesSpec
 * @since 1.3
 */
public interface ServiceDependencies {

  /**
   * Declares service depenencies via the given spec.
   *
   * @param spec the spec of service dependencies
   * @throws Exception any
   */
  void define(ServiceDependenciesSpec spec) throws Exception;

}
