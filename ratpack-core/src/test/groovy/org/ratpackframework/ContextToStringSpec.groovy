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

package org.ratpackframework

import org.ratpackframework.context.internal.LazyHierarchicalContext
import org.ratpackframework.context.internal.ObjectHoldingHierarchicalContext
import org.ratpackframework.context.internal.RootContext
import spock.lang.Specification

class ContextToStringSpec extends Specification {

  def "context to strings show chain"() {
    when:
    def root = new RootContext(1)
    def object1 = new ObjectHoldingHierarchicalContext(root, 2)
    def object2 = new ObjectHoldingHierarchicalContext(object1, 3)
    def lazy = new LazyHierarchicalContext(object2, Integer, { 4 } as org.ratpackframework.util.internal.Factory<Integer>)

    then:
    lazy.toString() == "LazyContext{${Integer.name}} -> ObjectContext{3} -> ObjectContext{2} -> RootContext{[1]}"
  }
}
