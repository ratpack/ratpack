
package ratpack.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ratpack.registry.Registry;
import ratpack.test.embed.EmbeddedApplication;
import ratpack.test.embed.EmbeddedApplicationBuilder;

import static ratpack.spring.Spring.spring;

public class Example {

  private static EmbeddedApplication createApp() {
    final Registry springBackedRegistry = spring(ExampleSpringBootApp.class);

    return EmbeddedApplicationBuilder.builder().build(chain -> chain
      .register(springBackedRegistry)
      .handler(context -> {
        String helloBean = context.get(String.class);
        context.render(helloBean);
      }));
  }

  public static void main(String[] args) {
    try (EmbeddedApplication app = createApp()) {
      assert app.getHttpClient().getText().equals("hello");
    }
  }

  @Configuration
  public static class ExampleSpringBootApp {

    @Bean
    String hello() {
      return "hello";
    }

    public static void main(String[] args) {
      SpringApplication.run(ExampleSpringBootApp.class, args);
    }
  }

}
