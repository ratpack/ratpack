/*
 * Copyright 2014 the original author or authors.
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

package ratpack.error;

import ratpack.exec.ExecControl;
import ratpack.exec.Throttle;
import ratpack.test.exec.ExecHarness;
import ratpack.test.exec.ExecResult;

import java.util.concurrent.atomic.AtomicInteger;

public class Example {

  public static void main(String... args) throws Exception {
    int numJobs = 1000;
    int maxAtOnce = 10;

    ExecResult<Integer> result = ExecHarness.yield(e -> {
      ExecControl control = e.getControl();

      AtomicInteger maxConcurrent = new AtomicInteger();
      AtomicInteger active = new AtomicInteger();
      AtomicInteger done = new AtomicInteger();

      Throttle throttle = Throttle.ofSize(maxAtOnce);

      // Launch numJobs forked executions, and return the maximum number that were executing at any given time
      return control.promise(f -> {
        for (int i = 0; i < numJobs; i++) {
          control.exec().start(e2 ->
            control
              .<Integer>promise(f2 -> {
                int activeNow = active.incrementAndGet();
                int maxConcurrentVal = maxConcurrent.updateAndGet(m -> Math.max(m, activeNow));
                active.decrementAndGet();
                f2.success(maxConcurrentVal);
              })
              .throttled(throttle) // limit concurrency
              .then(max -> {
                if (done.incrementAndGet() == numJobs) {
                  f.success(max);
                }
              }));
        }
      });
    });

    assert result.getValue() <= maxAtOnce;
  }
}
