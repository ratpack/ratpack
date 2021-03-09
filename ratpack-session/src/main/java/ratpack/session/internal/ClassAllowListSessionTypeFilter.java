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

package ratpack.session.internal;

import com.google.common.collect.Sets;
import ratpack.session.SessionTypeFilter;

import java.util.Collection;
import java.util.Set;

public class ClassAllowListSessionTypeFilter implements SessionTypeFilter {

  private final Set<Class<?>> types;

  public ClassAllowListSessionTypeFilter(Collection<Class<?>> types) {
    this.types = Sets.newIdentityHashSet();
    this.types.addAll(types);
  }

  @Override
  public boolean allow(Class<?> type) {
    return types.contains(type);
  }

}
