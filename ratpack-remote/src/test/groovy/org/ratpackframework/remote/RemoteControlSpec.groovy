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

package org.ratpackframework.remote

import groovyx.remote.client.RemoteControl
import groovyx.remote.transport.http.HttpTransport
import io.netty.handler.codec.http.HttpHeaders
import org.ratpackframework.file.internal.DefaultFileSystemBinding
import org.ratpackframework.file.internal.FileRenderer
import org.ratpackframework.launch.LaunchConfig
import org.ratpackframework.remote.internal.RemoteControlHandler
import org.ratpackframework.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static io.netty.handler.codec.http.HttpResponseStatus.*
import static org.ratpackframework.remote.RemoteControlModule.DEFAULT_REMOTE_CONTROL_PATH

class RemoteControlSpec extends RatpackGroovyDslSpec {

  void appWithRemoteControl() {
    app {
      modules {
        register new RemoteControlModule()
      }
    }
  }

  void appWithEnabledRemoteControl() {
    other['remoteControl.enabled'] = 'true'
    appWithRemoteControl()
  }

  RemoteControl getRemote() {
    startServerIfNeeded()
    new RemoteControl(new HttpTransport("http://localhost:${server.bindPort}/$DEFAULT_REMOTE_CONTROL_PATH"))
  }

  void 'by default the endpoint is not enabled'() {
    given:
    appWithRemoteControl()

    expect:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == NOT_FOUND.code()
  }

  void 'only posts are allowed'() {
    given:
    appWithEnabledRemoteControl()

    expect:
    get(DEFAULT_REMOTE_CONTROL_PATH).statusCode == METHOD_NOT_ALLOWED.code()
    head(DEFAULT_REMOTE_CONTROL_PATH).statusCode == METHOD_NOT_ALLOWED.code()
  }

  void 'only requests that contain groovy-remote-control-command are allowed'() {
    given:
    appWithEnabledRemoteControl()

    expect:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == UNSUPPORTED_MEDIA_TYPE.code()
  }

  void 'only requests that accept groovy-remote-control-result are allowed'() {
    given:
    appWithEnabledRemoteControl()

    when:
    request.header(HttpHeaders.Names.CONTENT_TYPE, RemoteControlHandler.REQUEST_CONTENT_TYPE)
    request.header(HttpHeaders.Names.ACCEPT, 'text/html')

    then:
    post(DEFAULT_REMOTE_CONTROL_PATH).statusCode == NOT_ACCEPTABLE.code()
  }

  @Unroll
  void 'sending a simple command - #scenario'() {
    given:
    app {
      modules {
        register new RemoteControlModule(path: modulePath)
      }
    }
    other = ['remoteControl.enabled': 'true'] + otherConfig

    and:
    startServerIfNeeded()
    def remoteControl = new RemoteControl(new HttpTransport("http://localhost:${server.bindPort}/$path"))


    expect:
    remoteControl { 1 + 2 } == 3

    where:
    scenario             | path                        | modulePath | otherConfig
    'default path'       | DEFAULT_REMOTE_CONTROL_PATH | null       | [:]
    'path set in module' | 'custom'                    | 'custom'   | [:]
    'path set in config' | 'fromConfig'                | null       | ['remoteControl.path': 'fromConfig']
  }

  void 'registry is available in command context'() {
    given:
    appWithEnabledRemoteControl()
    other.test = 'it works'

    expect:
    //from guice
    remote.exec { registry.get(LaunchConfig).other.test } == 'it works'
    //from root registry
    remote.exec { registry.get(DefaultFileSystemBinding) != null }
    //created just in time
    remote.exec { registry.get(FileRenderer) != null }
  }

  void 'endpoint is also enabled if reloading is enabled'() {
    given:
    reloadable = true
    appWithRemoteControl()

    expect:
    remote.exec { 1 + 2 } == 3
  }
}
