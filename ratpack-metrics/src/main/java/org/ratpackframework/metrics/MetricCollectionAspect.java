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

package org.ratpackframework.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.netty.handler.codec.http.FullHttpRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class MetricCollectionAspect {

  @Inject
  MetricRegistry metric;

  @Pointcut("execution(* org.ratpackframework.server.internal.NettyHandlerAdapter.channelRead0(..))")
  public void requests() {}

  public MetricCollectionAspect() {
    Injector injector = Guice.createInjector(new MetricsModule());
    injector.injectMembers(this);
  }

  @Around(value = "requests()", argNames = "pjp")
  public Object collectRequestMetrics(final ProceedingJoinPoint pjp) throws Throwable {
    FullHttpRequest request = (FullHttpRequest) pjp.getArgs()[1];
    String tag = "'" + request.getUri() + "'." + request.getMethod().name() + ".Request";
    Timer.Context timerContext =  metric.timer(tag).time();

    try {
      return pjp.proceed();
    } finally {
      timerContext.stop();
    }
  }

}
