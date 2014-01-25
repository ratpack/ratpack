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

package ratpack.rx;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Foreground;
import ratpack.handling.Handler;
import ratpack.rx.internal.DefaultRxBackground;
import ratpack.rx.internal.RatpackRxErrorHandler;
import rx.plugins.RxJavaPlugins;

import java.lang.reflect.Field;

/**
 * Integrates <a href="https://github.com/Netflix/RxJava">RxJava</a> with the Ratpack application.
 * <p>
 * This module <b>MUST</b> be registered with a Ratpack application in order to use the Rx integration.
 * It registers plugins with Rx which must be used.
 * <h4>Provided Types:</h4>
 * <ul>
 * <li>{@link RxBackground} - create observables for background operations.</li>
 * </ul>
 * <h4>Error handling:</h4>
 * <p>
 * When the application starts, this module will register a special error handler with Rx that forwards any unhandled
 * errors to the active thread's context's error handler (via {@link ratpack.handling.Context#error(Exception)}).
 * This means that you do not need to register explicit error handlers for <i>any</i> observable to route to the error handler.
 * <p>
 * If you are observing on a thread that is not managed by Ratpack (i.e. not a request or {@link ratpack.background.Background} thread),
 * this error handler is non effectual as there is no error handler to forward to.
 */
public class RxModule extends AbstractModule implements HandlerDecoratingModule {

  @Override
  protected void configure() {
    bind(RxBackground.class).to(DefaultRxBackground.class);
  }

  @Override
  public Handler decorate(Injector injector, Handler handler) {
    RxJavaPlugins instance = RxJavaPlugins.getInstance();
    try {
      instance.registerErrorHandler(new RatpackRxErrorHandler());
    } catch (IllegalStateException ignore) {
      if (!(instance.getErrorHandler() instanceof RatpackRxErrorHandler)) {
        throw new IllegalStateException("Another error handler has registered with Rx");
      }
    }

    RatpackRxErrorHandler.foreground = injector.getInstance(Foreground.class);
    setRxBackground(injector.getInstance(RxBackground.class));
    return handler;
  }

  private static void setRxBackground(RxBackground rxBackground) {
    try {
      Field field = RxRatpack.class.getDeclaredField("rxBackground");
      field.setAccessible(true);
      field.set(null, rxBackground);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Couldn't set private static field property", e);
    }
  }

}
