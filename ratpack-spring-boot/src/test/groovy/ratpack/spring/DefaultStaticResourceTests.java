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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ratpack.server.RatpackServer;
import ratpack.spring.DefaultStaticResourceTests.Application;
import ratpack.spring.config.EnableRatpack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@IntegrationTest("server.port=0")
public class DefaultStaticResourceTests {

  private TestRestTemplate restTemplate = new TestRestTemplate();

  @Autowired
  private RatpackServer server;

  @Test
  public void contextLoads() {
    ResponseEntity<String> mainCssResponse = restTemplate
      .getForEntity("http://localhost:" + server.getBindPort() + "/main.css",
        String.class);
    assertEquals(HttpStatus.OK, mainCssResponse.getStatusCode());
    assertTrue("Wrong body" + mainCssResponse.getBody(), mainCssResponse.getBody().contains("background: red;"));

    ResponseEntity<String> publicCssResponse = restTemplate
      .getForEntity("http://localhost:" + server.getBindPort() + "/public.css",
        String.class);
    assertEquals(HttpStatus.OK, publicCssResponse.getStatusCode());
    assertTrue("Wrong body" + publicCssResponse.getBody(), publicCssResponse.getBody().contains("color: blue;"));
  }

  @Configuration
  @EnableRatpack
  @EnableAutoConfiguration
  protected static class Application {
    public static void main(String[] args) throws Exception {
      SpringApplication.run(Application.class, args);
    }
  }

}
