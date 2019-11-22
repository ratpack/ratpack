package ratpack.micrometer.metrics.internal;

import com.codahale.metrics.annotation.Gauge;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static ratpack.micrometer.metrics.internal.TagUtils.exception;

/**
 * An implementation of TypeListener that collects {@link io.micrometer.core.instrument.Gauge} metrics for any valid method
 * annotated with {@link Gauge}.
 * <p>
 * Valid methods are those that have no parameters and a void return type.
 */
public class DropwizardGaugeTypeListener implements TypeListener {
  @Inject
  MeterRegistry meterRegistry;

  @Override
  public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
    for (final Method method : type.getRawType().getMethods()) {
      final Gauge gauge = method.getAnnotation(Gauge.class);

      if (gauge != null) {
        boolean validMethod = true;

        if (method.getParameterTypes().length != 0) {
          validMethod = false;
          encounter.addError("Method %s is annotated with @Gauge but requires an empty parameter list.", method);
        }

        // Even though Micrometer gauges are numeric only, we are exactly as permissive about return type
        // here as the Dropwizard module. Later, when invoking the method, we can return NaN if the
        // return type is non-numeric.
        if (method.getReturnType().equals(Void.TYPE)) {
          validMethod = false;
          encounter.addError("Method %s is annotated with @Gauge but has a void return type.", method);
        }

        if (validMethod) {
          encounter.register(new GaugeInjectionListener<>(gauge, method));
        }
      }
    }
  }

  private class GaugeInjectionListener<I> implements InjectionListener<I> {
    private final Gauge gauge;
    private final Method method;

    GaugeInjectionListener(Gauge gauge, Method method) {
      this.gauge = gauge;
      this.method = method;
    }

    @Override
    public void afterInjection(I injectee) {
      String gaugeName = gauge.name().isEmpty() ? method.getDeclaringClass().getName() + "." + method.getName() :
        gauge.name();
      Tags gaugeTags = gauge.name().isEmpty() ? Tags.of("method", method.getName(), "class", method.getDeclaringClass().getName()) :
        Tags.empty();

      meterRegistry.gauge(gaugeName, gaugeTags, method, m -> {
        try {
          Object invoked = method.invoke(injectee);
          if(invoked instanceof Number) {
            return ((Number) invoked).doubleValue();
          }
          return Double.NaN;
        } catch (IllegalAccessException | InvocationTargetException e) {
          return Double.NaN;
        }
      });
    }
  }
}
