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

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import ratpack.registry.NotInRegistryException;
import ratpack.registry.Registry;

import java.util.List;

public class JustInTimeInjectorRegistry implements Registry {

  private final Injector injector;

  public JustInTimeInjectorRegistry(Injector injector) {
    this.injector = injector;
  }

  public <T> T maybeGet(Class<T> type) {
    try {
      return injector.getInstance(type);
    } catch (ConfigurationException e) {
      return null;
    }
  }

  @Override
  public <O> List<O> getAll(Class<O> type) {
    return GuiceUtil.ofType(injector, type);
  }

  @Override
  public <O> O get(Class<O> type) throws NotInRegistryException {
    try {
      return injector.getInstance(type);
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
