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

/**
 * An upstream asynchronous data source.
 * <p>
 * An upstream can be {@link #connect connected} at any time to a {@link Downstream}, zero or more times.
 * Once connected, the upstream will at some time in the future invoke one of the downstream's {@link Downstream#success}, {@link Downstream#error}, {@link Downstream#complete} methods.
 * Only one of these methods will be invoked per downstream that is connected, and only invoked once.
 * <p>
 * An upstream is the producer side of a {@link Promise}.
 * Unlike a {@link org.reactivestreams.Publisher}, it only produces one value and does not stream.
 * <p>
 * Multiple downstreams can connect to a single upstream.
 *
 * @see Promise#transform(ratpack.func.Function)
 * @param <T> the type of item emitted downstream
 */
public interface Upstream<T> {

  /**
   * Connect the downstream.
   *
   * @param downstream the downstream to emit data to
   * @throws Exception any
   */
  void connect(Downstream<? super T> downstream) throws Exception;

}
