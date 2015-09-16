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
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.func.Function;
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.atomic.AtomicBoolean;

public class FlatMapPublisher<O, I> implements TransformablePublisher<O> {

  private final Publisher<I> input;
  private final Function<? super I, ? extends Promise<? extends O>> function;

  public FlatMapPublisher(Publisher<I> input, Function<? super I, ? extends Promise<? extends O>> function) {
    this.input = input;
    this.function = function;
  }

  @Override
  public void subscribe(final Subscriber<? super O> outSubscriber) {
    input.subscribe(new Subscriber<I>() {

      private Subscription subscription;
      private final AtomicBoolean done = new AtomicBoolean();

      @Override
      public void onSubscribe(Subscription subscription) {
        this.subscription = new Subscription() {
          @Override
          public void request(long n) {
            subscription.request(n);
          }

          @Override
          public void cancel() {
            subscription.cancel();
          }
        };
        outSubscriber.onSubscribe(this.subscription);
      }

      @Override
      public void onNext(I in) {
        if (done.get()) {
          return;
        }
        Promise<? extends O> out;
        try {
          out = function.apply(in);
        } catch (Throwable throwable) {
          subscription.cancel();
          innerOnError(throwable);
          return;
        }

        out.onError(e -> {
          subscription.cancel();
          innerOnError(e);
        }).then(v -> {
          if (!done.get()) {
            outSubscriber.onNext(v);
          }
        });
      }

      @Override
      public void onError(Throwable t) {
        Promise.value(t).then(this::innerOnError);
      }

      public void innerOnError(Throwable t) {
        if (done.compareAndSet(false, true)) {
          outSubscriber.onError(t);
        }
      }

      @Override
      public void onComplete() {
        Operation.noop().then(() -> {
          if (done.compareAndSet(false, true)) {
            outSubscriber.onComplete();
          }
        });
      }
    });
  }
}
