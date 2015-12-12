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

package ratpack.lazybones.fixture

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.internal.consumer.DefaultGradleConnector
import org.junit.rules.TemporaryFolder
import ratpack.test.ApplicationUnderTest

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LazybonesTemplateRatpackApp implements ApplicationUnderTest {

  final TemporaryFolder projectDirectoryProvider
  final File templateDirectory
  final String localRepoUrl

  ApplicationInstance instance

  private static class ApplicationInstance {
    final Process process
    final int port

    ApplicationInstance(Process process, int port) {
      this.process = process
      this.port = port
    }

    URI getAddress() {
      if (!running) {
        throw new IllegalStateException("App is no longer running")
      }
      new URI("http://localhost:$port/")
    }

    boolean isRunning() {
      try {
        process.exitValue()
        false
      } catch (IllegalThreadStateException ignore) {
        true
      }
    }

    void stop() {
      if (running) {
        process.destroy()
      }
    }
  }

  LazybonesTemplateRatpackApp(TemporaryFolder projectDirectoryProvider, File templateDirectory, String localRepoUrl) {
    this.projectDirectoryProvider = projectDirectoryProvider
    this.templateDirectory = templateDirectory
    this.localRepoUrl = localRepoUrl
  }

  private File getProjectDirectory() {
    projectDirectoryProvider.root
  }

  private void copyTemplateFiles() {
    FileUtils.copyDirectory(templateDirectory, projectDirectory)
  }

  private void modifyBuildFile() {
    def buildFile = new File(projectDirectory, "build.gradle")
    def lines = buildFile.readLines().collectNested {
      it.contains("jcenter()") ? ["jcenter()", "maven { url '${localRepoUrl}' } "] : it
    }.flatten()
    lines << "applicationName = 'templated'"

    buildFile.withWriter { writer ->
      lines.each { writer.println(it) }
    }
  }

  private void installDist() {
    DefaultGradleConnector connector = GradleConnector.newConnector() as DefaultGradleConnector
    connector.embedded(true)
    connector.forProjectDirectory(projectDirectory).connect().newBuild().forTasks("installDist").withArguments("-u").run()
  }

  ApplicationInstance ensureLaunched() {
    if (instance == null) {
      instance = launch()
    }
    instance
  }

  private File getApplicationLaunchScript() {
    new File(projectDirectory, "build/install/templated/bin/${SystemUtils.IS_OS_WINDOWS ? "templated.bat" : "templated"}")
  }

  private ApplicationInstance launch() {
    copyTemplateFiles()
    modifyBuildFile()
    installDist()

    def processBuilder = new ProcessBuilder(applicationLaunchScript.absolutePath).redirectErrorStream(true)
    processBuilder.environment().TEMPLATED_OPTS = "-Dratpack.port=0"
    processBuilder.directory(new File(projectDirectory, "build/install/templated"))

    def process = processBuilder.start()
    int port = -1

    def latch = new CountDownLatch(1)
    Exception startException = null
    StringBuilder processText = new StringBuilder()
    Thread.start {
      try {
        process.inputStream.eachLine { String line ->
          println line
          processText.append(line)
          processText.append('\n')
          if (latch.count) {
            if (line.contains("Exception in thread \"main\"")) {
              latch.countDown()
              startException = new RuntimeException(line)
            }
            if (line.contains("Ratpack started for http://localhost:")) {
              def matcher = (line =~ "http://localhost:(\\d+)")
              port = matcher[0][1].toString().toInteger()
              latch.countDown()
            }
          }
        }
      } catch (IOException ignore) {
        // can happen when process exits without emitting a whole line
      }
    }

    if (!latch.await(15, TimeUnit.SECONDS)) {
      throw new RuntimeException("Timeout waiting for application to start\nProcess Output:\n${processText.toString()}", startException)
    }

    new ApplicationInstance(process, port)
  }

  URI getAddress() {
    Objects.requireNonNull(ensureLaunched()).address
  }

  void close() {
    instance?.stop()
  }
}
