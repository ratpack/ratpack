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
* import org.slf4j.Logger;
* import org.slf4j.LoggerFactory;
*
* // A service interface
* interface LoggerI {
*   void log(String str);
* }
*
* // A service impl
* class LoggerImpl implements LoggerI {
*   private final static Logger LOGGER = LoggerFactory.getLogger(LoggerImpl.class);
*
*   public void log(String str) {
*     LOGGER.info(str);
*   }
* }
*
* // A handler that uses the service, and delegates
* class LoggingHandler implements Handler {
*   private final Handler rest;
*   private final LoggerI loggeri;
*
*   public LoggingHandler(LoggerI logger, Handler rest) {
*     this.loggeri = logger;
*     this.rest = rest;
*   }
*
*   public void handle(Context exchange) throws Exception {
*     loggeri.log("Request: " + exchange.getRequest().getPath());
*     rest.handle(exchange);
*   }
* }
*
* // A module that binds the service impl, and decorates the application handler
* class LoggingModule extends AbstractModule implements HandlerDecoratingModule {
*   protected void configure() {
*     bind(LoggerI.class).to(LoggerImpl.class);
*   }
*
*   public Handler decorate(Injector injector, Handler handler) {
*     return new LoggingHandler(injector.getInstance(LoggerI.class), handler);
*   }
* }
* </pre>
*
* @see ratpack.guice.Guice#builder(ratpack.registry.Registry)
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
