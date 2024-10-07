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
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.server.RatpackServer;
import ratpack.spring.JsonTests.Application;
import ratpack.spring.config.EnableRatpack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static ratpack.jackson.Jackson.fromJson;
import static ratpack.jackson.Jackson.json;

@ExtendWith(SpringExtension.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest("server.port=0")
public class JsonTests {

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Autowired
  private RatpackServer server;

  @Test
  public void get() {
    String body = restTemplate.getForObject("http://localhost:" + server.getBindPort(), String.class);
    assertTrue(body.contains("{"), "Wrong body" + body);
    assertFalse(body.toLowerCase().contains("<html"), "Wrong body" + body);
  }

  @Test
  public void post() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity<Map<String, String>>(Collections.singletonMap("foo", "bar"),
      headers);
    ResponseEntity<String> result = restTemplate.postForEntity("http://localhost:" + server.getBindPort(), entity,
      String.class);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    String body = restTemplate.getForObject("http://localhost:" + server.getBindPort(), String.class);
    assertTrue(body.contains("foo"), "Wrong body" + body);
  }

  @Configuration
  @EnableRatpack
  @EnableAutoConfiguration
  protected static class Application {

    private Map<String, Object> map = new LinkedHashMap<String, Object>();

    @Bean
    public Action<Chain> chain() {
      return chain -> chain.all(handler());
    }

    @SuppressWarnings("unchecked")
    @Bean
    public Handler handler() {
      // @formatter:off
      return context -> context.byMethod(spec -> spec.get(() -> context.render(json(map))).post(() -> {
        context.parse(fromJson(Map.class)).then(m -> {
          map.putAll(m);
          context.render(json(map));
        });
      }));
      // @formatter:on
    }

    public static void main(String[] args) throws Exception {
      SpringApplication.run(Application.class, args);
    }

  }

}
