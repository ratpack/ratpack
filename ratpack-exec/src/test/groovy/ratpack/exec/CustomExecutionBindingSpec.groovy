/*
 * Copyright 2020 the original author or authors.
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

import ratpack.exec.internal.ExecThreadBinding
import ratpack.func.Factory
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class CustomExecutionBindingSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness { spec ->
    spec.eventLoopBinding(CustomExecType.WORK) { e ->
      e.threads(1)
      e.prefix("work")
      e.priority(Thread.MAX_PRIORITY - 1)
    }
    spec.executorServiceBinding(CustomExecType.BATCH) { e ->
      e.prefix("batch")
      e.priority(Thread.MIN_PRIORITY)
    }
  }

  def "can use a custom computation binding"() {
    given:
    def latchThread = new AtomicReference<String>()
    def latch = new CountDownLatch(1)

    when:
    String name = harness.yield { e ->
      Promise.async { downstream ->
        ExecController.require().fork(CustomExecType.WORK).start { e2 ->
          e2.controller.getExecutorService(CustomExecType.BATCH).submit { ->
            latchThread.set(Thread.currentThread().name)
            latch.countDown()
          }
          downstream.success(Thread.currentThread().name)
        }
      }
    }.valueOrThrow

    then:
    noExceptionThrown()
    name.startsWith("work-")

    when:
    latch.await(1, TimeUnit.SECONDS)

    then:
    latchThread.get().startsWith("batch-")

    cleanup:
    harness.close()
  }

  def "use a custom execution type"() {
    given:
    def bindings = []

    when:
    def value = harness.yield {
      Promise.sync {
        bindings << ExecThreadBinding.require().executionType
        2
      }.flatMap { i ->
        Work.get {
          bindings << ExecThreadBinding.require().executionType
          i * 2
        }
      }.blockingMap { i ->
        bindings << ExecThreadBinding.require().executionType
        i - 1
      }
    }.valueOrThrow

    then:
    noExceptionThrown()
    value == 3
    bindings == [ExecType.COMPUTE, CustomExecType.WORK, ExecType.BLOCKING]

  }

  enum CustomExecType implements ExecutionType {
    WORK,
    BATCH
  }

  abstract class Work {
    private Work() {}

    static <T> Promise<T> get(Factory<T> factory) {
      return Promise.of(PromiseSupport.nonBlockingExecuteOn(CustomExecType.WORK, factory))
    }
  }
}
