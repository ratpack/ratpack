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

package ratpack.impose;

import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;

/**
 * Forces whether to start the application in {@link ServerConfig#isDevelopment()} mode or not.
 * <p>
 * This imposition is used by {@link ServerConfigBuilder}.
 * If present, the imposed value will be used regardless of any previously specified setting on the builder.
 * <p>
 * Note, this can be used to force development mode or force not development mode.
 * Though, it is more commonly used for the former.
 *
 * @see Impositions
 * @since 1.2
 */
public final class ForceDevelopmentImposition implements Imposition {

  private final boolean development;

  private ForceDevelopmentImposition(boolean development) {
    this.development = development;
  }

  /**
   * Creates a new imposition of the specified value.
   *
   * @param development the value to impose
   * @return a newly created imposition
   */
  public static ForceDevelopmentImposition of(boolean development) {
    return new ForceDevelopmentImposition(development);
  }

  /**
   * The value to impose.
   *
   * @return the value to impose
   */
  public boolean isDevelopment() {
    return development;
  }

}
