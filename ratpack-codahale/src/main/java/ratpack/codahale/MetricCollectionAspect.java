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

package ratpack.codahale;

import io.netty.channel.socket.SocketChannel;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class MetricCollectionAspect {

  @Pointcut("execution(* ratpack.server.internal.RatpackChannelInitializer.initChannel(..))")
  public void initChannel() {}

  @After(value = "initChannel() && args(ch)")
  public void addMetricChannelHandler(SocketChannel ch) {
    ch.pipeline().addBefore("handler", "metricHandler", new MetricChannelHandler());
  }

}
