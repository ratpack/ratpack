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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ratpack.groovy.Groovy.groovyTemplate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ratpack.groovy.template.TextTemplateModule;
import ratpack.core.handling.Context;
import ratpack.core.handling.Handler;
import ratpack.core.server.RatpackServer;
import ratpack.spring.config.EnableRatpack;
import ratpack.spring.config.RatpackProperties;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TemplateTests {

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Autowired
  private RatpackServer server;

  @Test
  public void contextLoads() {
    String body = restTemplate.getForObject("http://localhost:" + server.getBindPort(), String.class);
    assertTrue(body.contains("<body>Home"), "Wrong body" + body);
  }

  @Configuration
  @EnableRatpack
  @EnableAutoConfiguration
  protected static class Application {

    @Bean
    public Handler handler() {
      return new Handler() {
        @Override
        public void handle(Context context) throws Exception {
          context.render(groovyTemplate("index.html"));
        }
      };
    }

    @Bean
    public TextTemplateModule textTemplateGuiceModule(RatpackProperties ratpack) {
      TextTemplateModule module = new TextTemplateModule();
      module.configure(config -> {
        config.setTemplatesPath(ratpack.getTemplatesPath());
        config.setStaticallyCompile(ratpack.isStaticallyCompile());
      });
      return module;
    }

    public static void main(String[] args) throws Exception {
      SpringApplication.run(Application.class, args);
    }

  }

}
