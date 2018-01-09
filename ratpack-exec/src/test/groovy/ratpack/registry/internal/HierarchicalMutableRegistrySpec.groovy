/*
 * Copyright 2016 the original author or authors.
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

package ratpack.registry.internal

import ratpack.func.Action
import ratpack.registry.MutableRegistry
import ratpack.registry.Registry
import ratpack.registry.RegistrySpec
import ratpack.test.internal.registry.MutableRegistryContractSpec

class HierarchicalMutableRegistrySpec extends MutableRegistryContractSpec {

  def parent = EmptyRegistry.INSTANCE
  def child = new SimpleMutableRegistry()

  @Override
  MutableRegistry build(Action<? super RegistrySpec> spec) {
    def r = new HierarchicalMutableRegistry(parent, child)
    spec.execute(r)
    r
  }


  def "parent values are available from registry"() {
    given:
    parent = Registry.builder().add(String, "foo").add(Integer, 100).build()

    def r = build { spec ->
      spec.add(String, "bar")
      spec.add(Long, new Long(250))
    }

    expect:
    r.get(String) == "bar"
    r.getAll(String).toList() == ["bar", "foo"]
    r.get(Integer).intValue() == 100
    r.get(Long).longValue() == 250L

    when:
    r.add(Integer, 500)

    then:
    r.get(Integer).intValue() == 500
    parent.get(Integer).intValue() == 100
  }
}
