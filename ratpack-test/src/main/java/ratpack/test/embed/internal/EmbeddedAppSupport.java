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

package ratpack.test.embed.internal;

import ratpack.core.server.RatpackServer;
import ratpack.test.embed.EmbeddedApp;

import static ratpack.func.Exceptions.uncheck;

public abstract class EmbeddedAppSupport implements EmbeddedApp {

  private RatpackServer ratpackServer;

  @Override
  public RatpackServer getServer() {
    if (ratpackServer == null) {
      try {
        ratpackServer = createServer();
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    return ratpackServer;
  }

  abstract protected RatpackServer createServer() throws Exception;

}
