/*
 * Copyright 2016 the original author or authors.
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

import ratpack.stream.WriteStream;

/**
 * Note: should become a public type in 1.3, along with a public factory for {@link BufferingPublisher}.
 * @param <T> element type
 */
public interface BufferedWriteStream<T> extends WriteStream<T> {

  /**
   * The number of the outstanding requested items that the subscriber has asked for.
   *
   * @return
   */
  long getRequested();

  /**
   * How many items have been emitted but not yet requested
   *
   * @return
   */
  long getBuffered();

  boolean isCancelled();

}
