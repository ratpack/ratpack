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

package ratpack.reactor.internal;

import reactor.core.scheduler.Scheduler;

public class DefaultSchedulers {

  private static Scheduler computationScheduler = new MultiExecControllerBackedScheduler();
  private static Scheduler ioScheduler = new MultiBlockingExecutorBackedScheduler();

  public static Scheduler getComputationScheduler() {
    return computationScheduler;
  }

  public static Scheduler getIoScheduler() {
    return ioScheduler;
  }

}
