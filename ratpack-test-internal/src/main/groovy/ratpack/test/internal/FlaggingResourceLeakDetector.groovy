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

package ratpack.test.internal

import groovy.transform.CompileStatic
import io.netty.util.ResourceLeakDetector

import java.util.concurrent.atomic.AtomicReference

@CompileStatic
class FlaggingResourceLeakDetector<T> extends ResourceLeakDetector<T> {

  private final AtomicReference<Boolean> flag

  FlaggingResourceLeakDetector(Class<T> resourceType, int samplingInterval, long maxActive, AtomicReference<Boolean> flag) {
    super(resourceType, samplingInterval, maxActive)
    this.flag = flag
  }


  @Override
  protected void reportTracedLeak(String resourceType, String records) {
    flag.set(true)
    super.reportTracedLeak(resourceType, records)
  }

  @Override
  protected void reportUntracedLeak(String resourceType) {
    flag.set(true)
    super.reportUntracedLeak(resourceType)
  }

  @Override
  protected void reportInstancesLeak(String resourceType) {
    flag.set(true)
    super.reportInstancesLeak(resourceType)
  }
}
