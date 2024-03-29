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

package ratpack.exec.stream.tck;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterTest;
import ratpack.exec.stream.Streams;
import ratpack.exec.stream.TransformablePublisher;
import ratpack.exec.stream.bytebuf.ByteBufStreams;
import ratpack.test.exec.ExecHarness;

import java.time.Duration;

public class ByteBufferComposingPublisherVerification extends PublisherVerification<ByteBuf> {

  public ByteBufferComposingPublisherVerification() {
    super(new TestEnvironment());
  }

  private final ExecHarness harness = ExecHarness.harness();

  @AfterTest
  public void stopHarness() {
    harness.close();
  }

  @Override
  public Publisher<ByteBuf> createPublisher(long elements) {
    TransformablePublisher<ByteBuf> periodically = Streams.periodically(harness.getController().getExecutor(), Duration.ofNanos(100), i ->
      i < elements * 3 ? i : null
    ).map(Unpooled::copyInt);

    return ByteBufStreams.buffer(periodically, Long.MAX_VALUE, 3, ByteBufAllocator.DEFAULT).map(i -> i);
  }

  @Override
  public Publisher<ByteBuf> createFailedPublisher() {
    return null; // because subscription always succeeds. Nothing is attempted until a request is received.
  }

}
