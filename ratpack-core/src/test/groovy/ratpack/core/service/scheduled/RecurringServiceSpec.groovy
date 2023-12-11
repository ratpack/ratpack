/*
 * Copyright 2022 the original author or authors.
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

package ratpack.core.service.scheduled

import ratpack.exec.ExecController
import ratpack.exec.ExecResult
import ratpack.exec.Operation
import ratpack.exec.Promise
import ratpack.exec.util.Promised
import ratpack.func.Block
import ratpack.test.exec.ExecHarness
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.AutoCleanup
import spock.util.concurrent.PollingConditions

import java.time.Duration

class RecurringServiceSpec extends RatpackGroovyDslSpec {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness(3)

  Duration delay = Duration.ofHours(1)
  Block action = {}

  RecordingRecurringService service

  List<String> events = [].<String> asSynchronized()
  int actionCount = 0

  private class RecordingRecurringService extends RecurringService {

    protected RecordingRecurringService(ExecController execController, OnStart onStart) {
      super(execController, onStart)
    }

    @Override
    protected void performAction() throws Exception {
      events << "start-running"
      action.execute()
      events << "finish-running"
      actionCount++
    }

    @Override
    protected Duration nextDelay() {
      return delay
    }

    @Override
    protected void stopRequested() {
      events << "stop-requested"
    }
  }

  def "action runs repeatedly"() {
    given:
    service = new RecordingRecurringService(execHarness.controller, RecurringService.OnStart.EXEC)

    and:
    delay = Duration.ofMillis(50)

    when:
    def result = startService()

    then:
    result.success

    when:
    // Wait enough time to see the service perform its action twice in addition to the initial execution,
    // for a total of three times.
    Thread.sleep((delay.toMillis() * 2) + 20)

    then:
    actionCount == 3
  }

  def "exceptions are not propagated"() {
    given:
    service = new RecordingRecurringService(execHarness.controller, RecurringService.OnStart.EXEC)

    and:
    delay = Duration.ofMinutes(1)
    action = {
      throw new IllegalStateException("oh no!")
    }

    when:
    def result = startService()

    then:
    result.success
    //noinspection ChangeToOperator - It isn't my experience that '==' works here.
    events.equals(["start-running"].asSynchronized())
  }

  def "action is not rescheduled if stopped while running, but finishes its outstanding work"() {
    given:
    service = new RecordingRecurringService(execHarness.controller, RecurringService.OnStart.EXEC)

    and:
    def started = new Promised<Void>()
    def finished = new Promised<Void>()
    delay = Duration.ofMillis(50)
    action = {
      started.success(null)
      new PollingConditions().within(1, { service.isStopped() })
      finished.success(null)
    }

    when:
    startServiceAsync()
    execHarness.yield { started.promise() }

    and:
    stopService()
    execHarness.yield { finished.promise() }
    // Wait enough time, to know that if the service were still running, more events would be recorded
    Thread.sleep((delay.toMillis() * 2) + 20)

    then:
    events == ["start-running", "stop-requested", "finish-running"]
  }

  def "inline action blocks startup"() {
    given:
    service = new RecordingRecurringService(execHarness.controller, RecurringService.OnStart.INLINE)

    and:
    delay = Duration.ofMinutes(1)
    action = {
      throw new IllegalStateException("oh no!")
    }

    when:
    def result = startService()

    then:
    result.error
    result.throwable.message == "oh no!"
    //noinspection ChangeToOperator - It isn't my experience that '==' works here.
    events.equals(["start-running"].asSynchronized())
  }

  def "schedule can be started with the action immediately executed"() {
    given:
    service = new RecordingRecurringService(execHarness.controller, RecurringService.OnStart.EXEC)

    and:
    delay = Duration.ofMillis(250)

    when:
    def result = startService()

    then:
    result.success

    when:
    // Wait an amount of time shorter than the delay, to check that the execution was kicked off initially.
    Thread.sleep((delay.toMillis() / 2) - 50 as long)

    then:
    actionCount == 1
  }

  def "schedule can be started without immediately executing the action"() {
    given:
    service = new RecordingRecurringService(execHarness.controller, RecurringService.OnStart.NEXT)

    and:
    delay = Duration.ofMillis(250)

    when:
    def result = startService()

    then:
    result.success

    when:
    // Wait an amount of time shorter than the delay, to check that the action wasn't initially executed.
    Thread.sleep((delay.toMillis() / 2) - 50 as long)

    then:
    events.isEmpty()

    when:
    // Wait the full delay, to check that the action was scheduled.
    Thread.sleep(delay.toMillis())

    then:
    actionCount == 1
  }

  def "service can be started without scheduling the action"() {
    given:
    service = new RecordingRecurringService(execHarness.controller, RecurringService.OnStart.NONE)

    and:
    delay = Duration.ofMillis(250)

    when:
    def result = startService()

    then:
    result.success

    when:
    // Wait an amount of time longer than the delay, to check that no execution was either kicked off or scheduled.
    Thread.sleep(delay.toMillis() + 100)

    then:
    actionCount == 0
  }

  private ExecResult<Boolean> startService() {
    execHarness.<Boolean> yield { service.onStart(null); Promise.value(true) }
  }

  private void startServiceAsync() {
    execHarness.execute { service.onStart(null); Operation.of {} }
  }

  private void stopService() {
    execHarness.execute { service.onStop(null); Operation.of {} }
  }
}
