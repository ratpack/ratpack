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

import ratpack.api.NonBlocking;
import ratpack.func.Action;

/**
 * A promise of a successful outcome.
 * <p>
 * Ratpack promises are not fully featured <a href="http://promises-aplus.github.io/promises-spec/">A+ promises</a>.
 * They are designed to be low level and adapted to more fully features asynchronous composition libraries (like RxJava, for which there is a Ratpack extension library).
 * <p>
 * Ratpack promises are <b>not thread safe</b>.
 * Instances should not be used concurrently.
 * <p>
 * Async operations will typically return the subclass of the {@link SuccessPromise}, the {@link SuccessOrErrorPromise} which allows an error handling strategy to be specified.
 *
 * @param <T> the type of the outcome object
 */
public interface SuccessPromise<T> {

  /**
   * Specifies what should be done with the promised object when it is ready.
   *
   * @param then the action to forward the promised object on to
   */
  @NonBlocking
  void then(Action<? super T> then);

}
