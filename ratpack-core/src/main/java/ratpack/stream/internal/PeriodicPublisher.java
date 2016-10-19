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
import java.util.concurrent.TimeUnit;

public class PeriodicPublisher<T> extends BufferingPublisher<T> {
  public PeriodicPublisher(ScheduledExecutorService executorService, Function<? super Integer, ? extends T> producer, Duration duration) {
    super(Action.noop(), write -> {
      return new Subscription() {
        private volatile int counter;
        private volatile boolean started;
        private volatile boolean cancelled;

        class Task implements Runnable {
          @Override
          public void run() {
            T value;
            try {
              value = producer.apply(counter++);
            } catch (Exception e) {
              cancelled = true;
              write.error(e);
              return;
            }

            if (value == null) {
              cancelled = true;
              write.complete();
            } else {
              if (!cancelled) {
                write.item(value);
                executorService.schedule(this, duration.toNanos(), TimeUnit.NANOSECONDS);
              }
            }
          }
        }

        @Override
        public void request(long n) {
          if (!started) {
            started = true;
            new Task().run();
          }
        }

        @Override
        public void cancel() {
          cancelled = true;
        }
      };
    });
  }

}
