/*
 * Copyright 2014 the original author or authors.
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

package ratpack.groovy.launch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyVersionChecker {

  public static void ensureRequiredVersionUsed(String version) {
    Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)\\..*");
    Matcher matcher = pattern.matcher(version);

    while (matcher.find()) {
      int major = Integer.parseInt(matcher.group(1));
      int minor = Integer.parseInt(matcher.group(2));
      if (major > 2 || (major == 2 && minor >= 2)) {
        return;
      }
    }

    throw new RuntimeException("Ratpack requires Groovy 2.2+ to run but the version used is " + version);
  }
}
