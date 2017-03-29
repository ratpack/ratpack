/*
 * Copyright 2017 the original author or authors.
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

package ratpack.exec.util;

import ratpack.exec.Promise;
import ratpack.exec.util.internal.DefaultReadWriteAccess;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * Provides read/write serialization, analogous to {@link ReadWriteLock}.
 * <p>
 * Can be used whenever a “resource” has safe concurrent usages and mutually exclusive usages,
 * such as updating a file.
 * <p>
 * The {@link #read(Promise)} and {@link #write(Promise)} methods decorate promises with serialization.
 * Read serialized promises may execute concurrently with other read serialized promises,
 * but not with write serialized promises.
 * Write serialized promises may not execute concurrently with read or write serialized promises.
 * <p>
 * Access is generally fair.
 * That is, access is granted in the order that promises execute (n.b. not in the order they are decorated).
 * <p>
 * Access is not reentrant.
 * Deadlocks are not detected or prevented.
 *
 * @since 1.5
 */
public interface ReadWriteAccess {

  /**
   * Create a new read/write access object.
   *
   * @return a new read/write access object
   */
  static ReadWriteAccess create() {
    return new DefaultReadWriteAccess();
  }

  /**
   * Decorates the given promise with read serialization.
   * <p>
   * Read serialized promises may execute concurrently with other read serialized promises,
   * but not with write serialized promises.
   *
   * @param promise the promise to decorate
   * @param <T> the type of promised value
   * @return a decorated promise
   */
  <T> Promise<T> read(Promise<T> promise);

  /**
   * Decorates the given promise with write serialization.
   * <p>
   * Write serialized promises may not execute concurrently with read or write serialized promises.
   *
   * @param promise the promise to decorate
   * @param <T> the type of promised value
   * @return a decorated promise
   */
  <T> Promise<T> write(Promise<T> promise);

}
