# Spring Boot

The `ratpack-spring-boot` extension provides integration with [Spring Boot](http://projects.spring.io/spring-boot).
There are two main features in this library: one allows a Ratpack server registry to be created from a Spring `ApplicationContext`, and the other allows Ratpack itself to be embedded in a Spring Boot application (making the `ApplicationContext` automatically part of the server registry).

## The Spring convenience class

In a vanilla Ratpack application you can create a registry easily using the [`Spring`](api/ratpack/spring/Spring.html) convenience class.
This is an alternative or a complement to Guice dependency injection because it works more or less the same way and Ratpack allows registries to be chained together quite conveniently. 

Here's an example of a main class that uses this API to configure the application.
 
```language-java hello-world
package my.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ratpack.server.RatpackServer;

import static ratpack.spring.Spring.spring;

public class Main {
  public static void main(String... args) throws Exception {
    RatpackServer.start(server -> server
      .registry(spring(MyConfiguration.class))
      .handlers(chain -> chain
        .get(ctx -> ctx
          .render("Hello " + ctx.get(Service.class).message()))
        .get(":message", ctx -> ctx
          .render("Hello " + ctx.getPathTokens().get("message") + "!")
        )
      )
    );
  }
}

@Configuration
class MyConfiguration {
  @Bean
  public Service service() {
    return () -> "World!";
  }
}

interface Service {
  String message();
}
```

The `Spring.spring()` method creates an `ApplicationContext` and adapts it to the Ratpack `Registry` interface. 

NOTE: The Spring `ListableBeanFactory` API doesn't current support looking up beans with parameterized types.
The adapted `Registry` therefore instance doesn't support this because of this limitation.
There is a [feature request](https://jira.spring.io/browse/SPR-12147) to add the generics functionality to the Spring `ListableBeanFactory` API.

## Embedding Ratpack in a Spring Boot application

As an alternative to embedding Spring (as a `Registry`) in a Ratpack application, you can do the opposite: embed Ratpack as a server in Spring Boot, providing a nice
alternative to the Servlet containers that Spring Boot supports. 
The core of the feature set is an annotation [`@EnableRatpack`](api/ratpack/spring/config/EnableRatpack.html) which you add to a Spring configuration class in order to start Ratpack. 
Then you can declare handlers as `@Beans` of type `Action<Chain>`, for example:

```java
package my.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.spring.config.EnableRatpack;

@SpringBootApplication
@EnableRatpack
public class Main {

  @Bean
  public Action<Chain> home() {
    return chain -> chain
      .get(ctx -> ctx
        .render("Hello " + service().message())
      );
  }

  @Bean
  public Service service() {
    return () -> "World!";
  }

  public static void main(String... args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

}

interface Service {
  String message();
}
```

TIP: Ratpack will register handlers automatically for static content in the classpath under "/public" or "/static" (just like a regular Spring Boot application). 

### Re-using existing Guice modules

If Ratpack is embedded in a Spring application it can be helpful to re-use existing Guice modules, e.g. for template rendering.
To do this just include a `@Bean` of type `Module`.
For example with the `ThymeleafModule` for Thymeleaf support:

```java
package my.app;

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.thymeleaf.ThymeleafModule;
import ratpack.spring.config.EnableRatpack;

import static ratpack.thymeleaf.Template.thymeleafTemplate;

@SpringBootApplication
@EnableRatpack
public class Main {

  @Bean
  public Action<Chain> home(Service service) {
    return chain -> chain.get(ctx -> ctx
      .render(thymeleafTemplate("myTemplate", 
        m -> m.put("key", "Hello " + service.message())))
    );
  }

  @Bean
  public ThymeleafModule thymeleafModule() {
    return new ThymeleafModule();
  }

  @Bean
  public Service service() {
    return () -> "World!";
  }

  public static void main(String... args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

}
```
