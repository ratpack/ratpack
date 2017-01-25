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

package ratpack.exec.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.stream.TransformablePublisher;
import ratpack.util.Exceptions;

import java.util.concurrent.atomic.AtomicBoolean;

public class ExecutionBoundPublisher<T> implements TransformablePublisher<T> {

  private final Publisher<T> publisher;
  private final Action<? super T> disposer;

  public ExecutionBoundPublisher(Publisher<T> publisher, Action<? super T> disposer) {
    this.publisher = publisher;
    this.disposer = disposer;
  }

  // TODO: we should try and get request/cancel signals to the publisher as soon as possible.
  // If the signal comes in off execution, but the publisher is publishing lots of items in execution,
  // It might never see the signal (because we

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    DefaultExecution execution = DefaultExecution.require();
    execution.delimitStream(subscriber::onError, continuation ->
      publisher.subscribe(new Subscriber<T>() {

        public Subscription subscription;

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean pendingCancelSignal = new AtomicBoolean(true);

        private boolean dispatch(boolean respectCancel, Block block) {
          if (respectCancel && cancelled.get()) {
            return false;
          } else if (execution.isBound()) {
            try {
              block.execute();
              return true;
            } catch (Exception e) {
              throw Exceptions.uncheck(e); // really should not happen
            }
          } else {
            return continuation.event(block);
          }
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
          this.subscription = subscription;
          dispatch(true, () ->
            subscriber.onSubscribe(new Subscription() {
              @Override
              public void request(long n) {
                dispatch(true, () -> subscription.request(n));
              }

              @Override
              public void cancel() {
                if (cancelled.compareAndSet(false, true)) {
                  dispatch(false, () -> {
                    if (pendingCancelSignal.compareAndSet(true, false)) {
                      subscription.cancel();
                    }
                  });
                  continuation.complete(Block.noop());
                }
              }
            })
          );
        }

        @Override
        public void onNext(final T element) {
          boolean added = dispatch(true, () -> {
            if (cancelled.get()) {
              dispose(element);
            } else {
              subscriber.onNext(element);
            }
          });
          if (!added) {
            dispose(element);
            if (cancelled.get()) {
              if (execution.isBound() && pendingCancelSignal.compareAndSet(true, false)) {
                subscription.cancel();
              }
            }
          }
        }

        private void dispose(T element) {
          try {
            disposer.execute(element);
          } catch (Exception e) {
            DefaultExecution.LOGGER.warn("Exception raised disposing stream item will be ignored - ", e);
          }
        }

        @Override
        public void onComplete() {
          continuation.complete(() -> {
            if (!cancelled.get()) {
              subscriber.onComplete();
            }
          });
        }

        @Override
        public void onError(final Throwable cause) {
          if (!cancelled.get()) {
            continuation.complete(() -> subscriber.onError(cause));
          }
        }
      })
    );
  }
}
