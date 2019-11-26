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
