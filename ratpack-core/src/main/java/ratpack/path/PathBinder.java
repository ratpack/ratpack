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

import ratpack.api.Nullable;

/**
 * A path binder binds to a request path, extracting information from it.
 * <p>
 * They are used to conditionally execute handlers based on the request path.
 *
 * @see ratpack.handling.Handlers#path(String, java.util.List)
 * @see ratpack.handling.Handlers#path(PathBinder, java.util.List)
 * @see ratpack.handling.Handlers#prefix(String, java.util.List)
 */
public interface PathBinder {

  /**
   * Creates a binding for the given path, if this binder can bind to this path.
   * <p>
   * A binder may use whatever strategy it desires to decider whether or not it wants to
   * create a binding for the given path.
   *
   * @param path The path to maybe create a binding for
   * @param parentBinding The most recent upstream binding for this path, or null if there is no upstream binding
   * @return A binding if one could be created, otherwise null.
   */
  @Nullable
  PathBinding bind(String path, @Nullable PathBinding parentBinding);

}
