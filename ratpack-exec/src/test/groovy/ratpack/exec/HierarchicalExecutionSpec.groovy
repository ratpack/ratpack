/*
 * Copyright 2018 the original author or authors.
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

package ratpack.exec

import ratpack.exec.util.Promised
import ratpack.func.Action
import spock.util.concurrent.PollingConditions

class HierarchicalExecutionSpec extends BaseExecutionSpec {

  def "current execution is parent during initializer of child"() {
    given:
    counts(2)

    when:
    exec { e ->
      e.add("foo")
      Execution.fork()
        .register { it.add(Execution.current().get(String) + "-bar") }
        .onStart { events << "child-on-start"; events << Execution.current().get(String); sleep 500 }
        .onComplete { events << "child-complete"; latch.countDown() }
        .start { events << "child-start"; events << Execution.current().get(String) }
    }

    then:
    events == ["child-on-start", "foo", "complete", "child-start", "foo-bar", "child-complete"]
  }

  def "ref registry is empty when execution completes"() {
    when:
    ExecutionRef ref
    exec { e ->
      e.add("foo")
      ref = e.ref
      events << ref.get(String)
    }

    then:
    events == ["foo", "complete"]
    new PollingConditions().eventually { ref.getAll(Object).toList().empty }
  }

  def "parent is available during child execution"() {
    when:
    def p1 = new Promised<Void>()

    exec { e ->
      e.add("parent")
      Execution.fork().start {
        events << it.parent.get(String)
        p1.success(null)
      }

      p1.promise().then(Action.noop())
    }

    then:
    events == ["parent", "complete"]
  }

  def "parent is available transitively"() {
    when:
    def p1 = new Promised<Void>()

    exec { e ->
      e.add("root")
      Execution.fork().start {
        Execution.fork().start {
          events << it.parent.parent.get(String)
          p1.success(null)
        }
      }

      p1.promise().then {}
    }

    then:
    events == ["root", "complete"]
  }

  def "can query whether parent is complete"() {
    when:
    counts 2
    def p1 = new Promised<Void>()
    def p2 = new Promised<Void>()

    exec { e ->
      e.onComplete { p2.success(null) }
      Execution.fork().onStart {}.start {
        events << Execution.current().parent.complete.toString()
        p1.success(null)
        p2.promise().then {
          events << Execution.current().parent.complete.toString()
          latch.countDown()
        }
      }
      p1.promise().then(Action.noop())
    }

    then:
    events == ["false", "complete", "true"]
  }

  def "current execution is parent during initializer"() {
    given:
    counts 2

    when:
    exec {
      it.fork()
        .register {
        it.add(ExecInitializer, {
          events << (it.parent == Execution.current().ref).toString()
        } as ExecInitializer)
      }.start {
        latch.countDown()
      }
    }

    then:
    events == ["true", "complete"]
  }

  def "top level execution has no parent"() {
    when:
    exec {
      try {
        it.parent
        events << "no"
      } catch (IllegalStateException e) {
        events << it.maybeParent().map { true }.orElse(false).toString()
      }
    }

    then:
    events == ["false", "complete"]
  }
}
