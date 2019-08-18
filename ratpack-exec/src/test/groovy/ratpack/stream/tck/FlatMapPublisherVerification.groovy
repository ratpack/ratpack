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
import org.reactivestreams.Subscriber
import org.reactivestreams.tck.PublisherVerification
import org.reactivestreams.tck.TestEnvironment
import org.testng.annotations.AfterClass
import ratpack.exec.Blocking
import ratpack.stream.Streams
import ratpack.test.exec.ExecHarness

class FlatMapPublisherVerification extends PublisherVerification<Integer> {

  FlatMapPublisherVerification() {
    super(new TestEnvironment(500L))
  }

  private ExecHarness execHarness = ExecHarness.harness()

  @Override
  Publisher<Integer> createPublisher(long elements) {
    return new Publisher<Integer>() {
      @Override
      void subscribe(Subscriber<? super Integer> s) {
        if (s == null) {
          throw null
        }

        execHarness.fork().start {
          def stream = Streams.yield {
            it.requestNum < elements ? elements : null
          }

          stream.bindExec().flatMap { n ->
            Blocking.get { n * 2 }
          }.subscribe(s)
        }
      }
    }
  }

  @Override
  Publisher<Integer> createFailedPublisher() {
    null // because subscription always succeeds. Nothing is attempted until a request is received.
  }

  @AfterClass
  void shutdown() throws Exception {
    execHarness.close()
  }
}
