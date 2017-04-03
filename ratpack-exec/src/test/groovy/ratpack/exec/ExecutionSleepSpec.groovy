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

package ratpack.exec

import ratpack.func.Block

import java.time.Duration

class ExecutionSleepSpec extends BaseExecutionSpec {

  def record = []

  class RecordingInterceptor implements ExecInterceptor, ExecInitializer {

    final String id

    RecordingInterceptor(String id) {
      this.id = id
    }

    @Override
    void init(Execution execution) {
      record << "init:$id"
    }

    @Override
    void intercept(Execution execution, ExecInterceptor.ExecType type, Block executionSegment) {
      record << "$id:$type"
      executionSegment.execute()
    }
  }

  def "can sleep execution"() {
    given:
    def start = 0
    def stop = 0

    when:
    execStarter {
      it.register { it.add(new RecordingInterceptor("")) }.start {
        start = System.currentTimeMillis()
        Execution.sleep(Duration.ofSeconds(1)) {
          stop = System.currentTimeMillis()
        }
      }
    }

    then:
    stop - start > 900
    record == ["init:", ":COMPUTE", ":COMPUTE"] // assert that we relinquished the thread
  }
}
