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

import org.slf4j.MDC;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.util.Types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An execution interceptor that adds support for Slf4j's <a href="http://www.slf4j.org/manual.html#mdc">Mapped Diagnostic Context (MDC) feature</a>.
 * <p>
 * The MDC is a set of key-value pairs (i.e. map) that can be implicitly added to all logging statements within the <em>context</em>.
 * The term “context” here comes from Slf4j's lexicon and does not refer to Ratpack's {@link ratpack.handling.Context}.
 * It refers to a logical sequence of execution (e.g. handling of a request).
 * Slf4j's default strategy for MDC is based on a thread-per-request model, which doesn't work for Ratpack applications.
 * This interceptor maps Slf4j's notion of a “context” to Ratpack's notion of an {@link ratpack.exec.Execution “execution”}.
 * This means that after installing this interceptor, the {@link MDC MDC API} can be used naturally.
 * <p>
 * Please be sure to read the <a href="http://www.slf4j.org/manual.html#mdc">Slf4j manual section on MDC</a>, particularly about how the actual logging implementation being used must support MDC.
 * If your logging implementation doesn't support MDC (e.g. {@code slf4j-simple}) then all of the methods on the {@link MDC} API become no-ops.
 * <p>
 * The following example shows the registration of the interceptor and MDC API usage.
 * <pre class="java">{@code
 * import java.util.List;
 * import java.util.ArrayList;
 * import ratpack.test.embed.EmbeddedApp;
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
 *     EmbeddedApp.fromHandlers(chain -> chain
 *       .handler(ctx -> ctx.addInterceptor(MDCInterceptor.instance(), ctx::next))
 *       .handler(ctx -> {
 *         // Put a value into the MDC
 *         MDC.put("clientIp", ctx.getRequest().getRemoteAddress().getHostText());
 *         // The logging implementation/configuration may inject values from the MDC into log statements
 *         LOGGER.info("about to block");
 *         ctx.blocking(() -> {
 *           // The MDC is carried across asynchronous boundaries by the interceptor
 *           LOGGER.info("blocking");
 *           return "something";
 *         }).then(str -> {
 *           // And back again
 *           LOGGER.info("back from blocking");
 *           ctx.render("ok");
 *         });
 *       })
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
 * The MDC is not inherited by forked executions (e.g. {@link ratpack.handling.Context#exec()}).
 * If you wish context to be inherited, you must do so explicitly by capturing the variables you wish to be inherited
 * (i.e. via {@link MDC#get(String)}) as local variables and then add them to the MDC (i.e. via {@link MDC#put(String, String)}) in the forked execution.
 *
 * @see ratpack.exec.ExecControl#addInterceptor(ratpack.exec.ExecInterceptor, ratpack.func.NoArgAction)
 */
public class MDCInterceptor implements ExecInterceptor {

  private static final MDCInterceptor INSTANCE = new MDCInterceptor();

  private static class MDCMap extends LinkedHashMap<String, String> {

  }

  public MDCInterceptor() {

  }

  public static MDCInterceptor instance() {
    return INSTANCE;
  }

  public void intercept(Execution execution, ExecType type, Runnable continuation) {
    MDC.clear();

    MDCMap map = execution.maybeGet(MDCMap.class).orElse(null);
    if (map == null) {
      map = new MDCMap();
      execution.add(map);
    } else {
      MDC.setContextMap(map);
    }

    continuation.run();

    map.clear();

    Map<String, String> ctxMap = Types.cast(MDC.getCopyOfContextMap());
    if (ctxMap != null && ctxMap.size() > 0) {
      map.putAll(ctxMap);
      MDC.clear();
    }
  }
}
