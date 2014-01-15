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
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.rx.internal.DefaultRxBackground;
import ratpack.rx.internal.RatpackRxErrorHandler;
import rx.plugins.RxJavaPlugins;

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

    RatpackRxErrorHandler.contextProvider = injector.getInstance(LaunchConfig.class).getContextProvider();
    Rx.setRxBackground(injector.getInstance(RxBackground.class));

    return handler;
  }

}
