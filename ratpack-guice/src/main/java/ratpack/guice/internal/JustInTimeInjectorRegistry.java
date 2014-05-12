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

package ratpack.guice.internal;

import com.google.common.reflect.TypeToken;
import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class JustInTimeInjectorRegistry extends InjectorBackedRegistry {

  private final Injector injector;

  public JustInTimeInjectorRegistry(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  public <T> T maybeGet(Class<T> type) {
    return maybeGet(TypeToken.of(type));
  }

  public <T> T maybeGet(TypeToken<T> type) {
    @SuppressWarnings("unchecked") TypeLiteral<T> typeLiteral = (TypeLiteral<T>) TypeLiteral.get(type.getType());
    try {
      return injector.getInstance(Key.get(typeLiteral));
    } catch (ConfigurationException e) {
      return null;
    }
  }

}
