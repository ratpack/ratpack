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

import ratpack.http.Request;
import ratpack.http.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A {@link Handler handler}, that adds a {@value #HEADER_NAME} header to all requests indicating how long it took to start sending a response in milliseconds.
 * <p>
 * It is generally most convenient to add a timer into the handler chain by using the {@link #decorator()} method, which provides a {@link ratpack.handling.HandlerDecorator}.
 * <pre class="java">{@code
 * import ratpack.handling.ResponseTimer;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.http.client.ReceivedResponse;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registryOf(r -> r
 *         .add(ResponseTimer.decorator())
 *       )
 *       .handler(r ->
 *         ctx -> ctx.render("ok")
 *       )
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertNotNull(response.getHeaders().get("X-Response-Time"));
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * See {@link #handle(Context)} for precise detail on what is timed, and the time value.
 */
public class ResponseTimer implements Handler {

  private static final BigDecimal NANOS_IN_MILLIS = BigDecimal.valueOf(ChronoUnit.MILLIS.getDuration().toNanos());

  /**
   * The name of the header with the time value: {@value}.
   */
  public static final String HEADER_NAME = "X-Response-Time";

  /**
   * Creates a handler decorator that prepends a response timer to the rest of the handlers.
   *
   * @return a handler decorator.
   */
  public static HandlerDecorator decorator() {
    return HandlerDecorator.prepend(new ResponseTimer());
  }

  /**
   * Adds the number of milliseconds of elapsed time between {@link Request#getTimestamp()} and when the response is ready to be sent.
   * <p>
   * The timer is stopped, and the header added, by {@link Response#beforeSend(ratpack.func.Action)}.
   * This means that the time value is the elapsed time, commonly referred to as wall clock time, and not CPU time.
   * Similarly, it does not include the time to actually start sending data out over the socket.
   * It effectively times the application processing.
   * <p>
   * The value is in milliseconds, accurate to 5 decimal places.
   *
   * @param ctx the handling context.
   */
  @Override
  public void handle(Context ctx) {
    Response response = ctx.getResponse();
    response.beforeSend(m -> {
      Clock clock = ctx.get(Clock.class);
      Instant start = ctx.getRequest().getTimestamp();
      long nanos = start.until(Instant.now(clock), ChronoUnit.NANOS);
      BigDecimal diffNanos = new BigDecimal(nanos);
      BigDecimal diffMillis = diffNanos.divide(NANOS_IN_MILLIS, 5, RoundingMode.UP);
      m.getHeaders().set(HEADER_NAME, diffMillis.toString());
    });
    ctx.next();
  }
}
