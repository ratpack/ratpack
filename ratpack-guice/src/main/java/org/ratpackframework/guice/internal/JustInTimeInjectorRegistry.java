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

import com.google.inject.Injector;
import org.ratpackframework.registry.internal.RegistrySupport;

import java.util.List;

public class JustInTimeInjectorRegistry extends RegistrySupport<Object> {

  private final Injector injector;

  public JustInTimeInjectorRegistry(Injector injector) {
    this.injector = injector;
  }

  @Override
  protected <T> T doMaybeGet(Class<T> type) {
    return injector.getInstance(type);
  }

  @Override
  public <O extends Object> List<O> getAll(Class<O> type) {
    return GuiceUtil.ofType(injector, type);
  }

  @Override
  public String toString() {
    return "JustInTimeInjectorRegistry{" + injector + '}';
  }

}
