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

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import ratpack.registry.Registry;
import ratpack.spring.internal.SpringBackedRegistry;

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
  public static Registry registry(ListableBeanFactory beanFactory) {
    return new SpringBackedRegistry(beanFactory);
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
}
