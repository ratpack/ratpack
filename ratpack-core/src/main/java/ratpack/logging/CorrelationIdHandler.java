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

import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Random;
import java.util.UUID;

/**
 * A Handler that adds a correlation UUID to all requests. This is useful for logging in order
 * to reconstruct.
 *
 * <p>
 * This handler should decorate the existing handler chain
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.guice.*;
 * import com.google.inject.AbstractModule;
 * import com.google.inject.Injector;
 * import ratpack.logging.CorrelationIdHandler;
 *
 * // For logging all requests
 * class MyModule extends AbstractModule implements HandlerDecoratingModule {
 *
 *  protected void configure() {
 *  }
 *
 *  public Handler decorate(Injector injector, Handler handler) {
 *    return new CorrelationIdHandler(handler);
 *  }
 * }
 *
 * </pre>
 */
public class CorrelationIdHandler implements Handler {

  private final Handler rest;
  private final Random r;

  public CorrelationIdHandler(Handler rest) {
    this.r = new Random();
    this.rest = rest;
  }

  @Override
  public void handle(final Context context) throws Exception {
    context.getRequest().addLazy(RequestCorrelationId.class, () -> new RequestCorrelationId(new UUID(r.nextLong(), r.nextLong()).toString()));
    context.insert(rest);
  }
}