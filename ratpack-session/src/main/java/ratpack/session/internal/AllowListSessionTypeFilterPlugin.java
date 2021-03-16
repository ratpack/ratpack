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

import com.google.common.collect.Lists;
import ratpack.session.SessionTypeFilterPlugin;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AllowListSessionTypeFilterPlugin implements SessionTypeFilterPlugin {

  private final Set<String> types = new HashSet<>();

  public AllowListSessionTypeFilterPlugin(Collection<String> types) {
    this.types.addAll(types);
  }

  public static AllowListSessionTypeFilterPlugin ofClasses(Class<?>... classes) {
    return new AllowListSessionTypeFilterPlugin(Lists.transform(Arrays.asList(classes), Class::getName));
  }

  @Override
  public boolean allow(String type) {
    return types.contains(type);
  }

}
