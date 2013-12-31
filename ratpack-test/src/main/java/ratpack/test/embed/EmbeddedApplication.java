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

package ratpack.test.embed;

import ratpack.server.RatpackServer;
import ratpack.test.ApplicationUnderTest;

import java.io.Closeable;

/**
 * An application created and used at runtime, useful for functionally testing subsets of functionality.
 * <p>
 * This mechanism can be used for functionally testing isolated sections of an application,
 * or for testing general libraries that provide reusable functionality (e.g. Ratpack Guice modules).
 * <p>
 * Different implementations expose different API that can be used to define the actual application under test.
 * <p>
 * As embedded applications also implement {@link ratpack.test.ApplicationUnderTest}, they are suitable for use with clients accessing the app via HTTP.
 * Implementations must ensure that the application is up and receiving request when returning from {@link #getAddress()}.
 * Be sure to {@link #close()} the application after use to free resources.
 *
 * @see ratpack.test.embed.LaunchConfigEmbeddedApplication
 */
public interface EmbeddedApplication extends ApplicationUnderTest, Closeable {

  /**
   * The server for the application.
   * <p>
   * Calling this method does not implicitly start the server.
   *
   * @return The server for the application
   */
  RatpackServer getServer();

}
