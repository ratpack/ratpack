/*
 * Copyright 2016 the original author or authors.
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

package ratpack.test.internal.snippets.executer

import groovy.transform.CompileStatic
import org.gradle.testkit.runner.GradleRunner
import ratpack.test.internal.snippets.TestCodeSnippet
import ratpack.test.internal.snippets.fixture.SnippetFixture

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@CompileStatic
class GradleSnippetExecuter implements SnippetExecuter {

  private final SnippetFixture fixture

  private static final Lock LOCK = new ReentrantLock()
  private static final Condition AVAILABLE = LOCK.newCondition()
  private static volatile boolean busy

  GradleSnippetExecuter(SnippetFixture fixture) {
    this.fixture = fixture
  }

  @Override
  SnippetFixture getFixture() {
    return fixture
  }

  @Override
  void execute(TestCodeSnippet snippet) throws Exception {
    try {
      LOCK.lock()
      while (busy) {
        AVAILABLE.await()
      }
      busy = true
      doExecute(snippet)
    } finally {
      busy = false
      AVAILABLE.signal()
      LOCK.unlock()
    }
  }

  private static void doExecute(TestCodeSnippet snippet) {
    def projectDir = File.createTempDir()
    def buildFile = new File(projectDir, "build.gradle")

    File localRepo
    String localRepoProp = System.getProperty("localRepo")
    if (localRepoProp) {
      localRepo = new File(localRepoProp)
      assert localRepo.directory
    } else {
      localRepo = new File("build/localrepo")
      if (!localRepo.directory) {
        localRepo = new File("../build/localrepo")
        if (!localRepo.directory) {
          throw new Exception("could not find local repo")
        }
      }
    }

    def localRepoPath = localRepo.canonicalPath?.replaceAll("\\\\", "/")

    try {
      buildFile.text = "buildscript { repositories { maven { url 'file://${localRepoPath}' } } }\n" + snippet.snippet
      def runner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withDebug(true)

      def gradleUserHome = System.getProperty("gradleUserHome")
      if (gradleUserHome) {
        runner.withTestKitDir(new File(gradleUserHome))
      }

      def gradleHome = System.getProperty("gradleHome")
      if (gradleHome) {
        runner.withGradleInstallation(new File(gradleHome))
      }

      runner.build()

    } finally {
      projectDir.deleteDir()
    }

  }
}
