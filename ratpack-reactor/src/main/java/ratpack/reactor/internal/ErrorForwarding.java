/*
 * Copyright 2021 the original author or authors.
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

package ratpack.reactor.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

public class ErrorForwarding {

  private static final ThreadLocal<Integer> THREAD_STACK = ThreadLocal.withInitial(() -> 0);

  public static Publisher<Object> decorate(Publisher<Object> up) {
    if (THREAD_STACK.get() == 0) {
      if (up instanceof Flux) {
        return new ErrorForwardingFlux(up);
      } else if (up instanceof Mono) {
        return new ErrorForwardingMono(up);
      } else {
        throw new UnsupportedOperationException("unknown publisher type: " + up);
      }
    } else {
      return up;
    }
  }

  private static void subscribe(Publisher<Object> up, Subscriber<? super Object> down) {
    THREAD_STACK.set(THREAD_STACK.get() + 1);
    try {
      up.subscribe(new ErrorForwardingSubscriber(down));
    } finally {
      THREAD_STACK.set(THREAD_STACK.get() - 1);
    }
  }


  private static class ErrorForwardingFlux extends Flux<Object> {
    private final Publisher<Object> up;

    public ErrorForwardingFlux(Publisher<Object> up) {
      this.up = up;
    }

    @Override
    public void subscribe(@NonNull CoreSubscriber<? super Object> actual) {
      ErrorForwarding.subscribe(up, actual);
    }
  }

  private static class ErrorForwardingMono extends Mono<Object> {
    private final Publisher<Object> up;

    public ErrorForwardingMono(Publisher<Object> up) {
      this.up = up;
    }

    @Override
    public void subscribe(@NonNull CoreSubscriber<? super Object> actual) {
      ErrorForwarding.subscribe(up, actual);
    }
  }

  private static class ErrorForwardingSubscriber implements CoreSubscriber<Object> {
    private final Subscriber<? super Object> down;

    public ErrorForwardingSubscriber(Subscriber<? super Object> down) {
      this.down = down;
    }

    @Override
    public void onSubscribe(@NonNull Subscription subscription) {
      down.onSubscribe(subscription);
    }

    @Override
    public void onNext(Object o) {
      down.onNext(o);
    }

    @Override
    public void onError(Throwable t) {
      try {
        down.onError(t);
      } catch (Throwable e) {
        if (Execution.isActive()) {
          if (reactor.core.Exceptions.isErrorCallbackNotImplemented(t)) {
            Promise.error(t.getCause()).then(Action.noop());
          } else {
            Promise.error(reactor.core.Exceptions.unwrap(t)).then(Action.noop());
          }
        } else {
          throw reactor.core.Exceptions.propagate(t);
        }
      }
    }

    @Override
    public void onComplete() {
      down.onComplete();
    }
  }
}
