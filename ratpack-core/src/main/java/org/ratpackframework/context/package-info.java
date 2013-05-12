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
 * Handlers communicate with each other by binding a "context" on to each {@link org.ratpackframework.routing.Exchange}.
 *
 * A context is just an object that may be able to provide an object of a given type. Typically, upstream handlers add
 * objects to the context for downstream handlers to use.
 *
 * This package
 *
 * @see org.ratpackframework.context.Context
 */
package org.ratpackframework.context;
