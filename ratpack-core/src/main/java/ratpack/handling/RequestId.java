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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.internal.UuidBasedRequestIdGenerator;
import ratpack.http.Request;

/**
 * An opaque identifier for the request.
 * <p>
 * The request ID is useful for logging and correlation.
 *
 * @see Generator
 * @see #bind()
 * @see #bindAndLog()
 */
public interface RequestId {


  /**
   * The of the request.
   *
   * @return the id
   */
  String getId();

  /**
   * Generates a unique request ID.
   * <p>
   * A default implementation is provided uses random UUIDs as the ID value.
   * To supply your own implementation, put an implementation of this interface upstream from the {@link #bind() binding handler}.
   */
  public interface Generator {

    /**
     * Generate a request ID with a “unique” ID value.
     *
     * @param context the handling context
     * @return a unique request ID
     */
    RequestId generate(Context context);
  }

  /**
   * Creates a new request ID (using the {@link Generator} from the context) and inserts it into the request registry.
   * <p>
   * The {@code RequestId} can then be obtained from the request registry and used in response headers for example.
   * <pre class="java">{@code
   * import ratpack.handling.RequestId;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.test.embed.EmbeddedApp;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.fromHandlers(chain -> chain
   *       .handler(RequestId.bind())
   *       .handler(ctx -> {
   *         ctx.getResponse().getHeaders().add("X-Request-Id", ctx.getRequest().get(RequestId.class).getId());
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
   * To use a different strategy for generating IDs, put your own implementation of {@link Generator} into the context registry before this handler.
   *
   * @return a handler that generates a request ID and adds it to the request registry
   */
  static Handler bind() {
    return ctx -> {
      Generator generator = ctx.maybeGet(Generator.class).orElse(UuidBasedRequestIdGenerator.INSTANCE);
      RequestId requestId = generator.generate(ctx);
      ctx.getRequest().add(RequestId.class, requestId);
      ctx.next();
    };
  }

  // TODO docs here, including explanation of the log format.
  static Handler bindAndLog() {
    return Handlers.chain(bind(), new Handler() {
      private final Logger logger = LoggerFactory.getLogger(RequestId.class);

      @Override
      public void handle(Context ctx) throws Exception {
        ctx.onClose((RequestOutcome outcome) -> {
          Request request = ctx.getRequest();
          StringBuilder logLine = new StringBuilder()
            .append(request.getMethod().toString())
            .append(" ")
            .append(request.getUri())
            .append(" ")
            .append(outcome.getResponse().getStatus().getCode());

          request.maybeGet(RequestId.class).ifPresent(id1 -> {
            logLine.append(" id=");
            logLine.append(id1.toString());
          });

          logger.info(logLine.toString());
        });
      }
    });
  }

}
