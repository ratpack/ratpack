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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.func.Function;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

public class StreamMapPublisher<I, O> implements TransformablePublisher<O> {

  private final TransformablePublisher<I> upstream;
  private final Function<? super Streams.WriteStream<O>, ? extends Streams.WriteStream<I>> mapper;

  private Streams.WriteStream<I> input;

  public StreamMapPublisher(TransformablePublisher<I> upstream, Function<? super Streams.WriteStream<O>, ? extends Streams.WriteStream<I>> mapper) {
    this.upstream = upstream;
    this.mapper = mapper;
  }

  @Override
  public void subscribe(Subscriber<? super O> downstreamSubscriber) {
    upstream.subscribe(new Subscriber<I>() {
      @Override
      public void onSubscribe(Subscription upstreamSubscription) {
        try {
          input = mapper.apply(new Streams.WriteStream<O>() {
            @Override
            public void item(O item) {
              downstreamSubscriber.onNext(item);
            }

            @Override
            public void error(Throwable throwable) {
              upstreamSubscription.cancel();
              downstreamSubscriber.onError(throwable);
            }

            @Override
            public void complete() {
              upstreamSubscription.cancel();
              downstreamSubscriber.onComplete();
            }
          });
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
      public void onNext(I i) {
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

}
