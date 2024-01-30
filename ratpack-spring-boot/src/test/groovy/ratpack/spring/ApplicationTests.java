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
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.server.RatpackServer;
import ratpack.spring.ApplicationTests.Application;
import ratpack.spring.config.EnableRatpack;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static ratpack.jackson.Jackson.json;

@ExtendWith(SpringExtension.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest({"debug=true", "server.port=0"})
public class ApplicationTests {

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Autowired
  private RatpackServer server;

  @Test
  public void homePage() {
    assertEquals(
      "{\"message\":\"Hello World\"}",
      restTemplate.getForObject("http://localhost:" + server.getBindPort(), String.class));
  }

  @Test
  public void notFound() {
    ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + server.getBindPort() + "/none",
      String.class);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertNull(response.getBody(), "Default 404 handler has null body");
  }

  @Configuration
  @EnableAutoConfiguration
  @EnableRatpack
  @Import(MessageService.class)
  protected static class Application {

    @Autowired
    private MessageService service;

    @Bean
    public Action<Chain> handler() {
      return chain -> chain.get(context -> {
        // We're not using the registry here directly but it's good to
        // confirm that it contains our service:
        assertNotNull(context.get(MessageService.class));
        context.render(json(Collections.singletonMap("message", service.message())));
      });
    }

    public static void main(String[] args) throws Exception {
      SpringApplication.run(Application.class, args);
    }

  }

  @Service
  protected static class MessageService {

    public String message() {
      return "Hello World";
    }

  }

}
