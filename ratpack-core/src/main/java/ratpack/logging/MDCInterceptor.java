/*
 * Copyright 2014 the original author or authors.
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

package ratpack.logging;

import com.google.common.reflect.TypeToken;
import org.slf4j.MDC;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.util.Types;

import java.util.HashMap;
import java.util.Map;

/**
 * An execution interceptor that adds support for SLF4J's <a href="http://www.slf4j.org/manual.html#mdc">Mapped Diagnostic Context (MDC) feature</a>.
 * <p>
 * The MDC is a set of key-value pairs (i.e. map) that can be implicitly added to all logging statements within the <em>context</em>.
 * The term “context” here comes from SLF4J's lexicon and does not refer to Ratpack's {@link ratpack.handling.Context}.
 * It refers to a logical sequence of execution (e.g. handling of a request).
 * SLF4J's default strategy for MDC is based on a thread-per-request model, which doesn't work for Ratpack applications.
 * This interceptor maps SLF4J's notion of a “context” to Ratpack's notion of an {@link ratpack.exec.Execution “execution”}.
 * This means that after installing this interceptor, the {@link MDC MDC API} can be used naturally.
 * <p>
 * Please be sure to read the <a href="http://www.slf4j.org/manual.html#mdc">SLF4J manual section on MDC</a>, particularly about how the actual logging implementation being used must support MDC.
 * If your logging implementation doesn't support MDC (e.g. {@code slf4j-simple}) then all of the methods on the {@link MDC} API become no-ops.
 * <p>
 * The interceptor should be added to the server registry, so that it automatically is applied to all executions.
 * The following example shows the registration of the interceptor and MDC API usage.
 * <pre class="java">{@code
 * import java.util.List;
 * import java.util.ArrayList;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.exec.Blocking;
 * import org.slf4j.MDC;
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * import ratpack.logging.MDCInterceptor;
 *
 * public class Example {
 *
 *   private static final Logger LOGGER = LoggerFactory.getLogger(Example.class);
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registryOf(r -> r.add(MDCInterceptor.instance()))
 *       .handler(r ->
 *         ctx -> {
 *           // Put a value into the MDC
 *           MDC.put("clientIp", ctx.getRequest().getRemoteAddress().getHost());
 *           // The logging implementation/configuration may inject values from the MDC into log statements
 *           LOGGER.info("about to block");
 *           Blocking.get(() -> {
 *             // The MDC is carried across asynchronous boundaries by the interceptor
 *             LOGGER.info("blocking");
 *             return "something";
 *           }).then(str -> {
 *             // And back again
 *             LOGGER.info("back from blocking");
 *             ctx.render("ok");
 *           });
 *         }
 *       )
 *     ).test(httpClient ->
 *       assertEquals("ok", httpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 * <p>
 * Given the code above, using the Log4j bindings with configuration such as:
 * <pre>{@code <Console name="Console" target="SYSTEM_OUT">
 *   <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg - [%X{clientIp}] %n"/>
 * </Console>}</pre>
 * <p>
 * The client IP address will be appended to all log messages made while processing requests.
 * <h3>Inheritance</h3>
 * <p>
 * The MDC is not inherited by forked executions (e.g. {@link Execution#fork()}).
 * If you wish context to be inherited, you must do so explicitly by capturing the variables you wish to be inherited
 * (i.e. via {@link MDC#get(String)}) as local variables and then add them to the MDC (i.e. via {@link MDC#put(String, String)}) in the forked execution.
 *
 * @see ratpack.exec.ExecInterceptor
 */
public final class MDCInterceptor implements ExecInterceptor {

  private final Action<? super Execution> init;

  private static class MDCHolder {
    static final TypeToken<MDCHolder> TYPE = Types.token(MDCHolder.class);
    Map<String, String> map = new HashMap<>();
  }

  private MDCInterceptor(Action<? super Execution> init) {
    this.init = init;
  }

  /**
   * Creates an interceptor with no initialisation action.
   *
   * @return an interceptor with no initialisation action.
   * @see #withInit(Action)
   */
  public static MDCInterceptor instance() {
    return withInit(Action.noop());
  }

  /**
   * Creates an interceptor with the given initialisation action.
   * <p>
   * The given action will be executed before the first segment of each execution,
   * allowing the MDC to be primed with initial values.
   * <p>
   * The following demonstrates priming the MDC with the {@link ratpack.handling.RequestId}.
   *
   * <pre class="java">{@code
   * import org.slf4j.MDC;
   * import ratpack.handling.RequestId;
   * import ratpack.logging.MDCInterceptor;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(s -> s
   *         .registryOf(r -> r
   *             .add(MDCInterceptor.withInit(e ->
   *                 e.maybeGet(RequestId.class).ifPresent(requestId ->
   *                     MDC.put("requestId", requestId.toString())
   *                 )
   *             ))
   *         )
   *         .handlers(c -> c
   *             .get(ctx -> ctx.render(MDC.get("requestId")))
   *         )
   *     ).test(http ->
   *         // The default request ID generator generates UUIDs (i.e. 36 chars long)
   *         assertEquals(http.getText().length(), 36)
   *     );
   *   }
   * }
   * }</pre>
   * <p>
   * If no initialisation is required, use {@link #instance()}.
   *
   * @param init the initialisation action
   * @return an {@link MDCInterceptor}
   * @since 1.1
   */
  public static MDCInterceptor withInit(Action<? super Execution> init) {
    return new MDCInterceptor(init);
  }

  /**
   *
   * @param execution the execution that this segment belongs to
   * @param execType indicates whether this segment is execution on a compute or blocking thread
   * @param executionSegment the execution segment that is to be executed
   * @throws Exception any
   */
  public void intercept(Execution execution, ExecType execType, Block executionSegment) throws Exception {
    MDCHolder holder = execution.maybeGet(MDCHolder.TYPE).orElse(null);
    if (holder == null) {
      MDC.clear();
      holder = new MDCHolder();
      init.execute(execution);
      execution.add(MDCHolder.TYPE, holder);
    } else {
      if (holder.map == null) {
        MDC.clear();
      } else {
        MDC.setContextMap(holder.map);
      }
    }

    try {
      executionSegment.execute();
    } finally {
      holder.map = MDC.getCopyOfContextMap();
      MDC.clear();
    }
  }
}
