/*
 * Copyright 2015 the original author or authors.
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

package ratpack.pac4j

import org.pac4j.core.profile.UserProfile
import org.pac4j.http.client.BasicAuthClient
import org.pac4j.http.credentials.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.http.profile.UsernameProfileCreator
import ratpack.session.SessionModule
import ratpack.test.internal.RatpackGroovyDslSpec

class AuthenticationSpec extends RatpackGroovyDslSpec {

  def "secure handlers"() {
    given:
    serverConfig {
      development true
    }
    bindings {
      module SessionModule
    }
    handlers {
      all RatpackPac4j.authenticator(new BasicAuthClient(new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator()))
      prefix('require-auth') {
        all RatpackPac4j.requireAuth(BasicAuthClient)
        get {
          render "Hello " + get(UserProfile).getId()
        }
      }
      get {
        render "no auth required"
      }
    }
    httpClient.requestSpec { r ->
      r.redirects 1
    }

    expect:
    getText() == 'no auth required'
    get('require-auth').statusCode == 401
    requestSpec { r ->
      r.basicAuth('user', 'user')
    }.getText('require-auth') == 'Hello user'
  }
}
