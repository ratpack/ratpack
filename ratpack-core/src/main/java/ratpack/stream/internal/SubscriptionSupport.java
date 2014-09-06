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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

abstract class SubscriptionSupport implements Subscription {

  private final AtomicBoolean open = new AtomicBoolean();
  private final AtomicLong waiting = new AtomicLong();

  @Override
  public final void request(long n) {
    if (n < 1) {
      throw new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0.");
    }

    if (!open.get()) {
      waiting.addAndGet(n);
    } else {
      // Doesn't matter if this gets fired before the waiting demand is signalled in open()
      // as the value is just cumulative
      doRequest(n);
    }
  }

  protected void open() {
    if (open.compareAndSet(false, true)) {
      long waitingAmount = waiting.getAndSet(0);
      if (waitingAmount > 0) {
        doRequest(waitingAmount);
      }
    }
  }

  protected abstract void doRequest(long n);

}
