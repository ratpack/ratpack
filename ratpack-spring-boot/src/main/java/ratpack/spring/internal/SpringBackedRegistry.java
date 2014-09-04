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

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import ratpack.registry.internal.CachingSupplierRegistry;

import java.util.Arrays;
import java.util.List;

public class SpringBackedRegistry extends CachingSupplierRegistry {
  private final ListableBeanFactory beanFactory;

  public SpringBackedRegistry(final ListableBeanFactory beanFactory) {
    super(new Function<TypeToken<?>, List<? extends Supplier<?>>>() {
      @Override
      public List<? extends Supplier<?>> apply(TypeToken<?> input) {
        return FluentIterable.from(Arrays.asList(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
          input.getRawType()))).transform(new Function<String, Supplier<?>>() {
            @Override
            public Supplier<?> apply(final String beanName) {
              return new Supplier<Object>() {
                @Override
                public Object get() {
                  return beanFactory.getBean(beanName);
                }
              };
            }
          }).toList();
      }
    });
    this.beanFactory = beanFactory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SpringBackedRegistry that = (SpringBackedRegistry) o;

    return beanFactory.equals(that.beanFactory);
  }

  @Override
  public int hashCode() {
    return beanFactory.hashCode();
  }
}
