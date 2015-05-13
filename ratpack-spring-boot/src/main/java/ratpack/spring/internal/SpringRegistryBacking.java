/*
 * Copyright 2014 the original author or authors.
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

package ratpack.spring.internal;

import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import ratpack.registry.RegistryBacking;
import ratpack.util.Types;

public class SpringRegistryBacking implements RegistryBacking {
  private final ListableBeanFactory beanFactory;

  public SpringRegistryBacking(ListableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public <T> Iterable<Supplier<? extends T>> provide(TypeToken<T> type) {
    return FluentIterable.from(
      ImmutableList.copyOf(
        BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type.getRawType())
      ).reverse()
    ).transform(beanName ->
      () -> Types.cast(beanFactory.getBean(beanName))
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SpringRegistryBacking that = (SpringRegistryBacking) o;

    return beanFactory.equals(that.beanFactory);
  }

  @Override
  public int hashCode() {
    return beanFactory.hashCode();
  }
}
