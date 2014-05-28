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

package ratpack.util;


/**
 * Static utility methods for dealing with types.
 */
public abstract class Types {

  private Types() {
  }

  /**
   * Simply casts the argument to {@code T}.
   * <p>
   * This method will throw {@link ClassCastException} if {@code o} is not compatible with {@code T}.
   *
   * @param o the object to cast
   * @param <T> the target type
   * @return the input object cast to the target type
   */
  public static <T> T cast(Object o) {
    @SuppressWarnings("unchecked") T cast = (T) o;
    return cast;
  }

}
