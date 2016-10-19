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
import ratpack.spring.internal.SpringRegistryBacking;

/**
 * Methods to facilitate integrating <a href="http://projects.spring.io/spring-boot">Spring Boot</a> with Ratpack.
 *
 * <pre class="java">{@code
 * import org.springframework.boot.SpringApplication;
 * import org.springframework.context.annotation.Bean;
 * import org.springframework.context.annotation.Configuration;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static ratpack.spring.Spring.spring;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.fromHandlers(chain -> chain
 *       .register(spring(ExampleSpringBootApp.class))
 *       .all(context -> {
 *         String helloBean = context.get(String.class);
 *         context.render(helloBean);
 *       })
 *     ).test(httpClient -> {
 *       assertEquals("hello", httpClient.getText());
 *     });
 *   }
 *
 *   {@literal @}Configuration
 *   public static class ExampleSpringBootApp {
 *     {@literal @}Bean
 *     String hello() {
 *       return "hello";
 *     }
 *
 *     public static void main(String... args) {
 *       SpringApplication.run(ExampleSpringBootApp.class, args);
 *     }
 *   }
 * }
 * }</pre>
 */
public abstract class Spring {

  /**
   * Creates a registry backed by the given bean factory.
   * <p>
   * <b>Note:</b> Spring ListableBeanFactory API doesn't current support looking up beans with parameterized types.
   * The adapted {@code Registry} instance doesn't support this because of this limitation.
   * There is a <a href="https://jira.spring.io/browse/SPR-12147">feature request</a> to add the generics functionality to the Spring ListableBeanFactory API.
   *
   * @param beanFactory the bean factory to back the registry
   * @return a registry that retrieves objects from the given bean factory
   */
  public static Registry spring(ListableBeanFactory beanFactory) {
    return Registry.backedBy(new SpringRegistryBacking(beanFactory));
  }

  /**
   *  Creates a registry backed by the given Spring Boot application class.
   *
   * @param clazz a Spring Boot application class
   * @param args any arguments to pass to the application
   * @return a registry that retrieves objects from the given application's bean factory
   * @see #spring(org.springframework.beans.factory.ListableBeanFactory)
   */
  public static Registry spring(Class<?> clazz, String... args) {
    SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(clazz);
    springApplicationBuilder.main(clazz);
    return spring(springApplicationBuilder, args);
  }

  /**
   * Creates a registry backed by the given Spring Boot application builder.
   *
   * @param builder a Spring Boot application builder
   * @param args any arguments to pass to the application
   * @return a registry that retrieves objects from the given application's bean factory
   * @see #spring(org.springframework.beans.factory.ListableBeanFactory)
   */
  public static Registry spring(SpringApplicationBuilder builder, String... args) {
    return spring(builder.run(args));
  }

}
