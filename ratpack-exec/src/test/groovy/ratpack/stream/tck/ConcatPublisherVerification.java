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

package ratpack.stream.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import ratpack.func.Action;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Executors;

public class ConcatPublisherVerification extends PublisherVerification<Long> {

  public ConcatPublisherVerification() {
    super(new TestEnvironment());
  }

  @Override
  public Publisher<Long> createPublisher(long elements) {
    double p1 = Math.floor((double) elements / 2);
    double p2 = Math.ceil((double) elements / 2);

    return Streams.concat(Arrays.asList(makePublisher(p1), makePublisher(p2)), Action.noop());
  }

  private static TransformablePublisher<Long> makePublisher(final double elements) {
    return Streams.periodically(Executors.newSingleThreadScheduledExecutor(), Duration.ofMillis(5), i -> i < elements ? 1L : null);
  }

  @Override
  public Publisher<Long> createFailedPublisher() {
    return null; // because subscription always succeeds. Nothing is attempted until a request is received.
  }

}
