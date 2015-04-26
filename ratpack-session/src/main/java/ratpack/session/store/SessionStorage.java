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

package ratpack.session.store;

import ratpack.exec.Promise;
import java.util.Optional;
import java.util.Set;

/**
 * A marker concurrent map sub interface, to make retrieving the session storage from the service easier.
 * <p>
 * The session storage is not available for dependency injection via Guice.
 * It must be retrieved via service lookup.
 */
public interface SessionStorage  {

  <T> Promise<Optional<T>> get(String key, Class<T> type);

  Promise<Boolean> set(String key, Object value);

  Promise<Set<String>> getKeys();

  Promise<Integer> remove(String key);

  Promise<Integer> clear();
}
