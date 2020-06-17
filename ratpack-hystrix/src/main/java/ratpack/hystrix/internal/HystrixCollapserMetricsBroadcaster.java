/*
 * Copyright 2015 the original author or authors.
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
import com.netflix.hystrix.HystrixCollapserMetrics;
import ratpack.exec.stream.internal.MulticastPublisher;

import java.util.Collection;

/**
 * A message broadcaster for sending Hystrix collapser metrics to its subscribers.
 */
public class HystrixCollapserMetricsBroadcaster extends MulticastPublisher<Collection<HystrixCollapserMetrics>> {

  @Inject
  public HystrixCollapserMetricsBroadcaster(HystrixCollapserMetricsPeriodicPublisher publisher) {
    super(publisher);
  }

}
