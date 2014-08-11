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

package ratpack.stream.internal;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public abstract class StreamProcessorSupport<T> implements Processor<T, ByteBuf> {

  private final Publisher<T> sourcePublisher;
  protected Subscriber<ByteBuf> targetSubscriber;

  public StreamProcessorSupport(Publisher<T> sourcePublisher) {
    this.sourcePublisher = sourcePublisher;
  }

  @Override
  public void subscribe(Subscriber<ByteBuf> s) {
    this.targetSubscriber = s;
    sourcePublisher.subscribe(this);
  }

  @Override
  public void onSubscribe(Subscription s) {
    this.targetSubscriber.onSubscribe(s);
  }

  @Override
  public void onError(Throwable t) {
    this.targetSubscriber.onError(t);
  }

  @Override
  public void onComplete() {
    this.targetSubscriber.onComplete();
  }

}
