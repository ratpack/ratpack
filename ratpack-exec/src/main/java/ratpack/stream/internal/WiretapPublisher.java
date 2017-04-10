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
import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.stream.StreamEvent;
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WiretapPublisher<T> implements TransformablePublisher<T> {

  private final Publisher<? extends T> publisher;
  private final Action<? super StreamEvent<T>> listener;
  private final AtomicInteger counter = new AtomicInteger();

  public WiretapPublisher(Publisher<? extends T> publisher, Action<? super StreamEvent<T>> listener) {
    this.publisher = publisher;
    this.listener = listener;
  }

  @Override
  public void subscribe(final Subscriber<? super T> outSubscriber) {
    final int subscriptionId = counter.getAndIncrement();
    publisher.subscribe(new Subscriber<T>() {

      private Subscription subscription;
      private final AtomicBoolean done = new AtomicBoolean();

      @Override
      public void onSubscribe(final Subscription subscription) {
        this.subscription = subscription;
        outSubscriber.onSubscribe(new Subscription() {
          @Override
          public void request(long n) {
            try {
              listener.execute(new RequestEvent<T>(subscriptionId, n));
            } catch (Throwable throwable) {
              subscription.cancel();
              onError(throwable);
              return;
            }
            subscription.request(n);
          }

          @Override
          public void cancel() {
            try {
              listener.execute(new CancelEvent<T>(subscriptionId));
            } catch (Throwable throwable) {
              try {
                subscription.cancel();
              } catch (Throwable e) {
                throwable.addSuppressed(e);
              }
              onError(throwable);
              return;
            }
            subscription.cancel();
          }
        });
      }

      @Override
      public void onNext(T in) {
        try {
          listener.execute(new DataEvent<>(subscriptionId, in));
        } catch (Throwable throwable) {
          subscription.cancel();
          onError(throwable);
          return;
        }

        if (!done.get()) {
          outSubscriber.onNext(in);
        }
      }

      @Override
      public void onError(Throwable t) {
        if (done.compareAndSet(false, true)) {
          try {
            listener.execute(new ErrorEvent<T>(subscriptionId, t));
          } catch (Throwable throwable) {
            t.addSuppressed(throwable);
            onError(t);
            return;
          }
          outSubscriber.onError(t);
        }
      }

      @Override
      public void onComplete() {
        if (done.compareAndSet(false, true)) {
          try {
            listener.execute(new CompletionEvent<T>(subscriptionId));
          } catch (Throwable throwable) {
            outSubscriber.onError(throwable);
            return;
          }
          outSubscriber.onComplete();
        }
      }
    });
  }

  private static class DataEvent<T> implements StreamEvent<T> {
    private final int subscriptionId;
    private final T data;

    private DataEvent(int subscriptionId, T data) {
      this.subscriptionId = subscriptionId;
      this.data = data;
    }

    public int getSubscriptionId() {
      return subscriptionId;
    }

    @Override
    public boolean isComplete() {
      return false;
    }

    @Override
    public boolean isError() {
      return false;
    }

    @Override
    public boolean isData() {
      return true;
    }

    @Nullable
    @Override
    public Throwable getThrowable() {
      return null;
    }

    @Nullable
    @Override
    public T getItem() {
      return data;
    }

    @Override
    public boolean isCancel() {
      return false;
    }

    @Override
    public boolean isRequest() {
      return false;
    }

    @Override
    public long getRequestAmount() {
      return 0;
    }

    @Override
    public String toString() {
      return "StreamEvent[DataEvent{subscriptionId=" + subscriptionId + ", data=" + data + "}]";
    }
  }

  private static class CompletionEvent<T> implements StreamEvent<T> {

    private final int subscriptionId;

    private CompletionEvent(int subscriptionId) {
      this.subscriptionId = subscriptionId;
    }

    public int getSubscriptionId() {
      return subscriptionId;
    }

    @Override
    public boolean isComplete() {
      return true;
    }

    @Override
    public boolean isError() {
      return false;
    }

    @Override
    public boolean isData() {
      return false;
    }

    @Nullable
    @Override
    public Throwable getThrowable() {
      return null;
    }

    @Nullable
    @Override
    public T getItem() {
      return null;
    }

    @Override
    public boolean isCancel() {
      return false;
    }

    @Override
    public boolean isRequest() {
      return false;
    }

    @Override
    public long getRequestAmount() {
      return 0;
    }

    @Override
    public String toString() {
      return "StreamEvent[CompletionEvent{subscriptionId=" + subscriptionId + "}]";
    }
  }

  private static class ErrorEvent<T> implements StreamEvent<T> {
    private final int subscriptionId;
    private final Throwable error;

    private ErrorEvent(int subscriptionId, Throwable error) {
      this.subscriptionId = subscriptionId;
      this.error = error;
    }

    public int getSubscriptionId() {
      return subscriptionId;
    }

    @Override
    public boolean isComplete() {
      return false;
    }

    @Override
    public boolean isError() {
      return true;
    }

    @Override
    public boolean isData() {
      return false;
    }

    @Nullable
    @Override
    public Throwable getThrowable() {
      return error;
    }

    @Nullable
    @Override
    public T getItem() {
      return null;
    }

    @Override
    public boolean isCancel() {
      return false;
    }

    @Override
    public boolean isRequest() {
      return false;
    }

    @Override
    public long getRequestAmount() {
      return 0;
    }

    @Override
    public String toString() {
      return "StreamEvent[ErrorEvent{subscriptionId=" + subscriptionId + ", error=" + error + "}]";
    }
  }

  private static class CancelEvent<T> implements StreamEvent<T> {
    private final int subscriptionId;

    private CancelEvent(int subscriptionId) {
      this.subscriptionId = subscriptionId;
    }

    @Override
    public int getSubscriptionId() {
      return subscriptionId;
    }

    @Override
    public boolean isComplete() {
      return false;
    }

    @Override
    public boolean isError() {
      return false;
    }

    @Override
    public boolean isData() {
      return false;
    }

    @Override
    public boolean isCancel() {
      return true;
    }

    @Override
    public boolean isRequest() {
      return false;
    }

    @Override
    public long getRequestAmount() {
      return 0;
    }

    @Nullable
    @Override
    public Throwable getThrowable() {
      return null;
    }

    @Nullable
    @Override
    public T getItem() {
      return null;
    }

    @Override
    public String toString() {
      return "StreamEvent[CancelEvent{subscriptionId=" + subscriptionId + "}]";
    }
  }

  private static class RequestEvent<T> implements StreamEvent<T> {

    private final long requestAmount;
    private final int subscriptionId;

    private RequestEvent(int subscriptionId, long requestAmount) {
      this.requestAmount = requestAmount;
      this.subscriptionId = subscriptionId;
    }

    @Override
    public int getSubscriptionId() {
      return subscriptionId;
    }

    @Override
    public boolean isComplete() {
      return false;
    }

    @Override
    public boolean isError() {
      return false;
    }

    @Override
    public boolean isData() {
      return false;
    }

    @Override
    public boolean isCancel() {
      return false;
    }

    @Override
    public boolean isRequest() {
      return true;
    }

    @Override
    public long getRequestAmount() {
      return requestAmount;
    }

    @Nullable
    @Override
    public Throwable getThrowable() {
      return null;
    }

    @Nullable
    @Override
    public T getItem() {
      return null;
    }

    @Override
    public String toString() {
      return "StreamEvent[RequestEvent{requestAmount=" + requestAmount + ", subscriptionId=" + subscriptionId + "}]";
    }
  }

}
