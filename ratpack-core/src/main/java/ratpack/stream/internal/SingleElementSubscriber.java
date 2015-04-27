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
import ratpack.exec.Result;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SingleElementSubscriber<T> implements Subscriber<T> {
  private final Consumer<? super Result<T>> receiver;
  private Subscription subscription;
  private volatile T first;
  private AtomicBoolean fired = new AtomicBoolean();

  public static <T> Subscriber<T> to(Consumer<? super Result<T>> receiver) {
    return new SingleElementSubscriber<>(receiver);
  }

  public SingleElementSubscriber(Consumer<? super Result<T>> receiver) {
    this.receiver = receiver;
  }

  @Override
  public void onSubscribe(Subscription s) {
    if (this.subscription != null) {
      s.cancel();
      return;
    }

    this.subscription = s;
    this.subscription.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(T o) {
    if (first == null) {
      first = o;
    } else {
      subscription.cancel();
      fire(Result.<T>error(new IllegalStateException("Cannot convert stream of more than 1 item to a Promise")));
    }
  }

  private void fire(Result<T> result) {
    if (fired.compareAndSet(false, true)) {
      receiver.accept(result);
    }
  }

  @Override
  public void onError(Throwable t) {
    fire(Result.<T>error(t));
  }

  @Override
  public void onComplete() {
    fire(Result.success(first));
  }
}
