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

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import ratpack.hystrix.internal.HystrixRegistryBackedConcurrencyStrategy;

/**
 * Provides integration with <a href="https://github.com/Netflix/Hystrix">Hystrix</a>.
 * <p>
 * <b>IMPORTANT:</b> the {@link #initialize()} method must be called to enable Hystrix request caching and collapsing with Ratpack.
 */
public abstract class HystrixRatpack {

  /**
   * Registers a {@link com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy} with Hystrix that provides a {@link ratpack.registry.Registry}
   * backed strategy for caching {@link com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable} during a {@link ratpack.http.Request}.
   * <p>
   * This method only needs to be called once per JVM, regardless of how many Ratpack applications are running within the JVM.
   * <p>
   * Once initialized Hystrix will use Ratpack's Request Registry for request caching, request collapsing and request log.
   *
   * @see <a href="https://github.com/Netflix/Hystrix/wiki/Plugins#concurrency-strategy" target="_blank">Hystrix Plugins - Concurrency Strategy</a>
   * @see <a href="https://github.com/Netflix/Hystrix/wiki/How-To-Use#request-cache" target="_blank">Hystrix - Request Cache</a>
   * @see <a href="https://github.com/Netflix/Hystrix/wiki/How-To-Use#request-collapsing" target="_blank">Hystrix - Request Collapsing</a>
   */
  public static void initialize() {
    try {
      HystrixPlugins.getInstance().registerConcurrencyStrategy(new HystrixRegistryBackedConcurrencyStrategy());
    } catch (IllegalStateException e) {
      HystrixConcurrencyStrategy existingStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
      if (!(existingStrategy instanceof HystrixRegistryBackedConcurrencyStrategy)) {
        throw new IllegalStateException("Cannot install Hystrix integration because another concurrency strategy (" + existingStrategy.getClass() + ") is already installed");
      }
    }
  }

}
