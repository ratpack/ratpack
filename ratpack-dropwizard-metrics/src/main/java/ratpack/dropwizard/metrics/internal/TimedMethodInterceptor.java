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

package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import ratpack.exec.Promise;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of MethodInterceptor that collects {@link com.codahale.metrics.Timer} metrics for any method
 * annotated with {@link com.codahale.metrics.annotation.Timed}.
 */
public class TimedMethodInterceptor implements MethodInterceptor {

  @Inject
  MetricRegistry metricRegistry;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    String timerTag = buildTimerTag(invocation.getMethod().getAnnotation(Timed.class), invocation.getMethod());
    final Timer timer = metricRegistry.timer(timerTag);
    final Timer.Context timerContext = timer.time();
    Object result;
    try {
      result = invocation.proceed();
      if (result instanceof Promise<?>) {
        result = ((Promise<?>) result).time(duration ->
                timer.update(duration.getNano(), TimeUnit.NANOSECONDS)
        );
      } else {
        timerContext.stop();
      }
    } catch (Exception e) {
      timerContext.stop();
      throw e;
    }
    return result;
  }

  private String buildTimerTag(Timed annotation, Method method) {
    if (annotation.name().isEmpty()) {
      return MetricRegistry.name(method.getDeclaringClass(), method.getName());
    }

    if (annotation.absolute()) {
      return annotation.name();
    }

    return MetricRegistry.name(method.getDeclaringClass(), annotation.name());
  }

}

