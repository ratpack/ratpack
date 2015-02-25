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

import ratpack.exec.ExecControl;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;

/**
 * Intercept execution and add support for <a href="http://www.slf4j.org/api/org/slf4j/MDC.html">Slf4j MDC</a> described in the <a href="http://www.slf4j.org/manual.html#mdc">manual</a>.
 * <p>
 * Mapped Diagnostic Context (MDC) is a map of key-value pairs provided by client code and then automatically inserted in log messages.
 * The underlying logging framework has to support MDC logging.
 * Example {@code log4j2} configuration that assumes {@code value} as MDC context map value:
 * {@code
 * <Console name="Console" target="SYSTEM_OUT">
 *   <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg - [%X{value}] %n"/>
 *  </Console>
 * }
 * <p>
 * When interceptor is registered with {@link ratpack.exec.ExecControl#addInterceptor}, it is possible to directly use Slf4j MDC API.
 * The {@link ratpack.exec.Execution}, as registry, maintains own map of MDC entries.
 * Before running the continuation, it checks if internal map contains any entry.
 * If yes, it puts all of them from internal map to MDC (bounded to current thread).
 * If internal map is empty, it clears MDC.
 * After continuation it gets the entries from MDC's context map and stores them in execution's internal map.
 * <pre class="java">{@code
 * import java.util.List;
 * import java.util.ArrayList;
 * import ratpack.test.handling.RequestFixture;
 * import ratpack.test.handling.HandlingResult;
 * import org.slf4j.MDC;
 *
 * import static org.junit.Assert.assertTrue;
 *
 * import ratpack.logging.MDCInterceptor;
 *
 * public class MDCInterceptorExample {
 *   public static void main(String[] args) throws Exception {
 *     List<String> values = new ArrayList<String>();
 *     HandlingResult result = RequestFixture.requestFixture().handleChain(chain -> {
 *       chain
 *          .handler(ctx ->
 *            ctx.addInterceptor(new MDCInterceptor(), ctx::next)
 *          )
 *          .handler(ctx -> {
 *            MDC.put("value", "foo");
 *            values.add(MDC.get("value"));
 *            ctx.blocking(() -> {
 *              values.add(MDC.get("value"));
 *              return "bar3";
 *            }).then(str -> {
 *              values.add(MDC.get("value"));
 *              ctx.render(str);
 *            });
 *          });
 *     });
 *
 *     assertTrue(values.size() == 3);
 *   }
 * }
 * }</pre>
 *
 * @see ratpack.exec.ExecControl#addInterceptor(ratpack.exec.ExecInterceptor, ratpack.func.NoArgAction)
 */
public class MDCInterceptor implements ExecInterceptor {

  public static class MDCMap extends HashMap<String, String> {
  }

  private final Execution current;

  public MDCInterceptor() {
    current = ExecControl.current().getExecution();
    if (!current.maybeGet(MDCMap.class).isPresent()) {
      current.add(new MDCMap());
    }
  }

  public void intercept(ExecInterceptor.ExecType type, Runnable continuation) {
    MDCMap map = current.get(MDCMap.class);
    if (map != null && map.size() > 0) {
      MDC.setContextMap(map);
    } else {
      MDC.clear();
    }

    continuation.run();

    if (map != null) {
      map.clear();
      @SuppressWarnings(value = "unchecked")
      Map<String, String> ctxMap = (Map<String, String>)MDC.getCopyOfContextMap();
      if (ctxMap != null && ctxMap.size() > 0) {
        map.putAll(ctxMap);
        MDC.clear();
      }
    }
  }
}
