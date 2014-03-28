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

import io.netty.handler.codec.http.HttpHeaders
import io.remotecontrol.groovy.ContentType
import io.remotecontrol.groovy.client.RemoteControl
import io.remotecontrol.transport.http.HttpTransport
import ratpack.file.FileRenderer
import ratpack.file.FileSystemBinding
import ratpack.launch.LaunchConfig
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpResponseStatus.*
import static ratpack.remote.RemoteControlModule.DEFAULT_REMOTE_CONTROL_PATH

class RemoteControlSpec extends RatpackGroovyDslSpec {

  final Map<String, String> enabled = ['remoteControl.enabled': 'true'].asImmutable()

  def setup() {
    modules {
      register new RemoteControlModule()
    }
  }

  RemoteControl getRemote() {
    server.start()
    new RemoteControl(new HttpTransport("http://localhost:${server.bindPort}/$DEFAULT_REMOTE_CONTROL_PATH"))
  }

  void 'by default the endpoint is not enabled'() {
    expect:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == NOT_FOUND.code()
  }

  void 'only posts are allowed'() {
    given:
    launchConfig { other(enabled) }

    expect:
    get(DEFAULT_REMOTE_CONTROL_PATH).statusCode == METHOD_NOT_ALLOWED.code()
    head(DEFAULT_REMOTE_CONTROL_PATH).statusCode == METHOD_NOT_ALLOWED.code()
  }

  void 'only requests that contain groovy-remote-control-command are allowed'() {
    given:
    launchConfig { other(enabled) }

    expect:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == UNSUPPORTED_MEDIA_TYPE.code()
  }

  void 'only requests that accept groovy-remote-control-result are allowed'() {
    given:
    launchConfig { other(enabled) }

    when:
    request.header(HttpHeaders.Names.CONTENT_TYPE, ContentType.COMMAND.value)
    request.header(HttpHeaders.Names.ACCEPT, 'text/html')

    then:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == NOT_ACCEPTABLE.code()
  }

  @Unroll
  void 'sending a simple command - #scenario'() {
    given:
    modules {
      register new RemoteControlModule(path: modulePath)
    }
    launchConfig { other(*: enabled, *: otherConfig) }

    and:
    server.start()
    def remoteControl = new RemoteControl(new HttpTransport("http://localhost:${server.bindPort}/$path"))

    expect:
    remoteControl { 1 + 2 } == 3

    where:
    scenario | path | modulePath | otherConfig
    'default path'       | DEFAULT_REMOTE_CONTROL_PATH | null     | [:]
    'path set in module' | 'custom'                    | 'custom' | [:]
    'path set in config' | 'fromConfig'                | null     | ['remoteControl.path': 'fromConfig']
  }

  void 'registry is available in command context'() {
    given:
    launchConfig { other(*: enabled, test: 'it works') }

    expect:
    //from guice
    remote.exec { get(LaunchConfig).other.test } == 'it works'
    //from root registry
    remote.exec { get(FileSystemBinding) != null }
    //created just in time
    remote.exec { get(FileRenderer) != null }
  }

  void 'endpoint is also enabled if reloading is enabled'() {
    given:
    launchConfig {
      other(enabled)
      reloadable(true)
    }

    expect:
    remote.exec { 1 + 2 } == 3
  }
}
