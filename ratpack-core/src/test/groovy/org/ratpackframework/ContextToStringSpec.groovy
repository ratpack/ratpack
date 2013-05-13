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
