/*
 * Copyright 2013 the original author or authors.
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

package ratpack.guice;

import com.google.inject.Injector;
import com.google.inject.Module;
import ratpack.handling.Handler;

/**
 * A Guice module that contributes to the default handler setup.
 * <p>
 * Modules can implement this interface to decorate the application handler <b>before</b> user handlers.
 * <p>
 * The following example adds a global logging handler so that all requests are logged.
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.guice.*;
 * import com.google.inject.AbstractModule;
 * import com.google.inject.Injector;
 *
 * // A service interface
 * interface Logger {
 *   void log(String str);
 * }
 *
 * // A service impl
 * class LoggerImpl implements Logger {
 *   void log(String str) {
 *     System.out.println(str);
 *   }
 * }
 *
 * // A handler that uses the service, and delegates
 * class LoggingHandler implements Handler {
 *   private final Handler rest;
 *   private final Logger logger;
 *
 *   public LoggingHandler(Logger logger, Handler rest) {
 *     this.logger = logger;
 *     this.rest = rest;
 *   }
 *
 *   void handle(Context exchange) {
 *     logger.log("Request: " + exchange.getRequest().getPath());
 *     rest.handle(exchange);
 *   }
 * }
 *
 * // A module that binds the service impl, and decorates the application handler
 * class LoggingModule extends AbstractModule implements HandlerDecoratingModule {
 *   protected void configure() {
 *     bind(Logger.class).to(LoggerImpl.class);
 *   }
 *
 *   public Handler decorate(Injector injector, Handler handler) {
 *     return new LoggingHandler(injector.getInstance(Logger.class), handler);
 *   }
 * }
 * </pre>
 *
 * @see ratpack.guice.Guice#handler(ratpack.launch.LaunchConfig, ratpack.util.Action, ratpack.util.Action)
 */
public interface HandlerDecoratingModule extends Module {

  /**
   * Decorate the given handler with any <i>global</i> logic.
   *
   * @param injector The injector created from all the application modules
   * @param handler The application handler
   * @return A new handler that decorates the given handler
   */
  Handler decorate(Injector injector, Handler handler);

}
