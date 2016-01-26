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

package ratpack.guice;

import ratpack.func.Action;
import ratpack.impose.Imposition;
import ratpack.impose.Impositions;

/**
 * An {@link Imposition imposition} of additional and overridden Guice bindings, used by {@link Guice#registry(Action)} and similar methods.
 * <p>
 * This is typically used to override bindings at test time.
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.guice.BindingsImposition;
 * import ratpack.guice.Guice;
 * import ratpack.impose.ImpositionsSpec;
 * import ratpack.test.MainClassApplicationUnderTest;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   public static class App {
 *     public static void main(String[] args) throws Exception {
 *       RatpackServer.start(s -> s
 *         .registry(Guice.registry(r ->
 *           r.bindInstance(String.class, "foo")
 *         ))
 *         .handlers(c -> c
 *           .get(ctx -> ctx.render(ctx.get(String.class)))
 *         )
 *       );
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     new MainClassApplicationUnderTest(App.class) {
 *       {@literal @}Override
 *       protected void addImpositions(ImpositionsSpec impositions) {
 *         impositions.add(BindingsImposition.of(r ->
 *           r.bindInstance(String.class, "bar")
 *         ));
 *       }
 *     }.test(testHttpClient ->
 *       assertEquals("bar", testHttpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 *
 * @since 1.2
 * @see Impositions
 */
public final class BindingsImposition implements Imposition {

  private final Action<? super BindingsSpec> bindings;

  private BindingsImposition(Action<? super BindingsSpec> bindings) {
    this.bindings = bindings;
  }

  /**
   * The bindings to add/override.
   *
   * @return the bindings to add/override
   */
  public Action<? super BindingsSpec> getBindings() {
    return bindings;
  }

  /**
   * Creates a new bindings imposition of the given bindings.
   *
   * @param bindings the bindings to impose
   * @return a new bindings imposition
   */
  public static BindingsImposition of(Action<? super BindingsSpec> bindings) {
    return new BindingsImposition(bindings);
  }

}
