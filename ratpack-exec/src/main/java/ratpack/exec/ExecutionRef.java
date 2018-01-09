/*
 * Copyright 2018 the original author or authors.
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

import ratpack.registry.Registry;

import java.util.Optional;

/**
 * A reference to an {@link Execution} that is usable from outside of it.
 *
 * An execution ref is-a registry.
 * It is a read only view of the actual execution registry, while the execution is active.
 * Once the execution has completed, the registry is effectively empty.
 * Note that there are no guarantees that the registry is immutable.
 * The execution may mutate the registry or its entries.
 * Concurrent access via this reference is however safe.
 *
 * A ref avoids holding a long lived reference to the actual execution and its registry.
 * Therefore, holding a reference to an execution-ref in memory does not prevent the execution's
 * registry from being garbage collected.
 *
 * @see Execution#getParent()
 * @since 1.6
 */
public interface ExecutionRef extends Registry {

  /**
   * A ref to the execution that forked this execution.
   *
   * @throws IllegalStateException if this is a top level exception with no parent
   * @return a ref to the execution that forked this execution
   * @see #maybeParent()
   */
  ExecutionRef getParent() throws IllegalStateException;

  /**
   * A ref to the execution that forked this execution, if it has a parent.
   *
   * @return a ref to the execution that forked this execution
   */
  Optional<ExecutionRef> maybeParent();

  /**
   * Whether the execution this refers to is complete.
   *
   * This method is equivalent to {@link Execution#isComplete()}.
   *
   * @return whether the execution this refers to is complete
   */
  boolean isComplete();

}
