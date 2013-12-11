/*
 * Copyright 2013 the original author or authors.
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

package ratpack.server;

import ratpack.api.NonBlocking;

/**
 * The mechanism for stopping the application from within the application.
 * <p>
 * An implementation of this is always available via the context registry.
 */
public interface Stopper {

  /**
   * Initiates the shutdown process for the running application.
   * <p>
   * This method <i>may</i> return before the application is fully shut down.
   */
  @NonBlocking
  void stop();

}
