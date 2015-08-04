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

import ratpack.handling.internal.DefaultRequestId;
import ratpack.http.Request;

/**
 * An opaque identifier for the request.
 * <p>
 * The request ID can then be obtained from the registry and used in response headers or logging.
 * <p>
 * Out of the box, a request ID is available for all requests.
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
 *         ctx.getResponse().getHeaders().add("X-Request-Id", ctx.get(RequestId.class));
 *         ctx.render("ok");
 *       })
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertEquals("ok", response.getBody().getText());
 *
 *       // Default request ID generator generates random UUIDs (i.e. 36 chars long)
 *       assertEquals(36, response.getHeaders().get("X-Request-Id").length());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * The default ID {@link Generator} uses random UUIDs.
 * <p>
 * To use an alternative strategy, provide an implementation of the {@link Generator} interface in the application's registry.
 * Please note, adding an implementation to the request or context registries will have no effect.
 * The generator is always obtained from the server registry.
 *
 * @see Generator
 */
public interface RequestId extends CharSequence {

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
   * Generates a unique request ID.
   * <p>
   * A default implementation is provided uses random UUIDs as the ID value.
   */
  interface Generator {

    /**
     * Generate a request ID with a “unique” ID value.
     *
     * @param request the request
     * @return a unique request ID
     */
    RequestId generate(Request request);

  }

}
