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

package ratpack.exec;

import java.util.Objects;

@FunctionalInterface
public interface Sampler<I> {

  /**
   * The sampler implementation.
   *
   * @param i the input to the sample
   * @return operation that executes the sample
   * @throws Exception any
   */
  Operation sample(I i) throws Exception;

  /**
   * Joins {@code this} sample with the given sampler.
   *
   * @param after the sample to apply after {@code this} sample
   * @return the result of applying the given sample to {@code this} sample
   * @throws Exception any thrown by {@code this} or {@code after}
   */
  default Sampler<I> andThen(Sampler<? super I> after) throws Exception {
    Objects.requireNonNull(after);
    return (I i) -> sample(i).next(() -> after.sample(i));
  }

  /**
   * Joins the given sampler with {@code this} sample.
   *
   * @param before the sampler to apply {@code this} before this sample
   * @return the result of applying {@code this} sample after the given sampler
   * @throws Exception any thrown by {@code this} or {@code before}
   */
  default Sampler<I> compose(Sampler<? super I> before) throws Exception {
    Objects.requireNonNull(before);
    return (I i) -> before.sample(i).next(() -> sample(i));
  }
}
