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

package ratpack.hystrix.internal;

import com.google.inject.Inject;
import com.netflix.hystrix.HystrixCommandMetrics;
import ratpack.func.Function;
import ratpack.launch.LaunchConfig;
import ratpack.stream.internal.PeriodicPublisher;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class HystrixCommandMetricsPeriodicPublisher extends PeriodicPublisher<Collection<HystrixCommandMetrics>> {

  /**
   * The default reporting interval.
   */
  private final static String DEFAULT_INTERVAL = "30";

  @Inject
  public HystrixCommandMetricsPeriodicPublisher(LaunchConfig launchConfig) {
    super(
      launchConfig.getExecController().getExecutor(),
      new Function<Integer, Collection<HystrixCommandMetrics>>() {
        @Override
        public Collection<HystrixCommandMetrics> apply(Integer integer) throws Exception {
          return HystrixCommandMetrics.getInstances();
        }
      },
      new Long(launchConfig.getOther("hystrix.stream.interval", DEFAULT_INTERVAL)),
      TimeUnit.SECONDS
    );
  }

}
