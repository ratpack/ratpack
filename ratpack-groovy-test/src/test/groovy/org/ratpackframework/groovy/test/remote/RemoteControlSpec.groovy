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

package org.ratpackframework.groovy.test.remote

import org.ratpackframework.groovy.test.ScriptAppSpec
import org.ratpackframework.launch.LaunchConfig

class RemoteControlSpec extends ScriptAppSpec {

  void setup() {
    System.properties['ratpack.configResource'] = 'remoteControlSpec/ratpack.properties'
    System.properties['ratpack.other.remoteControl.enabled'] = 'true'
    System.properties['ratpack.other.test'] = 'it works!'
  }

  void 'can send commands to an endpoint registered with the default path'() {
    given:
    def remote = new RemoteControl(runningApplication)

    expect:
    remote.exec { registry.get(LaunchConfig).other.test } == 'it works!'
  }

  void 'can send commands to an endpoint registered with a custom path'() {
    given:
    System.properties['ratpack.other.remoteControl.path'] = 'custom-remote-control-path'
    def remote = new RemoteControl(runningApplication, 'custom-remote-control-path')

    expect:
    remote.exec { registry.get(LaunchConfig).other.test } == 'it works!'
  }
}
