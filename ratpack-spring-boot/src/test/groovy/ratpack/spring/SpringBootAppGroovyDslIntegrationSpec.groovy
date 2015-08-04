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

package ratpack.spring

import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ratpack.test.internal.RatpackGroovyDslSpec

class SpringBootAppGroovyDslIntegrationSpec extends RatpackGroovyDslSpec {
  def "Spring beans should be available for DI in Ratpack handlers"() {
    when:
    handlers {
      register Spring.spring(SampleSpringBootApp)
      path("foo") { String msg ->
        render msg
      }
    }

    then:
    getText("foo") == "hello"
  }
}

@Configuration
class SampleSpringBootApp {
  @Bean
  String hello() {
    "hello"
  }

  static void main(String[] args) {
    SpringApplication.run(SampleSpringBootApp, args)
  }
}
