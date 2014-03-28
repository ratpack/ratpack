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

package ratpack.parse;

public class Parse<T, O> {

  private final Class<T> type;
  private final O opts;

  private Parse(Class<T> type, O opts) {
    this.type = type;
    this.opts = opts;
  }

  public Class<T> getType() {
    return type;
  }

  public O getOpts() {
    return opts;
  }

  public static <T, O> Parse<T, O> of(Class<T> type, O opts) {
    return new Parse<>(type, opts);
  }

  public static <T> Parse<T, NullParseOpts> of(Class<T> type) {
    return of(type, NullParseOpts.INSTANCE);
  }

}
