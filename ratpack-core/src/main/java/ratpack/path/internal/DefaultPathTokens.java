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

package ratpack.path.internal;

import com.google.common.collect.ImmutableMap;
import ratpack.path.PathTokens;
import ratpack.util.internal.DefaultTypeCoercingMap;

public class DefaultPathTokens extends DefaultTypeCoercingMap<String> implements PathTokens {

  public DefaultPathTokens(ImmutableMap<String, String> delegate) {
    super(delegate);
  }

  public static PathTokens empty() {
    return new DefaultPathTokens(ImmutableMap.of());
  }

}
