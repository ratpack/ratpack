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
import org.testng.annotations.AfterTest
import ratpack.stream.Streams
import ratpack.test.exec.ExecHarness

import java.time.Duration

class BufferingPublisherVerification extends PublisherVerification<Integer> {

  BufferingPublisherVerification() {
    super(new TestEnvironment(300L), 5000)
  }

  ExecHarness harness = ExecHarness.harness()

  @AfterTest
  void stopHarness() throws Exception {
    harness.close()
  }

  @Override
  Publisher<Integer> createPublisher(long elements) {
    Streams.periodically(harness.controller.executor, Duration.ofNanos(100)) {
      it < elements ? it : null
    }
  }

  @Override
  Publisher<Integer> createFailedPublisher() {
    null // because subscription always succeeds. Nothing is attempted until a request is received.
  }

}
