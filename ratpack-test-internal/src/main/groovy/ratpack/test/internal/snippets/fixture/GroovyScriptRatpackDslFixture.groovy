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

package ratpack.test.internal.snippets.fixture

import ratpack.func.Block
import ratpack.groovy.Groovy
import ratpack.groovy.internal.ClosureUtil
import ratpack.groovy.internal.capture.RatpackDslBacking
import ratpack.groovy.internal.capture.RatpackDslClosures
import ratpack.guice.Guice
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig

class GroovyScriptRatpackDslFixture extends GroovyScriptFixture {

  @Override
  void around(Block action) throws Exception {
    def closures = RatpackDslClosures.capture({ new RatpackDslBacking(it) }, null, action)
    RatpackServer.start {
      it.serverConfig(ServerConfig.embedded())
      it.registry(Guice.registry {
        ClosureUtil.configureDelegateFirst(it, closures.bindings)
      })
      it.handlers(Groovy.chain(closures.handlers))
    }
  }
}
