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

package ratpack.pac4j.openid

import com.google.inject.Module
import org.pac4j.core.profile.UserProfile
import org.pac4j.openid.profile.google.GoogleOpenIdProfile
import ratpack.groovy.test.embed.ClosureBackedEmbeddedApplication
import ratpack.session.SessionModule
import ratpack.session.store.MapSessionsModule
import ratpack.test.embed.BaseDirBuilder

class RatpackOpenIdTestApplication extends ClosureBackedEmbeddedApplication {
  RatpackOpenIdTestApplication(int consumerPort, BaseDirBuilder baseDirBuilder, Module... additionalModules) {
    super(baseDirBuilder)
    launchConfig {
      port(consumerPort)
      publicAddress(new URI("http://localhost:${consumerPort}"))
    }

    bindings {
      add \
        new SessionModule(),
        new MapSessionsModule(10, 5)

      add additionalModules
    }

    handlers {
      get("noauth") {
        def typedUserProfile = request.maybeGet(GoogleOpenIdProfile)
        def genericUserProfile = request.maybeGet(UserProfile)
        response.send "noauth:${typedUserProfile?.email}:${genericUserProfile?.attributes?.email}"
      }
      get("auth") {
        def typedUserProfile = request.maybeGet(GoogleOpenIdProfile)
        def genericUserProfile = request.maybeGet(UserProfile)
        response.send "auth:${typedUserProfile.email}:${genericUserProfile?.attributes?.email}"
      }
      get("error") {
        response.send "An error was encountered."
      }
    }
  }
}
