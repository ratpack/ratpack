/*
 * Copyright 2021 the original author or authors.
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

package ratpack.gradle.functional

import com.google.common.base.StandardSystemProperty

import java.lang.management.ManagementFactory

class TestEnv {

  private static TestFile buildDir

  static boolean isDebuggerAttached() {
    ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0
  }

  static File getGradleUserHome() {
    def gradleUserHomeProperty = System.getProperty("gradleUserHome")
    if (gradleUserHomeProperty) {
      def gradleUserHome = new File(gradleUserHomeProperty)
      assert gradleUserHome.directory
      gradleUserHome
    } else {
      new File(StandardSystemProperty.USER_HOME.value(), ".gradle")
    }
  }

  static TestFile getBuildDir() {
    if (buildDir == null) {
      def projectDir = new TestFile(System.getProperty("user.dir")).absoluteFile
      if (projectDir.list().any { it.endsWith(".gradle") || it.endsWith(".gradle.kts") }) {
        projectDir.dir("build")
        def dir = projectDir.dir("build")
        if (!dir.exists()) {
          throw new IllegalStateException("Build dir $projectDir does not exist.")
        }
        buildDir = dir
      } else {
        throw new IllegalStateException("Working dir $projectDir does not appear to be a project directory.")
      }
    }

    buildDir
  }

}
