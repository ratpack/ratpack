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
import ratpack.func.Action;
import ratpack.handling.internal.NcsaRequestLogger;

/**
 * A handler that logs information about the request.
 * <p>
 * Implementations need only implement the {@link #log(RequestOutcome)} method.
 * This interface provides a default implementation of {@code handle()} that calls the {@code log()} method before delegating to the next handler.
 * The {@link #of(Action)} can also be used to create a request logger.
 * <p>
 * Unless there is a good reason not to, loggers should log to {@link #LOGGER} at the “info” logging level.
 * How this logging manifests can then be controlled by configuring the logging subsystem in use.
 *
 * <pre class="java">{@code
 * import ratpack.handling.RequestLogger;
 * import ratpack.test.embed.EmbeddedApp;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(c -> c
 *       .all(RequestLogger.ncsa())
 *       .all(ctx -> ctx.render("ok"))
 *     ).test(httpClient -> {
 *       assertEquals("ok", httpClient.get().getBody().getText());
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see #ncsa()
 * @see RequestId
 * @see UserId
 */
public interface RequestLogger extends Handler {

  /**
   * The name of {@link #LOGGER}: {@value}.
   */
  String LOGGER_NAME = "ratpack.request";

  /**
   * The default request logger.
   *
   * @see #LOGGER_NAME
   */
  Logger LOGGER = LoggerFactory.getLogger(LOGGER_NAME);

  /**
   * Creates a request logger with the given action as the implementation of the {@link #log(RequestOutcome)} method.
   *
   * <pre class="java">{@code
   * import ratpack.handling.RequestLogger;
   * import ratpack.test.embed.EmbeddedApp;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.fromHandlers(c -> c
   *       .all(RequestLogger.of(outcome ->
   *         RequestLogger.LOGGER.info(outcome.getRequest().getUri())
   *       ))
   *       .all(ctx -> ctx.render("ok"))
   *     ).test(httpClient -> {
   *       assertEquals("ok", httpClient.get().getBody().getText());
   *     });
   *   }
   * }
   * }</pre>
   *
   * <p>
   * Unless there is reason not to, the action should log at info level to {@link #LOGGER}.
   *
   * @param action an action that logs information about the request
   * @return a request logger implementation
   */
  static RequestLogger of(Action<? super RequestOutcome> action) {
    return action::execute;
  }

  /**
   * Calls {@link #ncsa(Logger)} with {@link #LOGGER}.
   *
   * @return a new request logger
   */
  static RequestLogger ncsa() {
    return ncsa(LOGGER);
  }

  /**
   * Logs in the NCSA Common Log format.
   *
   * The format for the request log is "host rfc931 username date:time request statuscode bytes" as defined by the NCSA Common (access logs) format (see link).
   * However, if the {@link RequestId} is additionally being added to requests, the value of the request Id will be appended to the end of the request log in the form: id=requestId
   * The resulting format is thus: "host rfc931 username date:time request statuscode bytes id=requestId"
   *
   * @param logger the logger to log to, at INFO level
   * @return a new request logger
   * @see <a href="http://publib.boulder.ibm.com/tividd/td/ITWSA/ITWSA_info45/en_US/HTML/guide/c-logs.html#common">NCSA Common Log format documentation.</a>
   */
  static RequestLogger ncsa(Logger logger) {
    return new NcsaRequestLogger(logger);
  }

  /**
   * Format the provided {@link RequestOutcome} to the given string builder.
   *
   * @param outcome the resulting outcome of a received request
   * @throws Exception any
   */
  void log(RequestOutcome outcome) throws Exception;

  /**
   * Adds {@link #log(RequestOutcome)} as a {@link Context#onClose(Action) context close action}, effectively logging the request.
   * <p>
   * The handler calls {@link Context#next()} after adding the context close action.
   *
   * @param ctx the request context
   */
  default void handle(Context ctx) {
    ctx.onClose(this::log);
    ctx.next();
  }

}
