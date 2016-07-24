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

package ratpack.stream;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

/**
 * Fundamentally transforms a stream.
 *
 * @param <U> the type of item emitted from upstream
 * @param <D> the type of item to emit downstream (i.e. post transform)
 * @since 1.4
 * @see Streams#streamMap(Publisher, StreamMapper)
 */
public interface StreamMapper<U, D> {

  /**
   * A transform step in a {@link Publisher} chain.
   *
   * @param subscription the upstream subscription
   * @param downstream the downstream to write to
   * @return a write stream that receives signals from upstream
   * @throws Exception any error in establishing the mapping
   */
  WriteStream<U> map(Subscription subscription, WriteStream<D> downstream) throws Exception;

}
