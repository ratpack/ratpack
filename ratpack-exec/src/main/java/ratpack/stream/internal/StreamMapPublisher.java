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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.stream.StreamMapper;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.WriteStream;

public class StreamMapPublisher<U, D> implements TransformablePublisher<D> {

  private final Publisher<? extends U> upstream;
  private final StreamMapper<? super U, ? extends D> mapper;

  private WriteStream<? super U> input;

  public StreamMapPublisher(Publisher<? extends U> upstream, StreamMapper<? super U, ? extends D> mapper) {
    this.upstream = upstream;
    this.mapper = mapper;
  }

  @Override
  public void subscribe(Subscriber<? super D> downstreamSubscriber) {
    upstream.subscribe(new Subscriber<U>() {
      @Override
      public void onSubscribe(Subscription upstreamSubscription) {
        try {
          input = mapStream(upstreamSubscription, downstreamSubscriber, mapper);
        } catch (Exception e) {
          upstreamSubscription.cancel();
          downstreamSubscriber.onError(e);
          return;
        }

        downstreamSubscriber.onSubscribe(new Subscription() {
          @Override
          public void request(long n) {
            upstreamSubscription.request(n);
          }

          @Override
          public void cancel() {
            upstreamSubscription.cancel();
          }
        });
      }


      @Override
      public void onNext(U i) {
        input.item(i);
      }

      @Override
      public void onError(Throwable t) {
        input.error(t);
      }

      @Override
      public void onComplete() {
        input.complete();
      }
    });
  }

  private static <U, D> WriteStream<U> mapStream(Subscription upstreamSubscription, final Subscriber<? super D> downstreamSubscriber, StreamMapper<U, D> mapper) throws Exception {
    return mapper.map(upstreamSubscription, new WriteStream<D>() {
      @Override
      public void item(D item) {
        downstreamSubscriber.onNext(item);
      }

      @Override
      public void error(Throwable throwable) {
        downstreamSubscriber.onError(throwable);
      }

      @Override
      public void complete() {
        downstreamSubscriber.onComplete();
      }
    });
  }

}
