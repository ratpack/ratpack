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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import ratpack.util.ExceptionUtils;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyVersionChecker {

  private GroovyVersionChecker() {
  }

  public static void ensureRequiredVersionUsed(String version) {
    try {
      String minimumVersion = retrieveMinimumGroovyVersion();

      Pattern versionPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+).*");
      Matcher minimumVersionMatcher = versionPattern.matcher(minimumVersion);
      minimumVersionMatcher.find();
      int minimumMajor = groupAsInt(minimumVersionMatcher, 1);
      int minimumMinor = groupAsInt(minimumVersionMatcher, 2);
      int minimumPatch = groupAsInt(minimumVersionMatcher, 3);

      Matcher matcher = versionPattern.matcher(version);
      if (matcher.find()) {
        int major = groupAsInt(matcher, 1);
        int minor = groupAsInt(matcher, 2);
        int patch = groupAsInt(matcher, 3);
        if (major > minimumMajor
          || (major == minimumMajor && minor > minimumMinor)
          || (major == minimumMajor && minor == minimumMinor && patch >= minimumPatch)) {
          return;
        }
      }

      throw new RuntimeException("Ratpack requires Groovy " + minimumVersion + "+ to run but the version used is " + version);
    } catch (IOException e) {
      ExceptionUtils.uncheck(e);
    }
  }

  private static int groupAsInt(Matcher matcher, int group) {
    return Integer.parseInt(matcher.group(group));
  }

  private static String retrieveMinimumGroovyVersion() throws IOException {
    URL resource = GroovyVersionChecker.class.getClassLoader().getResource("ratpack/minimum-groovy-version.txt");
    return Resources.toString(resource, Charsets.UTF_8);
  }
}
