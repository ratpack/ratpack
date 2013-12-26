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

package ratpack.groovy.test;

import ratpack.groovy.launch.GroovyRatpackMain;
import ratpack.test.ServerBackedApplicationUnderTest;

import java.util.HashMap;
import java.util.Map;

public class LocalScriptApplicationUnderTest extends ServerBackedApplicationUnderTest {

  public LocalScriptApplicationUnderTest() {
    this(new HashMap<String, String>());
  }

  public LocalScriptApplicationUnderTest(Map<String, String> overriddenProperties) {
    super(new RatpackMainServerFactory(overriddenProperties));
  }

  private static class RatpackMainServerFactory extends ratpack.test.RatpackMainServerFactory {
    public RatpackMainServerFactory(Map<String, String> overriddenProperties) {
      super(new GroovyRatpackMain(), overriddenProperties);
    }
  }
}
