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

package ratpack.hystrix;

import com.google.inject.Singleton;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import ratpack.guice.ConfigurableModule;
import ratpack.hystrix.internal.*;

/**
 * An extension module that provides support for <a href="https://github.com/Netflix/Hystrix/wiki">Hystrix</a>.
 * <p>
 * To use it one has to register the module.
 * <p>
 * By default the module registers a {@link com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy}
 * with Hystrix that provides a {@link ratpack.registry.Registry} backed strategy for caching {@link com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable}
 * during a {@link ratpack.http.Request}.  This means that Hystrix will use Ratpack's Request Registry for request caching, request collapsing and request log.
 * <p>
 * To enable the streaming of Hystrix metrics in text/event-stream format use the {@link #sse()} configuration option.
 * For example: (Groovy DSL)
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.hystrix.HystrixModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new HystrixModule().sse()
 *   }
 * }
 * </pre>
 * <p>
 * This allows Server Sent Event based clients such as the <a href="https://github.com/Netflix-Skunkworks/hystrix-dashboard/wiki">Hystrix Dashboard</a> and
 * <a href="https://github.com/Netflix/Turbine/wiki">Turbine</a> to consume the metrics being reported by your application in realtime.  See
 * {@link ratpack.hystrix.HystrixMetricsEventStreamHandler} for more details.
 *
 * @see <a href="https://github.com/Netflix/Hystrix/wiki" target="_blank">Hystrix</a>
 * @see <a href="https://github.com/Netflix/Hystrix/wiki/Plugins#concurrency-strategy" target="_blank">Hystrix Plugins - Concurrency Strategy</a>
 * @see <a href="https://github.com/Netflix/Hystrix/wiki/How-To-Use#request-cache" target="_blank">Hystrix - Request Cache</a>
 * @see <a href="https://github.com/Netflix/Hystrix/wiki/How-To-Use#request-collapsing" target="_blank">Hystrix - Request Collapsing</a>
 */
public class HystrixModule extends ConfigurableModule<HystrixModule.Config> {

  private boolean reportMetricsToSse;

  public static class Config {

    public static final long DEFAULT_INTERVAL = 2;

    private long streamInterval = DEFAULT_INTERVAL;

    /**
     * The configured stream interval in seconds;
     *
     * @return the configured stream interval in seconds;
     */
    public long getStreamInterval() {
      return streamInterval;
    }

    /**
     * Configure the number of seconds between periodic publications.
     *
     * @param streamInterval the number of seconds between publications in seconds
     * @return this
     */
    public Config streamInterval(long streamInterval) {
      this.streamInterval = streamInterval;
      return this;
    }
  }

  @Override
  protected void configure() {
    try {
      HystrixPlugins.getInstance().registerConcurrencyStrategy(new HystrixRegistryBackedConcurrencyStrategy());
    } catch (IllegalStateException e) {
      HystrixConcurrencyStrategy existingStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
      if (!(existingStrategy instanceof HystrixRegistryBackedConcurrencyStrategy)) {
        throw new IllegalStateException("Cannot install Hystrix integration because another concurrency strategy (" + existingStrategy.getClass() + ") is already installed");
      }
    }

    if (reportMetricsToSse) {
      bind(HystrixCommandMetricsPeriodicPublisher.class).in(Singleton.class);
      bind(HystrixCommandMetricsBroadcaster.class).in(Singleton.class);
      bind(HystrixCommandMetricsJsonMapper.class).in(Singleton.class);

      bind(HystrixThreadPoolMetricsPeriodicPublisher.class).in(Singleton.class);
      bind(HystrixThreadPoolMetricsBroadcaster.class).in(Singleton.class);
      bind(HystrixThreadPoolMetricsJsonMapper.class).in(Singleton.class);

      bind(HystrixCollapserMetricsPeriodicPublisher.class).in(Singleton.class);
      bind(HystrixCollapserMetricsBroadcaster.class).in(Singleton.class);
      bind(HystrixCollapserMetricsJsonMapper.class).in(Singleton.class);
    }
  }

  /**
   * Enable the reporting of Hystrix metrics via SSE.
   * <p>
   * To stream metrics within an application see {@link HystrixMetricsEventStreamHandler}.
   *
   * @return this {@code HystrixModule}
   */
  public HystrixModule sse() {
    this.reportMetricsToSse = true;
    return this;
  }
}
