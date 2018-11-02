/*
 * Copyright 2018 the original author or authors.
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
package ratpack.micrometer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import ratpack.handling.HandlerDecorator;
import ratpack.micrometer.internal.DefaultRequestTimingHandler;

import static ratpack.handling.Handlers.chain;

public class PrometheusHandlerModule extends AbstractModule {

  @Override
  public void configure() {
    bind(PrometheusHandler.class);
    bind(RequestTimingHandler.class).to(DefaultRequestTimingHandler.class);

    Multibinder.newSetBinder(binder(), HandlerDecorator.class)
      .addBinding()
      .toInstance((registry, rest) -> chain(chain(registry, chain ->
        chain.get("metrics", PrometheusHandler.class))));
  }
}

