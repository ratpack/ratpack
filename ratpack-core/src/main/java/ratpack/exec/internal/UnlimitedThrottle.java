/*
 * Copyright 2015 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;

public class UnlimitedThrottle implements Throttle {

  private final AtomicInteger active = new AtomicInteger();

  @Override
  public <T> Promise<T> throttle(Promise<T> promise) {
    return promise.onYield(active::incrementAndGet).wiretap(r -> active.decrementAndGet());
  }

  @Override
  public int getSize() {
    return -1;
  }

  @Override
  public int getActive() {
    return active.get();
  }

  @Override
  public int getWaiting() {
    return 0;
  }
}
