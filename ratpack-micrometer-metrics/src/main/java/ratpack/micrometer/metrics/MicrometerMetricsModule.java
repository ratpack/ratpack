/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Provider;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.micrometer.metrics.internal.*;

import static com.google.inject.Scopes.SINGLETON;

public class MicrometerMetricsModule extends ConfigurableModule<MicrometerMetricsConfig> {
  @Override
  protected void configure() {
    install(new MeterRegistryModule());

    bind(RequestTimingHandler.class).toProvider(RequestTimingHandlerProvider.class).in(SINGLETON);
    bind(BlockingExecTimingInterceptor.class).toProvider(BlockingExecTimingInterceptorProvider.class).in(SINGLETON);
    Provider<RequestTimingHandler> handlerProvider = getProvider(RequestTimingHandler.class);
    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toProvider(() -> HandlerDecorator.prepend(handlerProvider.get()));

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(io.micrometer.core.annotation.Timed.class), injected(new TimedAnnotationMethodInterceptor()));
    bindListener(Matchers.any(), injected(new DropwizardGaugeTypeListener()));

    configureDropwizardMigration();
  }

  /**
   * For easy migration from Dropwizard to Micrometer without changing annotation types
   */
  private void configureDropwizardMigration() {
    try {
      bindInterceptor(Matchers.any(), Matchers.annotatedWith(Metered.class), injected(new DropwizardAnnotationMethodInterceptor()));
      bindInterceptor(Matchers.any(), Matchers.annotatedWith(Timed.class), injected(new DropwizardAnnotationMethodInterceptor()));
    } catch(Throwable t) {
      // do nothing
    }
  }

  private <T> T injected(T instance) {
    requestInjection(instance);
    return instance;
  }
}
