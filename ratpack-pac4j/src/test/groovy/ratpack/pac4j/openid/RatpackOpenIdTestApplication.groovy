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
    modules {
      register new SessionModule()
      register new MapSessionsModule(10, 5)
      for (module in additionalModules) {
        register module
      }
    }
    handlers {
      get("noauth") {
        def userProfile = request.maybeGet(GoogleOpenIdProfile)
        response.send "noauth:${userProfile?.email}"
      }
      get("auth") {
        def userProfile = request.maybeGet(GoogleOpenIdProfile)
        response.send "auth:${userProfile.email}"
      }
      get("error") {
        response.send "An error was encountered."
      }
    }
  }
}
