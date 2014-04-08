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

package ratpack.handling;

import ratpack.func.Action;

import java.lang.Exception;

/**
 * Convenience subclass for {@link Action Action<Chain>} implementations.
 */
public abstract class ChainAction implements Action<Chain> {

  /**
   * Adds to the given chain.
   *
   * @param chain The chain to add handlers to
   * @throws Exception any exception thrown while trying to add handlers to the chain
   */
  public abstract void execute(Chain chain) throws Exception;

}
