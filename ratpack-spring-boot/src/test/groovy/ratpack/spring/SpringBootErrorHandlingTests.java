/*
 * Copyright 2016 the original author or authors.
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
import ratpack.core.error.ServerErrorHandler;
import ratpack.func.Action;
import ratpack.core.handling.Chain;
import ratpack.core.server.RatpackServer;
import ratpack.spring.config.EnableRatpack;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringBootErrorHandlingTests {

  @Autowired
  RatpackServer server;

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Test
  public void customErrorhandler() {
    assertEquals("!!", restTemplate.getForObject("http://localhost:" + server.getBindPort(), String.class));
  }

  @Configuration
  @EnableAutoConfiguration
  @EnableRatpack
  protected static class Application {

    @Bean
    public Action<Chain> handler() {
      return chain -> chain.get(context -> {
        throw new RuntimeException("!!");
      });
    }

    @Bean
    public ServerErrorHandler serverErrorHandler() {
      return (context, error) -> context.getResponse().status(200).send(error.getMessage());
    }

    public static void main(String[] args) throws Exception {
      SpringApplication.run(Application.class, args);
    }

  }
}
