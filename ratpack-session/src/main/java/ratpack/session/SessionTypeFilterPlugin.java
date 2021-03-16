/*
 * Copyright 2021 the original author or authors.
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

package ratpack.session;

/**
 * A marker interface for type filter implementations that should be composed into the type filter.
 * <p>
 * The default module binding for {@link SessionTypeFilter} composes all of the multi-bound instances
 * of this type together. This allows modules to conveniently <i>add</i> types that are safe.
 * <p>
 * Most applications do not need to use this class directly.
 * Instead they can use {@link SessionModule#allowTypes}.
 *
 * @since 1.9
 */
public interface SessionTypeFilterPlugin extends SessionTypeFilter {

}
