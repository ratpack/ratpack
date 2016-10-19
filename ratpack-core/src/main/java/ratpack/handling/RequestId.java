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

import com.google.common.reflect.TypeToken;
import ratpack.handling.internal.DefaultRequestId;
import ratpack.handling.internal.HeaderBasedRequestIdGenerator;
import ratpack.handling.internal.UuidBasedRequestIdGenerator;
import ratpack.http.Request;
import ratpack.util.Types;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An opaque identifier for the request.
 * <p>
 * The request ID can then be obtained from the registry and used in response headers or logging.
 * A request ID is always available.
 * <p>
 * The value is determined by the {@link Generator} present in the server registry.
 * By default, a {@link Generator#randomUuid() random UUID value is used}.
 * <p>
 * The following example demonstrates a custom request ID strategy using an incrementing long.
 *
 * <pre class="java">{@code
 * import ratpack.handling.RequestId;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(chain -> chain
 *       .all(ctx -> {
 *         ctx.getResponse().getHeaders().add("X-Request-ID", ctx.get(RequestId.class));
 *         ctx.render("ok");
 *       })
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertEquals("ok", response.getBody().getText());
 *
 *       // Default request ID generator generates random UUIDs (i.e. 36 chars long)
 *       assertEquals(36, response.getHeaders().get("X-Request-ID").length());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * Please note, adding an implementation to the request or context registries will have no effect.
 * The generator is always obtained from the server registry.
 *
 * @see Generator
 */
public interface RequestId extends CharSequence {

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<RequestId> TYPE = Types.token(RequestId.class);

  /**
   * Creates a new request ID from the given string.
   *
   * @param requestId the string of the request id
   * @return a new request id
   */
  static RequestId of(CharSequence requestId) {
    return new DefaultRequestId(requestId);
  }

  /**
   * Generates, or assigns, an ID for requests.
   *
   * <pre class="java">{@code
   * import ratpack.handling.RequestId;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.concurrent.atomic.AtomicLong;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     AtomicLong counter = new AtomicLong();
   *     EmbeddedApp.of(s -> s
   *         .registryOf(r -> r
   *             .add(RequestId.Generator.class, request -> RequestId.of(Long.toString(counter.incrementAndGet())))
   *         )
   *         .handlers(c -> c
   *             .get(ctx ->
   *                 ctx.render("ID: " + ctx.get(RequestId.class))
   *             )
   *         )
   *     ).test(http -> {
   *       assertEquals(http.getText(), "ID: 1");
   *       assertEquals(http.getText(), "ID: 2");
   *     });
   *   }
   * }
   * }</pre>
   *
   * @see #randomUuid()
   * @see #header(CharSequence)
   */
  interface Generator {

    /**
     * A type token for this type.
     *
     * @since 1.1
     */
    TypeToken<Generator> TYPE = Types.token(Generator.class);

    /**
     * Generates IDs based of a random UUID.
     * <p>
     * This strategy is installed into the server registry.
     * It is used if no other strategy is provided.
     * <p>
     * Internally {@link ThreadLocalRandom#current()} is used to produce values.
     * Please consult its documentation for the nature of the randomness of the UUIDs.
     *
     * @return a request id generator
     */
    static Generator randomUuid() {
      return UuidBasedRequestIdGenerator.INSTANCE;
    }

    /**
     * Creates a generator that uses the value for the given header, falling back to a {@link #randomUuid()} generator if the header is not present.
     * <p>
     * This strategy is particularly useful in any kind of distributed environment, where a logical ID for the work is generated (or known) by the thing making the request.
     * This applies to cloud environments like <a href="https://www.heroku.com/home">Heroku</a>, where the edge router <a href="https://devcenter.heroku.com/articles/http-request-id">assigns a request ID</a>.
     *
     * <pre class="java">{@code
     * import ratpack.handling.RequestId;
     * import ratpack.test.embed.EmbeddedApp;
     *
     * import static org.junit.Assert.assertEquals;
     *
     * public class Example {
     *   public static void main(String... args) throws Exception {
     *     EmbeddedApp.of(s -> s
     *         .registryOf(r -> r
     *             .add(RequestId.Generator.header("X-Request-ID"))
     *         )
     *         .handlers(c -> c
     *             .get(ctx ->
     *                 ctx.render("ID: " + ctx.get(RequestId.class))
     *             )
     *         )
     *     ).test(http -> {
     *       http.requestSpec(r -> r.getHeaders().add("X-Request-ID", "foo"));
     *       assertEquals(http.getText(), "ID: foo");
     *     });
     *   }
     * }
     * }</pre>
     *
     * @param headerName the name of the header containing the request ID
     * @return a request ID generator
     * @see #header(CharSequence, RequestId.Generator)
     */
    static Generator header(CharSequence headerName) {
      return new HeaderBasedRequestIdGenerator(headerName, randomUuid());
    }

    /**
     * Creates a generator that uses the value for the given header, using the given fallback generator if the header is not present.
     *
     * @param headerName the name of the header containing the request ID
     * @param fallback the generator to use if the header is not present
     * @return a request ID generator
     * @see #header(CharSequence)
     */
    static Generator header(CharSequence headerName, Generator fallback) {
      return new HeaderBasedRequestIdGenerator(headerName, fallback);
    }

    /**
     * Generate the ID for the request.
     *
     * @param request the request
     * @return a request ID
     */
    RequestId generate(Request request);

  }

}
