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

package ratpack.test.remote

import ratpack.remote.RemoteControlModule
import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.test.remote.RemoteControl.command

class RemoteControlUsageSpec extends RatpackGroovyDslSpec {

  RemoteControl remoteControl

  def setup() {
    bindings {
      add new RemoteControlModule(), { it.enabled(true) }
      bind ValueHolder
    }
    handlers {
      get {
        render getAll(ValueHolder)*.value.join(":")
      }
    }
    remoteControl = new RemoteControl(applicationUnderTest)
  }

  @javax.inject.Singleton
  static class ValueHolder {
    String value = "initial"
  }

  def "can access application internals"() {
    expect:
    text == "initial"

    when:
    remoteControl.exec { get(ValueHolder).value = "changed" }

    then:
    text == "changed"
  }

  def "can override registry entries"() {
    expect:
    text == "initial"

    when:
    remoteControl.exec { add(ValueHolder, new ValueHolder(value: "overridden")) }

    then:
    text == "overridden:initial"

    when:
    remoteControl.exec { clearRegistry() }

    then:
    text == "initial"
  }

  def "can use command method to create detached command"() {
    given:
    def command = command { add(ValueHolder, new ValueHolder(value: "command")) }

    when:
    remoteControl.exec command, { add(ValueHolder, new ValueHolder(value: "overridden")) }

    then:
    text == "command:overridden:initial"
  }

}
