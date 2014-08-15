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

package ratpack.lazybones

import org.gradle.api.GradleException
import org.gradle.tooling.GradleConnectionException
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.ResultHandler
import ratpack.groovy.test.TestHttpClient
import ratpack.groovy.test.TestHttpClients
import ratpack.test.ApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

class TemplateSpec extends Specification {

  @AutoCleanup
  @Delegate
  TestHttpClient client = TestHttpClients.testHttpClient(new ApplicationUnderTest() {
    @Override
    URI getAddress() {
      "http://localhost:5050".toURI()
    }
  })

  def "can run template"() {
    given:
    def connection = GradleConnector.newConnector().forProjectDirectory(template).connect()

    when:
    startApp(connection)

    then:
    text.contains("This is the main page for your Ratpack app")

    where:
    template << new File(System.getProperty("templatesBaseDir", "ratpack-lazybones/build/lazybones-templates")).absoluteFile.listFiles()
  }

  private static void startApp(ProjectConnection connection) {
    def buildOutput = new ByteArrayOutputStream()
    def started = new CountDownLatch(1)
    String outputString = ""
    def buildException

    connection.newBuild().forTasks("run").setStandardOutput(buildOutput).setStandardError(buildOutput).run(new ResultHandler<Void>() {
      @Override
      void onComplete(Void result) {
        buildException = new GradleException("Build finished early: ${outputString}")
        started.countDown()
      }

      @Override
      void onFailure(GradleConnectionException failure) {
        buildException = new GradleException("Build failed: ${outputString}", failure)
        started.countDown()
      }
    })

    def timeoutMins = 1
    def retryMs = 500
    def startAt = System.currentTimeMillis()
    def stopAt = startAt + (timeoutMins * 60 * 1000)

    while (started.count && System.currentTimeMillis() < stopAt) {
      outputString = buildOutput.toString()
      if (outputString.lastIndexOf("Ratpack started for http://localhost:") > -1) {
        started.countDown()
      }
      if (outputString.contains("ratpack.launch.LaunchException")) {
        throw new GradleException("App failed to launch: $outputString")
      }
      sleep retryMs
    }

    if (buildException) {
      throw buildException
    }
  }

}
