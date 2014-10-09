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

package ratpack.exec.internal;

import ratpack.exec.Promise;
import ratpack.exec.Throttle;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThrottle implements Throttle {

  private final int size;

  private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
  private final AtomicInteger active = new AtomicInteger();
  private final AtomicInteger waiting = new AtomicInteger();

  public DefaultThrottle(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("throttle size must be greater than 1");
    }
    this.size = size;
  }

  @Override
  public <T> Promise<T> throttle(Promise<T> promise) {
    return promise.defer(r -> {
      waiting.incrementAndGet();
      queue.add(r);
      drain();
    }).wiretap(r -> {
      active.decrementAndGet();
      drain();
    });
  }

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public int getActive() {
    return active.get();
  }

  @Override
  public int getWaiting() {
    return waiting.get();
  }

  private void drain() {
    if (!queue.isEmpty()) {
      int i = active.getAndIncrement();
      if (i < size) {
        Runnable job = queue.poll();
        if (job == null) {
          active.decrementAndGet();
        } else {
          waiting.decrementAndGet();
          job.run();
        }
      } else {
        i = active.decrementAndGet();
        if (i < size) {
          drain();
        }
      }
    }
  }

}
