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

package ratpack.pac4j.cookiesession

import org.pac4j.core.profile.UserProfile
import ratpack.pac4j.Pac4jModule
import ratpack.session.clientside.ClientSideSessionsModule
import ratpack.session.clientside.serializer.JavaValueSerializer
import ratpack.test.internal.RatpackGroovyDslSpec

//import static io.netty.handler.codec.http.HttpResponseStatus.FOUND
//import static io.netty.handler.codec.http.HttpHeaderNames.LOCATION

class CookieSessionSpec extends RatpackGroovyDslSpec {
  def "successful authorization"() {
    given:
    bindings {
      module ClientSideSessionsModule, {
        it.with {
          secretKey = "aaaaaaaaaaaaaaaa"
          // required to share the same session between app instances (in cluster)
          secretToken = "bbbbbb"
          // required for Pac4J UserProfile serialization
          valueSerializer = new JavaValueSerializer()
        }
      }
      /*module(new Pac4jModule(
        new FormClient("/login", new SimpleTestUsernamePasswordAuthenticator(), new UsernameProfileCreator()),
        new PathAuthorizer()
      ))*/
    }

    handlers {
      get("noauth") {
        def userProfile = request.maybeGet(UserProfile).orElse(null)
        response.send "noauth:" + userProfile?.attributes?.username
      }
      get("auth") {
        def userProfile = request.maybeGet(UserProfile).orElse(null)
        response.send "noauth:" + userProfile?.attributes?.username
      }
    }

    when:
    get("auth")
    def resp1 = response

    then:
    resp1 != null
    //resp1.statusCode == FOUND.code()
    //resp1.headers.get().contains("/login")
  }
}
