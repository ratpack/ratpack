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

package ratpack.path;

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.path.internal.DefaultPathBinderBuilder;

import java.util.Optional;

/**
 * A path binder binds to a request path, extracting information from it.
 * <p>
 * Path binders are the basis of path based “routing” in Ratpack, in conjunction with {@link Handlers#path(PathBinder, Handler)}.
 * It is not common to use or create a path binder directly.
 * Instead, methods such as {@link Chain#path(String, Handler)} create a binder from a string specification (using {@link #parse(String, boolean)}).
 * <p>
 * See <a href="../handling/Chain.html#path-binding">the section on path binding as part of the Chain documentation</a> for more information.
 *
 * @see Chain#path(String, Handler)
 * @see Handlers#path(String, Handler)
 * @see Handlers#path(PathBinder, Handler)
 * @see Handlers#prefix(String, Handler)
 */
public interface PathBinder {

  /**
   * Attempts to bind in the context of the given parent binding.
   * <p>
   * A binder may use whatever strategy it desires to decider whether or not it wants to
   * create a binding for the given path.
   *
   * @param parentBinding the parent binding
   * @return A binding if one could be created
   */
  Optional<PathBinding> bind(PathBinding parentBinding);

  /**
   * Creates a path binder by parsing the given path binding specification.
   * <p>
   * This method is used by methods such as {@link Chain#path(String, Class)}.
   * <p>
   * See <a href="../handling/Chain.html#path-binding">the section on path binding as part of the Chain documentation</a> for the format of the string.
   *
   * @param pathBindingSpec the path binding specification.
   * @param exhaustive whether the binder must match the entire unbound path (false for a prefix match)
   * @return a path binder constructed from the given path binding specification
   */
  static PathBinder parse(String pathBindingSpec, boolean exhaustive) {
    return DefaultPathBinderBuilder.parse(pathBindingSpec, exhaustive);
  }

  /**
   * Builds a path binder programmatically.
   *
   * @param exhaustive whether the binder must match the entire unbound path (false for a prefix match)
   * @param action the binder definition
   * @return a path binder
   * @throws Exception any thrown by {@code action}
   */
  static PathBinder of(boolean exhaustive, Action<? super PathBinderBuilder> action) throws Exception {
    return action.with(builder()).build(exhaustive);
  }

  /**
   * Creates a new path binder builder.
   *
   * @return a new path binder builder
   */
  static PathBinderBuilder builder() {
    return new DefaultPathBinderBuilder();
  }
}
