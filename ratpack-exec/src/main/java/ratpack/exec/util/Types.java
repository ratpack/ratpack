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

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;

public class Types {

  /**
   * Creates a type token for a promise of of the given type.
   * <pre class="java">{@code
   * import ratpack.exec.util.Types;
   * import ratpack.exec.Promise;
   * import com.google.common.reflect.TypeToken;
   *
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) {
   *     assertEquals(Types.promiseOf(String.class), new TypeToken<Promise<String>>() {});
   *   }
   * }
   * }</pre>
   *
   * @param type the promise element type
   * @param <T> the promise element type
   * @return a type token for a promise of of the given type.
   */
  public static <T> TypeToken<Promise<T>> promiseOf(Class<T> type) {
    return promiseOf(ratpack.util.Types.token(type));
  }

  /**
   * Creates a type token for a promise of the given type.
   *
   * <pre class="java">{@code
   * import ratpack.exec.util.Types;
   * import ratpack.exec.Promise;
   * import com.google.common.reflect.TypeToken;
   *
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) {
   *     assertEquals(
   *       Types.promiseOf(new TypeToken<List<String>>() {}),
   *       new TypeToken<Promise<List<String>>>() {}
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param type the promise element type
   * @param <T> the promise element type
   * @return a type token for a promise of of the given type.
   */
  public static <T> TypeToken<Promise<T>> promiseOf(TypeToken<T> type) {
    return new TypeToken<Promise<T>>() {
    }.where(new TypeParameter<T>() {
    }, type);
  }
}
