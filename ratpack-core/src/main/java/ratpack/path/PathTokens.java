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

import ratpack.util.TypeCoercingMap;

/**
 * A marker interface for the contextual object that represents the tokens extracted from the request path.
 *
 * @see ratpack.handling.Chain#path(String, ratpack.handling.Handler)
 */
public interface PathTokens extends TypeCoercingMap<String> {

}
