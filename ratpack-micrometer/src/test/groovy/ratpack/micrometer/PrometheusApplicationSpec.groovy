/*
 * Copyright 2018 the original author or authors.
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
package ratpack.micrometer


import ratpack.groovy.test.embed.GroovyEmbeddedApp
import spock.lang.Specification

class PrometheusApplicationSpec extends Specification {

  def "publish prometheus metrics on endpoint /metrics"() {
    when:
    def app = GroovyEmbeddedApp.ratpack {
      bindings {
        module(PrometheusMicrometerModule, {
          it.application = "my-app"
          it.groups = [:]
        })
        module(PrometheusHandlerModule)
      }
    }

    then:
    def response = app.httpClient.get("metrics")
    response.status.code == 200
    response.body.text.isEmpty()
  }
}




