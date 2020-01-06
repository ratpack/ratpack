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

package ratpack.micrometer.metrics.internal;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static ratpack.micrometer.metrics.internal.TagUtils.exception;

/**
 * An implementation of MethodInterceptor that collects {@link io.micrometer.core.instrument.Timer} metrics for any method
 * annotated with {@link Timed} or {@link Metered}.
 */
public class DropwizardAnnotationMethodInterceptor implements MethodInterceptor {
  private MeterRegistry meterRegistry;

  @Inject
  public void setMeterRegistry(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    Object result;
    Method method = invocation.getMethod();
    String annotationName = annotationName(invocation);

    Function<Exception, Timer> timer = e ->
      annotationName.isEmpty() ?
        meterRegistry.timer(method.getDeclaringClass().getName() + "." + method.getName(),
          Tags.of(exception(e))) :
        meterRegistry.timer(annotationName,
          Tags.of(
            Tag.of("method", method.getName()),
            Tag.of("class", method.getDeclaringClass().getName()),
            exception(e)
          )
        );

    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      result = invocation.proceed();
      if (result instanceof Promise<?>) {
        result = ((Promise<?>) result).time(duration ->
          timer.apply(null).record(duration.getNano(), TimeUnit.NANOSECONDS)
        );
      } else {
        sample.stop(timer.apply(null));
      }
    } catch (Exception e) {
      sample.stop(timer.apply(e));
      throw e;
    }
    return result;
  }

  private String annotationName(MethodInvocation invocation) {
    String annotatedName = "";
    Timed timed = invocation.getMethod().getAnnotation(Timed.class);
    if (timed != null) {
      annotatedName = timed.name();
    }
    if (timed == null || annotatedName.isEmpty()) {
      Metered metered = invocation.getMethod().getAnnotation(Metered.class);
      if (metered != null) {
        annotatedName = metered.name();
      }
    }
    return annotatedName;
  }
}
