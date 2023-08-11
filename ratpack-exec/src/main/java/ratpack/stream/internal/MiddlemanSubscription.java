/*
 * Copyright 2023 the original author or authors.
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
import ratpack.func.Action;

public abstract class MiddlemanSubscription<U, D> extends ManagedSubscription<D> {

  private final Publisher<? extends U> upstream;
  private final Action<? super U> upstreamDisposer;
  private volatile Subscription upstreamSubscription;

  public MiddlemanSubscription(
      Subscriber<? super D> downstream,
      Action<? super D> downstreamDisposer,
      Publisher<? extends U> upstream,
      Action<? super U> upstreamDisposer
  ) {
    super(downstream, downstreamDisposer);
    this.upstream = upstream;
    this.upstreamDisposer = upstreamDisposer;
  }

  public void connect() {
    upstream.subscribe(new Subscriber<U>() {
      @Override
      public void onSubscribe(Subscription s) {
        upstreamSubscription = s;
        downstream.onSubscribe(MiddlemanSubscription.this);
        if (!isDone()) {
          onConnected();
        }
      }

      @Override
      public void onNext(U u) {
        if (isDone()) {
          doDispose(upstreamDisposer, u);
        } else {
          receiveNext(u);
        }
      }

      @Override
      public void onError(Throwable t) {
        receiveError(t);
      }

      @Override
      public void onComplete() {
        receiveComplete();
      }
    });
  }

  protected void onConnected() {

  }

  @Override
  protected void onRequest(long n) {
    upstreamSubscription.request(n);
  }

  @Override
  protected void onCancel() {
    upstreamSubscription.cancel();
  }

  protected void onConsumed() {
    if (!isOpen()) {
      upstreamSubscription.request(1);
    }
  }

  protected abstract void receiveNext(U upstreamItem);

  protected void receiveError(Throwable error) {
    emitError(error);
  }

  protected void receiveComplete() {
    emitComplete();
  }

}
