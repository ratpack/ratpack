/*
 * Copyright 2013 the original author or authors.
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

/**
 * A single argument function.
 *
 * @param <I> the type of the input
 * @param <O> the type of the output
 */
public interface Function<I, O> {

  /**
   * The function implementation.
   *
   * @param i the input to the function
   * @return the output of the function
   * @throws Exception any
   */
  O apply(I i) throws Exception;

}
