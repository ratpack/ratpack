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

package ratpack.server.internal;

public abstract class Slf4jNoBindingDetector {

  private static final String CLASS_NAME = "org.slf4j.impl.StaticLoggerBinder";

  private static final boolean HAS_BINDING = init();

  private Slf4jNoBindingDetector() {
  }

  public static boolean isHasBinding() {
    return HAS_BINDING;
  }

  private static boolean init() {
    try {
      Slf4jNoBindingDetector.class.getClassLoader().loadClass(CLASS_NAME);
      return true;
    } catch (ClassNotFoundException e) {
      System.err.println(
        "WARNING: No slf4j logging binding found for Ratpack, there will be no logging output." + System.lineSeparator()
          + "WARNING: Please add an slf4j binding, such as slf4j-log4j2, to the classpath." + System.lineSeparator()
          + "WARNING: More info may be found here: http://ratpack.io/manual/current/logging.html"
      );
      return false;
    }
  }

}
