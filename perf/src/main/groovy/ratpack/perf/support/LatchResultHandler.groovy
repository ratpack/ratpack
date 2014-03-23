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

package ratpack.perf.support

import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.ResultHandler

import java.util.concurrent.CountDownLatch

class LatchResultHandler implements ResultHandler<Void> {

  final CountDownLatch latch
  boolean complete
  GradleConnectionException failure

  LatchResultHandler(CountDownLatch latch) {
    this.latch = latch
  }

  @Override
  void onComplete(Void result) {
    complete = true
    latch.countDown()
  }

  @Override
  void onFailure(GradleConnectionException failure) {
    this.failure = failure
    latch.countDown()
  }

}
