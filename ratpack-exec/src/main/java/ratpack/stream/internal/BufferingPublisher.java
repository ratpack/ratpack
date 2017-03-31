/*
 * Copyright 2016 the original author or authors.
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

import io.netty.util.internal.PlatformDependent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.stream.TransformablePublisher;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BufferingPublisher<T> implements TransformablePublisher<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BufferingPublisher.class);

  private static final Object ON_COMPLETE = new Object();
  private static final Object ON_ERROR = new Object();
  private static final Object CANCEL = new Object();

  private final Action<? super T> disposer;
  private final Function<? super BufferedWriteStream<T>, ? extends Subscription> function;

  public BufferingPublisher(Action<? super T> disposer, Publisher<T> publisher) {
    this(disposer, write -> {
      return new ConnectingSubscriber<>(publisher, write);
    });
  }

  public BufferingPublisher(Action<? super T> disposer, Function<? super BufferedWriteStream<T>, ? extends Subscription> function) {
    this.disposer = disposer;
    this.function = function;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    new BufferingSubscription(subscriber);
  }

  private static class ConnectingSubscriber<T> implements Subscriber<T>, Subscription {

    private final Publisher<T> publisher;
    private final BufferedWriteStream<T> write;

    private volatile Subscription upstream;

    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final Queue<Object> signals = PlatformDependent.newMpscQueue();

    ConnectingSubscriber(Publisher<T> publisher, BufferedWriteStream<T> write) {
      this.publisher = publisher;
      this.write = write;
    }

    @Override
    public void request(long n) {
      signals.add(n);
      if (connected.compareAndSet(false, true)) {
        publisher.subscribe(this);
      } else {
        drain();
      }
    }

    @Override
    public void cancel() {
      signals.add(CANCEL);
      drain();
    }

    private void drain() {
      if (draining.compareAndSet(false, true)) {
        try {
          Subscription upstreamRead = upstream;
          if (upstreamRead != null) {
            Object signal = signals.poll();
            while (signal != null) {
              if (signal == CANCEL) {
                upstreamRead.cancel();
              } else {
                upstreamRead.request((long) signal);
              }
              signal = signals.poll();
            }
          }
        } finally {
          draining.set(false);
        }
        if (!signals.isEmpty() && upstream != null) {
          drain();
        }
      }
    }

    @Override
    public void onSubscribe(Subscription s) {
      upstream = s;
      drain();
    }

    @Override
    public void onNext(T t) {
      write.item(t);
    }

    @Override
    public void onError(Throwable t) {
      write.error(t);
    }

    @Override
    public void onComplete() {
      write.complete();
    }
  }

  private class BufferingSubscription implements Subscription {
    private final Deque<T> buffer = new ConcurrentLinkedDeque<>();

    private final AtomicLong wanted = new AtomicLong();
    private final AtomicBoolean draining = new AtomicBoolean();
    private volatile boolean open;

    private volatile Subscription upstreamSubscription;
    private volatile Subscriber<? super T> downstream;
    private volatile Throwable error;

    private final BufferedWriteStream<T> writeStream = new WriteStream();

    BufferingSubscription(Subscriber<? super T> downstream) {
      this.downstream = downstream;
      downstream.onSubscribe(this);
      open = true;
      drain();
    }

    private void drain() {
      if (draining.compareAndSet(false, true)) {
        try {
          T item = buffer.poll();
          while (item != null) {
            if (item == ON_COMPLETE) {
              if (downstream != null) {
                downstream.onComplete();
                downstream = null;
              }
            } else if (item == ON_ERROR) {
              if (downstream != null) {
                assert error != null;
                downstream.onError(error);
                downstream = null;
              }
            } else {
              if (downstream == null || error != null) {
                try {
                  disposer.execute(item);
                } catch (Exception e) {
                  LOGGER.warn("exception raised disposing of " + item + " - will be ignored", e);
                }
              } else {
                if (wanted.get() > 0) {
                  downstream.onNext(item);
                  if (wanted.decrementAndGet() == 0) {
                    break;
                  }
                } else {
                  buffer.push(item);
                  break;
                }
              }
            }
            item = buffer.poll();
          }
        } finally {
          draining.set(false);
        }
        T peek = buffer.peek();
        if (peek != null && (wanted.get() > 0 || peek == ON_COMPLETE || peek == ON_ERROR)) {
          drain();
        }
      }
    }

    @Override
    public void request(long n) {
      if (downstream == null) {
        return;
      }
      if (n < 1) {
        downstream.onError(new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0."));
        cancel();
      }

      if (upstreamSubscription == null) {
        try {
          upstreamSubscription = function.apply(writeStream);
        } catch (Exception e) {
          writeStream.error(e);
          return;
        }
      }

      if (wanted.get() < Long.MAX_VALUE) {
        long nowWanted = wanted.addAndGet(n);
        if (nowWanted == Long.MAX_VALUE || nowWanted < 0) {
          wanted.set(Long.MAX_VALUE);
          upstreamSubscription.request(Long.MAX_VALUE);
        } else {
          long outstanding = n - buffer.size();
          if (outstanding > 0) {
            upstreamSubscription.request(outstanding);
          }
        }
      }
      drain();
    }

    @Override
    public void cancel() {
      if (downstream != null) {
        downstream = null;
        if (upstreamSubscription != null) {
          upstreamSubscription.cancel();
        }
        drain();
      }
    }

    class WriteStream implements BufferedWriteStream<T> {
      @Override
      public void item(T item) {
        buffer.add(item);
        if (open) {
          drain();
        }
      }

      @SuppressWarnings("unchecked")
      @Override
      public void error(Throwable throwable) {
        error = throwable;
        buffer.add((T) ON_ERROR);
        if (open) {
          drain();
        }
      }

      @SuppressWarnings("unchecked")
      @Override
      public void complete() {
        buffer.add((T) ON_COMPLETE);
        if (open) {
          drain();
        }
      }

      @Override
      public long getRequested() {
        return wanted.get();
      }

      @Override
      public long getBuffered() {
        return buffer.size();
      }

      @Override
      public boolean isCancelled() {
        return downstream == null;
      }
    }

  }
}
