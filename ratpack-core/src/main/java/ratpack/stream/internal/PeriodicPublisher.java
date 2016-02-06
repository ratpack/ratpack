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

import org.reactivestreams.Subscription;
import ratpack.func.Action;
import ratpack.func.Function;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PeriodicPublisher<T> extends BufferingPublisher<T> {
  public PeriodicPublisher(ScheduledExecutorService executorService, Function<? super Integer, ? extends T> producer, Duration duration) {
    super(Action.noop(), write -> {
      return new Subscription() {
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile ScheduledFuture<?> future;

        @Override
        public void request(long n) {
          if (future == null) {
            future = executorService.scheduleWithFixedDelay(() -> {
              int i = counter.getAndIncrement();
              T value;
              try {
                value = producer.apply(i);
              } catch (Exception e) {
                write.error(e);
                cancel();
                return;
              }

              if (value == null) {
                write.complete();
                cancel();
              } else {
                write.item(value);
              }
            }, 0, duration.toNanos(), TimeUnit.NANOSECONDS);
          }
        }

        @Override
        public void cancel() {
          if (future != null) {
            future.cancel(false);
          }
        }
      };
    });
  }

}
