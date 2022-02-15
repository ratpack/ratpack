/*
 * Copyright 2022 the original author or authors.
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

package ratpack.exec.stream.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.stream.TransformablePublisher;
import ratpack.func.Predicate;

public class TakeWhilePublisher<T> implements TransformablePublisher<T> {

  private final Predicate<T> condition;
  private final Publisher<T> upstreamPublisher;

  public TakeWhilePublisher(Predicate<T> condition, Publisher<T> upstreamPublisher) {
    this.condition = condition;
    this.upstreamPublisher = upstreamPublisher;
  }

  @Override
  public void subscribe(Subscriber<? super T> downstreamSubscriber) {
    upstreamPublisher.subscribe(new Subscriber<T>() {

      private Subscription subscription;

      @Override
      public void onSubscribe(Subscription upstreamPublisherSubscription) {
        subscription = upstreamPublisherSubscription;
        downstreamSubscriber.onSubscribe(upstreamPublisherSubscription);
      }

      @Override
      public void onNext(T t) {
        boolean shouldContinue;
        try {
          shouldContinue = condition.apply(t);
        } catch (Exception e) {
          if (subscription != null) {
            subscription.cancel();
          }
          onError(e);
          return;
        }

        if (shouldContinue) {
          downstreamSubscriber.onNext(t);
        } else {
          if (subscription != null) {
            subscription.cancel();
          }
          onComplete();
        }
      }

      @Override
      public void onError(Throwable t) {
        downstreamSubscriber.onError(t);
      }

      @Override
      public void onComplete() {
        downstreamSubscriber.onComplete();
      }
    });
  }
}

