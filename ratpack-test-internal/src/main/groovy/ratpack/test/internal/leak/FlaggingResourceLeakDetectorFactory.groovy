/*
 * Copyright 2021 the original author or authors.
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

package ratpack.test.internal.leak

import groovy.transform.CompileStatic
import io.netty.util.ResourceLeakDetector
import io.netty.util.ResourceLeakDetectorFactory

import java.util.concurrent.ConcurrentLinkedQueue

@CompileStatic
class FlaggingResourceLeakDetectorFactory extends ResourceLeakDetectorFactory {

  static void install() {
    ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID)
    ResourceLeakDetectorFactory.resourceLeakDetectorFactory = new FlaggingResourceLeakDetectorFactory()
  }

  public static final Queue<String> LEAKS = new ConcurrentLinkedQueue<>()

  @Override
  @SuppressWarnings("UnnecessaryPublicModifier")
  <T> ResourceLeakDetector<T> newResourceLeakDetector(Class<T> resource, int samplingInterval, long maxActive) {
    return new FlaggingResourceLeakDetector<T>(resource, samplingInterval, LEAKS)
  }

}
