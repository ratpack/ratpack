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

package org.ratpackframework.registry

import com.google.common.collect.ImmutableList
import org.ratpackframework.registry.internal.LazyChildRegistry
import org.ratpackframework.registry.internal.ObjectHoldingChildRegistry
import org.ratpackframework.registry.internal.RootRegistry
import spock.lang.Specification

class ServiceRegistryToStringSpec extends Specification {

  def "registry to strings show chain"() {
    when:
    def root = new RootRegistry(ImmutableList.of(1))
    def object1 = new ObjectHoldingChildRegistry(root, 2)
    def object2 = new ObjectHoldingChildRegistry(object1, 3)
    def lazy = new LazyChildRegistry(object2, Integer, { 4 } as org.ratpackframework.util.internal.Factory<Integer>)

    then:
    lazy.toString() == "LazyChildRegistry{${Integer.name}} -> ObjectServiceRegistry{3} -> ObjectServiceRegistry{2} -> RootRegistry{[1]}"
  }
}
