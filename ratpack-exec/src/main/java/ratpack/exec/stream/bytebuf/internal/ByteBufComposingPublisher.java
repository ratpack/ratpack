/*
 * Copyright 2017 the original author or authors.
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

package ratpack.exec.stream.bytebuf.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import ratpack.exec.stream.TransformablePublisher;

public class ByteBufComposingPublisher implements TransformablePublisher<ByteBuf> {

  private final Publisher<? extends ByteBuf> upstream;
  private final ByteBufAllocator alloc;
  private final int maxNum;
  private final long watermark;

  public ByteBufComposingPublisher(int maxNum, long sizeWatermark, ByteBufAllocator alloc, Publisher<? extends ByteBuf> upstream) {
    this.upstream = upstream;
    this.alloc = alloc;
    this.maxNum = maxNum;
    this.watermark = sizeWatermark;
  }

  @Override
  public void subscribe(Subscriber<? super ByteBuf> subscriber) {
    subscriber.onSubscribe(new ByteBufBufferingSubscription(upstream, subscriber, alloc, maxNum, watermark));
  }

}
