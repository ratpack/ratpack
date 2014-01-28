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

package ratpack.codahale.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Gauge;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of TypeListener that collects {@link com.codahale.metrics.Gauge} metrics for any valid method
 * annotated with {@link Gauge}.
 * <p>
 * Valid methods are those that have no parameters and a void return type.
 */
public class GaugeTypeListener implements TypeListener {

  private final static Logger LOGGER = Logger.getLogger(GaugeTypeListener.class.getName());
  private final MetricRegistry metricRegistry;

  public GaugeTypeListener(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  @Override
  public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
    for (final Method method : type.getRawType().getMethods()) {
      final Gauge annotation = method.getAnnotation(Gauge.class);

      if (annotation != null) {
        boolean validMethod = true;

        if (method.getParameterTypes().length != 0) {
          validMethod = false;
          encounter.addError("Method %s is annotated with @Gauge but requires parameters.", method);
        }

        if (method.getReturnType().equals(Void.TYPE)) {
          validMethod = false;
          encounter.addError("Method %s is annotated with @Gauge but has a void return type.", method);
        }

        if (validMethod) {
          final String gaugeTag = buildGaugeTag(method.getAnnotation(Gauge.class), method);
          encounter.register(new GaugeInjectionListener<>(gaugeTag, method));
        }
      }
    }
  }

  private String buildGaugeTag(Gauge annotation, Method method) {
    if (annotation.name().isEmpty()) {
      return MetricRegistry.name(method.getDeclaringClass(), method.getName());
    }

    if (annotation.absolute()) {
      return annotation.name();
    }

    return MetricRegistry.name(method.getDeclaringClass(), annotation.name());
  }

  private class GaugeInjectionListener<I> implements InjectionListener<I> {

    private final String gaugeTag;
    private final Method method;

    GaugeInjectionListener(String gaugeTag, Method method) {
      this.gaugeTag = gaugeTag;
      this.method = method;
    }

    @Override
    public void afterInjection(final I injectee) {
      if (!metricRegistry.getMetrics().containsKey(gaugeTag)) {
        metricRegistry.register(gaugeTag, new com.codahale.metrics.Gauge<Object>() {

          @Override
          public Object getValue() {
            try {
              return method.invoke(injectee);
            } catch (Exception e) {
              return new RuntimeException(e);
            }
          }
        });
      } else {
        LOGGER.log(Level.WARNING, "Cannot register metric gauge, already registered: " + gaugeTag);
      }
    }

  }
}

