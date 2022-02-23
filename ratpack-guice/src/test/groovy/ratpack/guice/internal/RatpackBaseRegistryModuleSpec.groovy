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

package ratpack.guice.internal

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import io.netty.buffer.ByteBufAllocator
import org.reactivestreams.Publisher
import ratpack.core.error.ClientErrorHandler
import ratpack.core.error.ServerErrorHandler
import ratpack.exec.ExecController
import ratpack.exec.Promise
import ratpack.config.FileSystemBinding
import ratpack.core.file.MimeTypes
import ratpack.core.form.FormParseOpts
import ratpack.exec.internal.ExecControllerInternal
import ratpack.guice.Guice
import ratpack.core.handling.Redirector
import ratpack.core.http.client.HttpClient
import ratpack.core.impose.Impositions
import ratpack.core.parse.Parser
import ratpack.core.render.Renderable
import ratpack.core.render.Renderer
import ratpack.core.server.PublicAddress
import ratpack.core.server.RatpackServer
import ratpack.core.server.ServerConfig
import ratpack.core.server.internal.ServerRegistry
import ratpack.test.internal.BaseRatpackSpec
import spock.lang.Subject

import java.nio.file.Path

@Subject(RatpackBaseRegistryModule)
@SuppressWarnings("UnnecessaryObjectReferences")
class RatpackBaseRegistryModuleSpec extends BaseRatpackSpec {
  def "injector contains bindings based on base registry"() {
    when:
    def ratpackServer = Mock(RatpackServer)
    def execController = (ExecControllerInternal) ExecController.of { e ->
      e.compute { c ->
        c.threads(4)
      }
    }
    def serverConfig = ServerConfig.builder().build()
    def baseRegistry = ServerRegistry.serverRegistry(ratpackServer, Impositions.none(), execController, serverConfig, Guice.registry {})
    def injector = baseRegistry.get(Injector)

    then:
    injector.getInstance(ServerConfig) == serverConfig
    injector.getInstance(ByteBufAllocator) == baseRegistry.get(ByteBufAllocator)
    injector.getInstance(ExecController) == baseRegistry.get(ExecController)
    injector.getInstance(MimeTypes) == baseRegistry.get(MimeTypes)
    injector.getInstance(PublicAddress) == baseRegistry.get(PublicAddress)
    injector.getInstance(Redirector) == baseRegistry.get(Redirector)
    injector.getInstance(ClientErrorHandler) == baseRegistry.get(ClientErrorHandler)
    injector.getInstance(ServerErrorHandler) == baseRegistry.get(ServerErrorHandler)
    injector.getInstance(RatpackServer) == ratpackServer
    injector.getInstance(Key.get(new TypeLiteral<Renderer<Path>>() {}))
    injector.getInstance(Key.get(new TypeLiteral<Renderer<Promise<?>>>() {}))
    injector.getInstance(Key.get(new TypeLiteral<Renderer<Publisher<?>>>() {}))
    injector.getInstance(Key.get(new TypeLiteral<Renderer<Renderable>>() {}))
    injector.getInstance(Key.get(new TypeLiteral<Renderer<CharSequence>>() {}))
    injector.getInstance(Key.get(new TypeLiteral<Parser<FormParseOpts>>() {}))
    !injector.getExistingBinding(Key.get(FileSystemBinding))
    injector.getInstance(HttpClient)
  }
}
