/*
 * Copyright 2019 the original author or authors.
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

package ratpack.func;

@FunctionalInterface
public interface Function7<I1, I2, I3, I4, I5, I6, I7, O> {

  O apply(I1 i1, I2 i2, I3 i3, I4 i4, I5 i5, I6 i6, I7 i7) throws Exception;

  default Function6<I2, I3, I4, I5, I6, I7, O> curry(I1 i1) {
    return (i2, i3, i4, i5, i6, i7) -> apply(i1, i2, i3, i4, i5, i6, i7);
  }

  default <V> Function7<I1, I2, I3, I4, I5, I6, I7, V> andThen(Function<? super O, ? extends V> transform) {
    return (i1, i2, i3, i4, i5, i6, i7) -> transform.apply(apply(i1, i2, i3, i4, i5, i6, i7));
  }

}
