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

package ratpack.groovy.internal;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import ratpack.api.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyVersionCheck {

  private GroovyVersionCheck() {
  }

  public static void ensureRequiredVersionUsed(String groovyVersion) throws RuntimeException {
    String minimumVersion = maybeLoadMinimumGroovyVersion();
    if (minimumVersion != null) {
      ensureRequiredVersionUsed(groovyVersion, minimumVersion);
    }
  }

  static void ensureRequiredVersionUsed(String groovyVersion, String minimumVersion) {
    Pattern versionPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+).*");
    Matcher minimumVersionMatcher = versionPattern.matcher(minimumVersion);
    if (!minimumVersionMatcher.find()) {
      throw new IllegalStateException("Minimum groovy isn't in expected format");
    }
    int minimumMajor = groupAsInt(minimumVersionMatcher, 1);
    int minimumMinor = groupAsInt(minimumVersionMatcher, 2);
    int minimumPatch = groupAsInt(minimumVersionMatcher, 3);

    Matcher matcher = versionPattern.matcher(groovyVersion);
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

    throw new RuntimeException("Ratpack requires Groovy " + minimumVersion + "+ to run but the version used is " + groovyVersion);
  }

  private static int groupAsInt(Matcher matcher, int group) {
    return Integer.parseInt(matcher.group(group));
  }

  @Nullable
  public static String maybeLoadMinimumGroovyVersion() {
    URL resource = GroovyVersionCheck.class.getClassLoader().getResource("ratpack/minimum-groovy-version.txt");
    try {
      return resource == null ? null : Resources.toString(resource, Charsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }

}
