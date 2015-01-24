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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.RequestOutcome;
import ratpack.http.Request;
import ratpack.http.SentResponse;

/**
 * A Handler that logs the the completion of every request using the standard
 * request log format.
 *
 * <p>
 * This handler should decorate the existing handler chain
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.guice.*;
 * import com.google.inject.AbstractModule;
 * import com.google.inject.Injector;
 * import ratpack.logging.*;
 *
 * // For logging all requests
 * class MyModule1 extends AbstractModule implements HandlerDecoratingModule {
 *  protected void configure() {
 *  }
 *
 *  public Handler decorate(Injector injector, Handler handler) {
 *    return new RequestLoggingHandler(handler);
 *  }
 * }
 *
 *
 * // For logging all requests with a UUID
 *  class MyModule2 extends AbstractModule implements HandlerDecoratingModule {
 *
 *  protected void configure() {
 *
 *  }
 *
 *  public Handler decorate(Injector injector, Handler handler) {
 *    return new CorrelationIdHandler(new RequestLoggingHandler(handler));
 *  }
 * }
 * </pre>
 */
public class RequestLoggingHandler  implements Handler {
  private final Handler rest;
  private final static Logger LOGGER = LoggerFactory.getLogger(RequestLoggingHandler.class);

  public RequestLoggingHandler(Handler rest) {
    this.rest = rest;
  }

  @Override
  public void handle(Context context) throws Exception {
    Request request = context.getRequest();

    context.onClose((RequestOutcome thing) -> LOGGER.info(getRequestLogEntry(request, thing.getResponse())));
    context.insert(rest);
  }

  private String getRequestLogEntry(Request request, SentResponse response) {
    StringBuilder sb = new StringBuilder();
    sb.append(request.getMethod().toString());
    sb.append(" ");
    sb.append(request.getUri());
    sb.append(" ");
    sb.append(response.getStatus().getCode());

    request.maybeGet(RequestCorrelationId.class).ifPresent(id -> {
      sb.append(" correlationId=");
      sb.append(id.toString());
    });

    return sb.toString();
  }
}
