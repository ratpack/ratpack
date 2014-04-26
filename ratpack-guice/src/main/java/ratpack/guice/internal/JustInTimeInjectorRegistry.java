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
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;

import java.util.List;

public class JustInTimeInjectorRegistry implements Registry {

  private final Injector injector;

  public JustInTimeInjectorRegistry(Injector injector) {
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

  @Override
  public <O> List<O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  @Override
  public <O> List<O> getAll(TypeToken<O> type) {
    @SuppressWarnings("unchecked") TypeLiteral<O> typeLiteral = (TypeLiteral<O>) TypeLiteral.get(type.getType());
    return GuiceUtil.ofType(injector, typeLiteral);
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    return get(TypeToken.of(type));
  }

  @Override
  public <O> O get(TypeToken<O> type) throws NotInRegistryException {
    @SuppressWarnings("unchecked") TypeLiteral<O> typeLiteral = (TypeLiteral<O>) TypeLiteral.get(type.getType());
    try {
      return injector.getInstance(Key.get(typeLiteral));
    } catch (ConfigurationException e) {
      throw new NotInRegistryException(type);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    JustInTimeInjectorRegistry that = (JustInTimeInjectorRegistry) o;

    return injector.equals(that.injector);
  }

  @Override
  public int hashCode() {
    return injector.hashCode();
  }
}
