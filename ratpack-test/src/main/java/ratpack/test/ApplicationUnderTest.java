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

package ratpack.test;

import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.RatpackServer;
import ratpack.test.http.TestHttpClient;

import java.net.URI;

/**
 * Provides the address of the running application.
 * <p>
 * This will be called on demand. Implementations may bootstrap the application
 * the first time the address is asked for.
 * <p>
 * Implementations do not need to be thread safe.
 */
public interface ApplicationUnderTest {

  static CloseableApplicationUnderTest of(RatpackServer ratpackServer) {
    return of(() -> ratpackServer);
  }

  static CloseableApplicationUnderTest of(Factory<? extends RatpackServer> ratpackServer) {
    return new ServerBackedApplicationUnderTest() {
      @Override
      protected RatpackServer createServer() throws Exception {
        return ratpackServer.create();
      }
    };
  }

  static CloseableApplicationUnderTest of(Class<?> mainClass) {
    return new MainClassApplicationUnderTest(mainClass);
  }

  static CloseableApplicationUnderTest of(Class<?> mainClass, Action<? super RegistrySpec> action) throws Exception {
    return new MainClassApplicationUnderTest(mainClass) {
      @Override
      protected Registry createOverrides(Registry serverRegistry) throws Exception {
        return Registry.of(action);
      }
    };
  }

  /**
   * The address of the application under test, which is guaranteed to be accepting requests.
   *
   * @return The address of the application under test, which is guaranteed to be accepting requests
   */
  URI getAddress();

  /**
   * Creates a new test HTTP client that tests this application.
   *
   * @return a new test HTTP client that tests this application
   */
  default TestHttpClient getHttpClient() {
    return TestHttpClient.testHttpClient(this);
  }

}
