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

import ratpack.test.embed.EmbeddedApp;
import ratpack.test.http.TestHttpClient;

import java.net.URI;

/**
 * A Ratpack application that is being tested, or used at test time.
 *
 * @see CloseableApplicationUnderTest
 * @see ServerBackedApplicationUnderTest
 * @see MainClassApplicationUnderTest
 * @see EmbeddedApp
 */
public interface ApplicationUnderTest {

  /**
   * The address of the application under test, which is guaranteed to be listening for requests.
   * <p>
   * Implementations should start the application if it has not already been started before returning from this method.
   *
   * @return the address of the application under test
   */
  URI getAddress();

  /**
   * Creates a new test HTTP client that makes requests to this application.
   *
   * @return a new test HTTP client that tests this application
   */
  default TestHttpClient getHttpClient() {
    return TestHttpClient.testHttpClient(this);
  }

}
