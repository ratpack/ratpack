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
import ratpack.registry.internal.CachingBackedRegistry;

import java.util.Optional;

public class JustInTimeInjectorRegistry extends CachingBackedRegistry {

  private final Injector injector;

  public JustInTimeInjectorRegistry(Injector injector) {
    super(new InjectorRegistryBacking(injector));
    this.injector = injector;
  }

  public <T> Optional<T> maybeGet(TypeToken<T> type) {
    @SuppressWarnings("unchecked") TypeLiteral<T> typeLiteral = (TypeLiteral<T>) TypeLiteral.get(type.getType());
    try {
      return Optional.of(injector.getInstance(Key.get(typeLiteral)));
    } catch (ConfigurationException e) {
      return Optional.empty();
    }
  }

}
