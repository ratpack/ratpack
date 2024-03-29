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
import ratpack.func.Action;
import ratpack.core.handling.Chain;
import ratpack.core.server.RatpackServer;
import ratpack.spring.config.EnableRatpack;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StaticResourceTests {

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Autowired
  private RatpackServer server;

  @Test
  public void contextLoads() {
    String body = restTemplate
      .getForObject("http://localhost:" + server.getBindPort() + "/root/main.css",
        String.class);
    assertTrue(body.contains("background"), "Wrong body" + body);
  }

  @Configuration
  @EnableRatpack
  @EnableAutoConfiguration
  protected static class Application {

    @Bean
    public Action<Chain> handlers() {
      return chain -> chain
          .files(f -> f
            .path("root")
            .dir("root")
            .indexFiles("index.html"));
    }

    public static void main(String[] args) throws Exception {
      SpringApplication.run(Application.class, args);
    }

  }

}
