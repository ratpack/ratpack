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

package org.ratpackframework.guice.internal

import com.google.common.collect.ImmutableList
import com.google.inject.Guice
import org.ratpackframework.launch.LaunchConfig
import org.ratpackframework.registry.internal.RootRegistry
import spock.lang.Specification

import static org.ratpackframework.guice.Guice.justInTimeRegistry

class JustInTimeInjectorChildRegistrySpec extends Specification {

  static class Thing {}

  def "delegates to parent when no bound instance"() {
    when:
    def launchConfig = Mock(LaunchConfig)
    def parent = new RootRegistry(ImmutableList.of(launchConfig))
    def injector = Guice.createInjector()
    def registry = justInTimeRegistry(parent, injector)

    then:
    registry.get(Thing) instanceof Thing
    registry.get(LaunchConfig).is launchConfig
  }

}
