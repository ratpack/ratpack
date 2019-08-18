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

import java.lang.annotation.*;

/**
 * Declares the other service types that services of the annotated type depend on.
 * <p>
 * This annotation is only effective when present on {@link Service} types.
 * <pre class="java">{@code
 * import ratpack.server.ServerConfig;
 * import ratpack.server.RatpackServer;
 * import ratpack.service.DependsOn;
 * import ratpack.service.Service;
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
 *   private static class ServiceOne implements Service {
 *     {@literal @}Override
 *     public void onStart(StartEvent event) throws Exception {
 *       EVENTS.add("one-start");
 *     }
 *
 *     {@literal @}Override
 *     public void onStop(StopEvent event) throws Exception {
 *       EVENTS.add("one-stop");
 *     }
 *   }
 *
 *   {@literal @}DependsOn(ServiceOne.class)
 *   private static class ServiceTwo implements Service {
 *     {@literal @}Override
 *     public void onStart(StartEvent event) throws Exception {
 *       EVENTS.add("two-start");
 *     }
 *
 *     {@literal @}Override
 *     public void onStop(StopEvent event) throws Exception {
 *       EVENTS.add("two-stop");
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     RatpackServer server = RatpackServer.of(s -> s
 *       .serverConfig(ServerConfig.embedded())
 *       .registryOf(r -> r
 *         // note: order of registration is irrelevant here
 *         .add(new ServiceOne())
 *         .add(new ServiceTwo())
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
 *
 * @see ServiceDependencies
 * @since 1.3
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface DependsOn {

  /**
   * The types of services that services of the annotated type depend on.
   *
   * @return the types of services that services of the annotated type depend on.
   */
  Class<?>[] value();

}
