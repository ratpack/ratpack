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

import ratpack.registry.Registry;

/**
 * Informs when the server should be reloaded, during {@link ServerConfig#isDevelopment() development}.
 * <p>
 * During development mode, all reload informants present in the server registry will be asked if the server should reload before serving each request.
 * The term “reload” here specifically refers to rebuilding the server definition by re-<i>executing</i> the function given to the {@link RatpackServer#of(ratpack.func.Action)} method that defined the server.
 * <p>
 * Reload informants will never be queried concurrently so can be safely stateful.
 * Calls to {@link #shouldReload(ratpack.registry.Registry)} are serialised for any given informant, and informants are queried in sequence.
 * <p>
 * Reload informants are queried in the order they are returned by the server registry.
 * If an informant indicates that the server should reload, no further informants will be queried.
 * <p>
 * As reload informants are queried for every request, it is sometimes desirable to internally use some kind of polling technique internally to avoid creating too much overhead.
 * However, implementations do not need to be too performance sensitive as reload informants only apply during development.
 * <p>
 * Reload informants are never queried when not in development mode.
 * It is completely benign for informants to be in the server registry when not in development.
 * <p>
 * Below shows a contrived reload informant that simply asks that the server reload on every other request.
 * <pre class="java">{@code
 * import ratpack.server.ReloadInformant;
 * import ratpack.server.ServerConfig;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.registry.Registry;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   static class ReloadEveryOtherRequest implements ReloadInformant {
 *     private int i = 0;
 *
 *     public boolean shouldReload(Registry registry) {
 *       return ++i % 2 == 0;
 *     }
 *
 *     public String toString() {
 *       return "every other request informant";
 *     }
 *   }
 *
 *   static int counter = 0;
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *         .serverConfig(ServerConfig.embedded().development(true))
 *         .registryOf(r -> r
 *             .add(ReloadInformant.class, new ReloadEveryOtherRequest())
 *             .add(Integer.class, Example.counter++)
 *         )
 *         .handler(r -> ctx -> ctx.render(ctx.get(Integer.class).toString()))
 *     ).test(httpClient -> {
 *       assertEquals("0", httpClient.getText()); // first request never queries informants
 *       assertEquals("1", httpClient.getText()); // reload triggered
 *       assertEquals("1", httpClient.getText());
 *       assertEquals("2", httpClient.getText()); // reload triggered
 *       assertEquals("2", httpClient.getText());
 *     });
 *   }
 * }
 * }</pre>
 */
public interface ReloadInformant {

  /**
   * Whether the server should reload.
   *
   * @return whether the server should reload
   * @param registry the server registry
   */
  boolean shouldReload(Registry registry);

  /**
   * The description of this reload informant.
   * <p>
   * This value will be logged if the informant requests a reload, indicating which informant requested a reload.
   *
   * @return the description of this reload informant
   */
  @Override
  String toString();

}
