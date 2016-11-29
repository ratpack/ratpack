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

package ratpack.stream.tck

import org.reactivestreams.Publisher
import org.reactivestreams.tck.PublisherVerification
import org.reactivestreams.tck.TestEnvironment
import ratpack.stream.Streams
import ratpack.stream.TransformablePublisher

import java.time.Duration
import java.util.concurrent.Executors

class ConcatPublisherVerification extends PublisherVerification<Long> {

  ConcatPublisherVerification() {
    super(new TestEnvironment())
  }

  @Override
  Publisher<Long> createPublisher(long elements) {
    def p1 = Math.floor(elements / 2)
    def p2 = Math.ceil(elements / 2)
    Streams.concat(makePublisher(p1), makePublisher(p2))
  }

  static TransformablePublisher<Integer> makePublisher(double elements) {
    Streams.periodically(Executors.newSingleThreadScheduledExecutor(), Duration.ofMillis(10)) {
      it < elements ? 1 : null
    }
  }

  @Override
  Publisher<Long> createFailedPublisher() {
    null // because subscription always succeeds. Nothing is attempted until a request is received.
  }

}
