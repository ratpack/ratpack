/*
 * Copyright 2013 the original author or authors.
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

package ratpack.remote

import io.remotecontrol.groovy.ContentType
import io.remotecontrol.groovy.client.RemoteControl
import io.remotecontrol.transport.http.HttpTransport
import ratpack.file.FileSystemBinding
import ratpack.http.client.RequestSpec
import ratpack.http.internal.HttpHeaderConstants
import ratpack.render.Renderer
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.file.Path

import static io.netty.handler.codec.http.HttpResponseStatus.*
import static ratpack.remote.RemoteControl.DEFAULT_REMOTE_CONTROL_PATH

class RemoteControlSpec extends RatpackGroovyDslSpec {

  RemoteControl getRemote() {
    server.start()
    new RemoteControl(new HttpTransport("http://localhost:${server.bindPort}/$DEFAULT_REMOTE_CONTROL_PATH"))
  }

  void 'by default the endpoint is not enabled'() {
    expect:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == NOT_FOUND.code()
  }

  void 'only posts are allowed'() {
    when:
    bindings {
      bindInstance ratpack.remote.RemoteControl.handlerDecorator()
    }

    then:
    get(DEFAULT_REMOTE_CONTROL_PATH).statusCode == METHOD_NOT_ALLOWED.code()
    head(DEFAULT_REMOTE_CONTROL_PATH).statusCode == METHOD_NOT_ALLOWED.code()
  }

  void 'only requests that contain groovy-remote-control-command are allowed'() {
    when:
    bindings {
      bindInstance ratpack.remote.RemoteControl.handlerDecorator()
    }

    then:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == UNSUPPORTED_MEDIA_TYPE.code()
  }

  void 'only requests that accept groovy-remote-control-result are allowed'() {
    when:
    bindings {
      bindInstance ratpack.remote.RemoteControl.handlerDecorator()
    }

    then:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.set(HttpHeaderConstants.CONTENT_TYPE, ContentType.COMMAND.value)
      requestSpec.headers.set(HttpHeaderConstants.ACCEPT, 'text/html')
    }

    then:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == NOT_ACCEPTABLE.code()
  }

  void 'can use a custom path'() {
    when:
    bindings {
      bindInstance ratpack.remote.RemoteControl.handlerDecorator("foo")
    }

    and:
    server.start()
    def remoteControl = new RemoteControl(new HttpTransport("http://localhost:${server.bindPort}/foo"))

    then:
    remoteControl { 1 + 2 } == 3
  }

  void 'registry is available in command context'() {
    when:
    bindings {
      bindInstance String, "foo"
      bindInstance ratpack.remote.RemoteControl.handlerDecorator()
    }

    then:
    //from guice
//    remote.exec { get(String) } == "foo"
    //from root registryFunction
    remote.exec { get(FileSystemBinding) != null }
    //created just in time
    remote.exec { get(Renderer.typeOf(Path)) != null }
  }

  void 'endpoint is also enabled if reloading is enabled'() {
    given:
    bindings {
      bindInstance ratpack.remote.RemoteControl.handlerDecorator()
    }
    serverConfig {
      development(true)
    }

    expect:
    remote.exec { 1 + 2 } == 3
  }
}
