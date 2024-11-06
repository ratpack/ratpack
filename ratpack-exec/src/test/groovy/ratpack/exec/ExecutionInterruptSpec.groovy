/*
 * Copyright 2024 the original author or authors.
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

import ratpack.func.Action

class ExecutionInterruptSpec extends BaseExecutionSpec {

  def "can interrupt sync promise"() {
    when:
    exec { execution ->
      Promise.sync {
        execution.interrupt()
        1
      } onError {
        events << it
      } then {
        events << it
      }
    }

    then:
    events[0] instanceof InterruptedException
    events[1] == "complete"
  }

  def "can interrupt async promise"() {
    when:
    exec { execution ->
      Promise.async {
        Thread.start {
          execution.interrupt()
        }
      } onError {
        events << it
      } then {
        events << it
      }
    }

    then:
    events[0] instanceof InterruptedException
    events[1] == "complete"
  }

  def "completing async op after interruption is innocuous"() {
    when:
    exec { execution ->
      Promise.async { down ->
        Thread.start {
          execution.interrupt()
          down.success(10)
        }
      } onError {
        events << it
      } then {
        events << it
      }
    }

    then:
    events[0] instanceof InterruptedException
    events[1] == "complete"
  }

  def "can interrupt blocking"() {
    when:
    exec { execution ->
      Blocking.get {
        Thread.start {
          execution.interrupt()
        }
        Thread.sleep(3000)
        1
      } onError {
        events << it
      } then {
        events << it
      }
    }

    then:
    events[0] instanceof InterruptedException
    events[1] == "complete"
  }

  def "can interrupt more than once"() {
    when:
    exec { execution ->
      Promise.sync {
        execution.interrupt()
        1
      } onError {
        events << it
        Promise.async { down ->
          events << "async"
          Thread.start {
            execution.interrupt()
            down.success(2)
          }
        } then {
          events << it
        }
      } then {
        events << it
      }
    }

    then:
    events[0] instanceof InterruptedException
    events[1] == "async"
    events[2] instanceof InterruptedException
    events[3] == "complete"
  }


  def "can swallow interrupt"() {
    when:
    exec { execution ->
      Promise.sync {
        execution.interrupt()
        1
      } onError {
        events << it
        Promise.async { down ->
          events << "async"
          Thread.start {
            down.success(2)
          }
        } then {
          events << it
        }
      } then {
        events << it
      }
    }

    then:
    events[0] instanceof InterruptedException
    events[1] == "async"
    events[2] == 2
    events[3] == "complete"
  }

  def "can interrupt stream"() {
    when:
    exec { execution ->
      execution.delimitStream(Action.throwException()) { continuationStream ->
        Thread.start {
          continuationStream.event {
            events << "1"
          }
          execution.interrupt()
          continuationStream.event {
            events << "2"
          }
        }
      }
    }

    then:
    events.any { it instanceof InterruptedException }
    events.last() == "complete"
  }

}
