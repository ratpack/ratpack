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

package org.ratpackframework.guice.internal;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.ChildRegistrySupport;

import java.util.List;

public class JustInTimeInjectorChildRegistry extends ChildRegistrySupport<Object> {

  private final Injector injector;

  public JustInTimeInjectorChildRegistry(Registry<Object> parent, Injector injector) {
    super(parent);
    this.injector = injector;
  }

  @Override
  protected String describe() {
    return "JustInTimeInjectorChildRegistry{" + injector + '}';
  }

  @Override
  protected <O extends Object> O doMaybeGet(Class<O> type) {
    try {
      return injector.getInstance(type);
    } catch (ConfigurationException ignore) {
      return null;
    }
  }

  @Override
  protected <O extends Object> List<O> doChildGetAll(Class<O> type) {
    return GuiceUtil.ofType(injector, type);
  }
}
