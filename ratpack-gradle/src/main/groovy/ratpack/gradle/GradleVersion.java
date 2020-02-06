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

package ratpack.gradle;

import org.gradle.internal.UncheckedException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleVersion implements Comparable<GradleVersion> {

  private static final Pattern VERSION_PATTERN = Pattern.compile("((\\d+)(\\.\\d+)+)(-(\\p{Alpha}+)-(\\w+))?(-(SNAPSHOT|\\d{14}([-+]\\d{4})?))?");
  private static final int STAGE_MILESTONE = 0;
  private static final int STAGE_UNKNOWN = 1;
  private static final int STAGE_PREVIEW = 2;
  private static final int STAGE_RC = 3;

  private final String version;
  private final int majorPart;
  private final Long snapshot;
  private final String versionPart;
  private final Stage stage;

  /**
   * Parses the given string into a GradleVersion.
   *
   * @throws IllegalArgumentException On unrecognized version string.
   */
  public static GradleVersion version(String version) throws IllegalArgumentException {
    return new GradleVersion(version);
  }

  private GradleVersion(String version) {
    this.version = version;
    Matcher matcher = VERSION_PATTERN.matcher(version);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(String.format("'%s' is not a valid Gradle version string (examples: '1.0', '1.0-rc-1')", version));
    }

    versionPart = matcher.group(1);
    majorPart = Integer.parseInt(matcher.group(2), 10);

    this.stage = parseStage(matcher);
    this.snapshot = parseSnapshot(matcher);
  }

  private Long parseSnapshot(Matcher matcher) {
    if ("snapshot".equals(matcher.group(5)) || isCommitVersion(matcher)) {
      return 0L;
    } else if (matcher.group(8) == null) {
      return null;
    } else if ("SNAPSHOT".equals(matcher.group(8))) {
      return 0L;
    } else {
      try {
        if (matcher.group(9) != null) {
          return new SimpleDateFormat("yyyyMMddHHmmssZ").parse(matcher.group(8)).getTime();
        } else {
          SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
          format.setTimeZone(TimeZone.getTimeZone("UTC"));
          return format.parse(matcher.group(8)).getTime();
        }
      } catch (ParseException e) {
        throw UncheckedException.throwAsUncheckedException(e);
      }
    }
  }

  private Stage parseStage(Matcher matcher) {
    if (matcher.group(4) == null || isCommitVersion(matcher)) {
      return null;
    } else if (isStage("milestone", matcher)) {
      return Stage.from(STAGE_MILESTONE, matcher.group(6));
    } else if (isStage("preview", matcher)) {
      return Stage.from(STAGE_PREVIEW, matcher.group(6));
    } else if (isStage("rc", matcher)) {
      return Stage.from(STAGE_RC, matcher.group(6));
    } else {
      return Stage.from(STAGE_UNKNOWN, matcher.group(6));
    }
  }

  private boolean isCommitVersion(Matcher matcher) {
    return "commit".equals(matcher.group(5));
  }

  private boolean isStage(String stage, Matcher matcher) {
    return stage.equals(matcher.group(5));
  }

  private String setOrParseCommitId(String commitId, Matcher matcher) {
    if (commitId != null || !isCommitVersion(matcher)) {
      return commitId;
    } else {
      return matcher.group(6);
    }
  }

  @Override
  public String toString() {
    return "Gradle " + version;
  }

  public String getVersion() {
    return version;
  }

  public boolean isSnapshot() {
    return snapshot != null;
  }

  /**
   * The base version of this version. For pre-release versions, this is the target version.
   * <p>
   * For example, the version base of '1.2-rc-1' is '1.2'.
   *
   * @return The version base
   */
  public GradleVersion getBaseVersion() {
    if (stage == null && snapshot == null) {
      return this;
    }
    return version(versionPart);
  }

  public GradleVersion getNextMajor() {
    return version((majorPart + 1) + ".0");
  }

  @Override
  public int compareTo(GradleVersion gradleVersion) {
    String[] majorVersionParts = versionPart.split("\\.");
    String[] otherMajorVersionParts = gradleVersion.versionPart.split("\\.");

    for (int i = 0; i < majorVersionParts.length && i < otherMajorVersionParts.length; i++) {
      int part = Integer.parseInt(majorVersionParts[i]);
      int otherPart = Integer.parseInt(otherMajorVersionParts[i]);

      if (part > otherPart) {
        return 1;
      }
      if (otherPart > part) {
        return -1;
      }
    }
    if (majorVersionParts.length > otherMajorVersionParts.length) {
      return 1;
    }
    if (majorVersionParts.length < otherMajorVersionParts.length) {
      return -1;
    }

    if (stage != null && gradleVersion.stage != null) {
      int diff = stage.compareTo(gradleVersion.stage);
      if (diff != 0) {
        return diff;
      }
    }
    if (stage == null && gradleVersion.stage != null) {
      return 1;
    }
    if (stage != null && gradleVersion.stage == null) {
      return -1;
    }

    Long thisSnapshot = snapshot == null ? Long.MAX_VALUE : snapshot;
    Long theirSnapshot = gradleVersion.snapshot == null ? Long.MAX_VALUE : gradleVersion.snapshot;

    if (thisSnapshot.equals(theirSnapshot)) {
      return version.compareTo(gradleVersion.version);
    } else {
      return thisSnapshot.compareTo(theirSnapshot);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || o.getClass() != getClass()) {
      return false;
    }
    GradleVersion other = (GradleVersion) o;
    return version.equals(other.version);
  }

  @Override
  public int hashCode() {
    return version.hashCode();
  }

  public boolean isValid() {
    return versionPart != null;
  }

  static final class Stage implements Comparable<Stage> {

    final int stage;
    final int number;
    final Character patchNo;

    private Stage(int stage, int number, Character patchNo) {
      this.stage = stage;
      this.number = number;
      this.patchNo = patchNo;
    }

    static Stage from(int stage, String stageString) {
      Matcher m = Pattern.compile("(\\d+)([a-z])?").matcher(stageString);
      int number;
      if (m.matches()) {
        number = Integer.parseInt(m.group(1));
      } else {
        return null;
      }

      if (m.groupCount() == 2 && m.group(2) != null) {
        return new Stage(stage, number, m.group(2).charAt(0));
      } else {
        return new Stage(stage, number, '_');
      }
    }

    @Override
    public int compareTo(Stage other) {
      if (stage > other.stage) {
        return 1;
      }
      if (stage < other.stage) {
        return -1;
      }
      if (number > other.number) {
        return 1;
      }
      if (number < other.number) {
        return -1;
      }
      if (patchNo > other.patchNo) {
        return 1;
      }
      if (patchNo < other.patchNo) {
        return -1;
      }
      return 0;
    }
  }
}
