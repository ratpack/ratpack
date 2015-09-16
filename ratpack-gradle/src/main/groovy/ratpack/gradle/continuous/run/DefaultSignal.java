/*
 * Copyright 2015 the original author or authors.
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

package ratpack.gradle.continuous.run;

import java.util.concurrent.SynchronousQueue;

public class DefaultSignal implements Signal {

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final SynchronousQueue<Object> queue = new SynchronousQueue<Object>();

  @Override
  public void await() {
    try {
      queue.take();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void fire() {
    try {
      queue.put(new Object());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
