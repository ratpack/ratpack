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
import ratpack.stream.TransformablePublisher;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Note, this publisher does not respect back pressure.
 * Must be used in conjunction with a throttling strategy.
 */
public class PeriodicPublisher<T> implements TransformablePublisher<T> {
  private final ScheduledExecutorService executorService;
  private final Function<Integer, T> producer;
  private final Duration duration;

  public PeriodicPublisher(ScheduledExecutorService executorService, Function<Integer, T> producer, Duration duration) {
    this.executorService = executorService;
    this.producer = producer;
    this.duration = duration;
  }

  @Override
  public void subscribe(final Subscriber<? super T> s) {
    s.onSubscribe(new Subscription() {
      private final AtomicInteger counter = new AtomicInteger(0);
      private final ScheduledFuture<?> future = executorService.scheduleWithFixedDelay(() -> {
        int i = counter.getAndIncrement();
        T value;
        try {
          value = producer.apply(i);
        } catch (Exception e) {
          cancel();
          s.onError(e);
          return;
        }

        if (value == null) {
          s.onComplete();
          cancel();
        } else {
          s.onNext(value);
        }
      }, 0, duration.toNanos(), TimeUnit.NANOSECONDS);

      @Override
      public void request(long n) {

      }

      @Override
      public void cancel() {
        future.cancel(false);
      }

    });
  }
}
