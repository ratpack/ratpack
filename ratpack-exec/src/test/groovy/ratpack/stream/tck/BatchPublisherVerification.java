/*
 * Copyright 2016 the original author or authors.
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
import org.testng.annotations.AfterTest;
import ratpack.stream.Streams;
import ratpack.test.exec.ExecHarness;

import java.time.Duration;

public class BatchPublisherVerification extends PublisherVerification<Integer> {

  public BatchPublisherVerification() {
    super(new TestEnvironment());
  }

  private final ExecHarness harness = ExecHarness.harness();

  @AfterTest
  public void stopHarness() throws Exception {
    harness.close();
  }

  @Override
  public Publisher<Integer> createPublisher(long elements) {
    return Streams.periodically(harness.getController().getExecutor(), Duration.ofNanos(100), i ->
      i < elements ? i : null
    );
  }

  @Override
  public Publisher<Integer> createFailedPublisher() {
    return null; // because subscription always succeeds. Nothing is attempted until a request is received.
  }

}
