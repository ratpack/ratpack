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

package ratpack.exec;

import ratpack.exec.internal.DefaultThrottle;
import ratpack.exec.internal.UnlimitedThrottle;

/**
 * Limits the concurrency of operations, typically access to an external resource.
 * <p>
 * A given throttle limits the amount of concurrently executing promise, effectively limiting concurrency.
 * <p>
 * The queueing employed by the throttle is generally fair (i.e. oldest promises execute first), but this is not completely guaranteed.
 *
 * @see Promise#throttled(Throttle)
 */
public interface Throttle {

  /**
   * Create a new throttle of the given size.
   *
   * @param size the desired size
   * @return a new throttle of the given size
   */
  static Throttle ofSize(int size) {
    return new DefaultThrottle(size);
  }

  /**
   * Create a new throttle that does not limit concurrency.
   *
   * @return an unlimited throttle
   */
  static Throttle unlimited() {
    return new UnlimitedThrottle();
  }

  /**
   * Throttles the given promise.
   *
   * @param promise the promise to throttle
   * @param <T> the type of promised value
   * @return the throttled promise
   */
  <T> Promise<T> throttle(Promise<T> promise);

  /**
   * The size of this throttle.
   * <p>
   * The throttle guarantees that no more than this number of promises that were throttled via {@link #throttle(Promise)} will execute at the same time.
   * <p>
   * Returns &lt; 1 if the throttle is unlimited.
   *
   * @return the throttle size
   */
  int getSize();

  /**
   * How many throttled promises are currently executing.
   *
   * @return how many throttled promises are currently executing
   */
  int getActive();

  /**
   * The number of throttled promises that are waiting to execute (that is, the queue size).
   *
   * @return the number of promises waiting to execute
   */
  int getWaiting();
}
