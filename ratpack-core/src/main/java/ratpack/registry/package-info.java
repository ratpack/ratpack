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

/**
 * Registries hold objects that can be retrieved via type, and are a key aspect of Ratpack applications.
 * <p>
 * Registries are primarily used for inter {@link ratpack.handling.Handler} communication in request processing.
 * The {@link ratpack.handling.Context} object that handles operate on implements the {@link ratpack.registry.Registry} interface.
 * <p>
 * See {@link ratpack.registry.Registry} for examples of how registries are used by handlers.
 *
 * @see ratpack.registry.Registry
 */
package ratpack.registry;
