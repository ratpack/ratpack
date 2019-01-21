/*
 * Copyright 2012 the original author or authors.
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
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class FunctionalSpec extends Specification {
  @Rule
  TemporaryFolder dir

  String gradleVersion

  private static final String RATPACK_VERSION = FunctionalSpec.classLoader.getResource("ratpack/ratpack-version.txt").text.trim()

  GradleRunner runner(String... args) {
    def runner = GradleRunner.create()
      .withProjectDir(dir.root)
      .forwardOutput()
      .withTestKitDir(getTestKitDir())
      .withArguments(args.toList())

    if (gradleVersion) {
      runner.withGradleVersion(gradleVersion)
    }

    runner


  }

  BuildResult run(String... args) {
    runner(args).build()
  }

  BuildResult fail(String... args) {
    runner(args).buildAndFail()
  }

  private static File getTestKitDir() {
    def gradleUserHome = System.getenv("GRADLE_USER_HOME")
    if (!gradleUserHome) {
      gradleUserHome = new File(System.getProperty("user.home"), ".gradle").absolutePath
    }
    return new File(gradleUserHome, "testkit")
  }

  File getBuildFile() {
    makeFile("build.gradle")
  }

  File makeFile(String path) {
    def f = file(path)
    if (!f.exists()) {
      def parts = path.split("/")
      if (parts.size() > 1) {
        dir.newFolder(*parts[0..-2])
      }
      dir.newFile(path)
    }
    f
  }

  File file(String path) {
    def file = new File(dir.root, path)
    assert file.parentFile.mkdirs() || file.parentFile.exists()
    file
  }


  def setup() {
    file("settings.gradle") << "rootProject.name = 'test-app'"
    buildFile << """
      buildscript {
        repositories {
          maven { url "${localRepo.toURI()}" }
          jcenter()
        }
        repositories { jcenter() }
        dependencies {
          classpath 'com.github.jengelman.gradle.plugins:shadow:4.0.3'
          classpath 'io.ratpack:ratpack-gradle:${RATPACK_VERSION}'
        }
      }

      apply plugin: 'io.ratpack.ratpack-groovy'
      apply plugin: 'com.github.johnrengelman.shadow'
      version = "1.0"
      repositories {
        maven { url "${localRepo.toURI()}" }
        jcenter()
      }
      dependencies {
        compile 'org.slf4j:slf4j-simple:1.7.25'
      }
      shadowJar {
        // Needed in Gradle 5.1 due to breaking change in convention mapping
        // Can be removed once Shadow 5.0.0 is available
        classifier = 'all'
      }
    """

    file("src/ratpack/ratpack.properties") << "port=0\n"
  }

  def unzip(File source, File destination) {
    def project = ProjectBuilder.builder().withProjectDir(dir.root).build()
    project.copy {
      from project.zipTree(source)
      into destination
    }
  }

  File getDistZip() {
    def f = file("build/distributions/test-app-1.0.zip")
    assert f.exists()
    f
  }

  File getShadowJar() {
    def f = file("build/libs/test-app-1.0-all.jar")
    assert f.exists()
    f
  }

  File getLocalRepo() {
    def rootRelative = new File("build/localrepo")
    rootRelative.directory ? rootRelative : new File(new File(StandardSystemProperty.USER_DIR.value()).parentFile, "build/localrepo")
  }

  int scrapePort(Process process) {
    int port = -1

    def latch = new CountDownLatch(1)
    Thread.start {
      process.errorStream.eachLine { String line ->
        System.err.println(line)
        if (latch.count) {
          if (line.contains("Ratpack started for http://localhost:")) {
            def matcher = (line =~ "http://localhost:(\\d+)")
            port = matcher[0][1].toString().toInteger()
            latch.countDown()
          }
        }
      }
    }

    if (!latch.await(15, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout waiting for application to start")
    }

    port
  }
}
