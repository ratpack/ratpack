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

package ratpack.spring;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBacking;

import java.util.Arrays;

/**
 * Static utility methods for using Spring in Ratpack applications.
 *
 */
public abstract class Spring {
  /**
   * Adapts a Spring ListableBeanFactory instance to Ratpack's Registry interface
   *
   * Spring ListableBeanFactory API doesn't current support looking up beans with generic parameterized types.
   * The adapted Registry instance doesn't support this because of this limitation. There is a
   * <a href="https://jira.spring.io/browse/SPR-12147">feature request</a> to add the generics functionality to
   * the Spring ListableBeanFactory API.
   *
   * @param beanFactory
   * @return
   */
  public static Registry registry(final ListableBeanFactory beanFactory) {
    return Registries.registry(new SpringRegistryBacking(beanFactory));
  }

  /**
   * Runs a Spring Boot application
   *
   * @param springApplicationClass Spring Boot Application class
   * @param args arguments to pass to application
   * @return Ratpack Registry instance that looks up dependencies in the Spring Boot Application's context
   */
  public static Registry run(Class<?> springApplicationClass, String... args) {
    SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(springApplicationClass);
    springApplicationBuilder.main(springApplicationClass);
    return run(springApplicationBuilder, args);
  }

  /**
   * Runs a Spring Boot application
   *
   * @param springApplicationBuilder Spring Boot SpringApplicationBuilder instance
   * @param args arguments to pass to application
   * @return Ratpack Registry instance that looks up dependencies in the Spring Boot Application's context
   */
  public static Registry run(SpringApplicationBuilder springApplicationBuilder, String... args) {
    return registry(springApplicationBuilder.run(args));
  }

  private static class SpringRegistryBacking implements RegistryBacking {
    private final ListableBeanFactory beanFactory;

    public SpringRegistryBacking(ListableBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    public <T> Iterable<Supplier<? extends T>> provide(TypeToken<T> type) {
      return FluentIterable.from(Arrays.asList(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
        type.getRawType()))).transform(new Function<String, Supplier<? extends T>>() {
          @Override
          public Supplier<? extends T> apply(final String beanName) {
            return new Supplier<T>() {
              @Override
              public T get() {
                @SuppressWarnings("unchecked") T bean = (T) beanFactory.getBean(beanName);
                return bean;
              }
            };
          }
        });
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
}
