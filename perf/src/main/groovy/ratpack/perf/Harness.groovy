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

package ratpack.perf

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import ratpack.perf.support.HtmlReportGenerator
import ratpack.perf.support.LatchResultHandler
import ratpack.perf.support.Requester
import ratpack.perf.support.SessionResults

import java.awt.*
import java.util.List
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@CompileStatic
class Harness {

  static void main(String[] args) {
    try {
      run()
      System.exit(0)
    } catch (Throwable e) {
      e.printStackTrace()
      System.exit(1)
    }
  }

  def static void run() {
    def appsBaseDir = new File(System.getProperty("appsBaseDir", "perf/build/apps")).absoluteFile
    assert appsBaseDir.exists()

    def resultsDir = new File(System.getProperty("resultsDir", "perf/build/results/${new Date().format("yyyyMMddHHmmss")}")).absoluteFile
    if (resultsDir.exists()) {
      throw new IllegalStateException("Results dir $resultsDir exists")
    }

    assert resultsDir.mkdirs()

    List<String> apps = appsBaseDir.listFiles().findAll { File it -> it.directory && (!it.name.startsWith(".")) }.collect { File it -> it.name } as List<String>

    def concurrency = Math.ceil(Runtime.runtime.availableProcessors() / 2).toInteger()
    println "Request concurrency: $concurrency"
    def executor = Executors.newFixedThreadPool(concurrency)
    def requester = new Requester("http://localhost:5050")

    // Make sure we can compile each of the apps…

    apps.each { String appName ->
      def dir = new File(appsBaseDir, appName)
      ["base", "head"].each { String version ->
        def versionDir = new File(dir, version)
        println "Connecting to $versionDir..."
        def connection = openConnection(versionDir)
        try {
          println "compiling…"
          connection.newBuild().withArguments("-u", "classes").run()
        } finally {
          connection.close()
        }
      }
    }

    // Start testing…

    def sessionResults = new SessionResults()

    apps.each { String appName ->
      def dir = new File(appsBaseDir, appName)
      ["base", "head"].each { String version ->
        def versionDir = new File(dir, version)
        println "Connecting to $versionDir..."
        def connection = openConnection(versionDir)
        try {
          List<String> endpoints = new JsonSlurper().parse(new File(versionDir, "endpoints.json")) as List<String>
          endpoints.each { String endpoint ->
            println "Testing endpoint: $endpoint"

            def endpointName = "$appName:$endpoint"
            def versionName = version

            println "starting app…"
            startApp(connection)
            println "app started"

            def warmupRequestsPerRound = 10000
            def warmupRounds = 3
            def warmupCooldown = 1
            requester.run("warmup", warmupRequestsPerRound, warmupRounds, warmupCooldown, executor, endpoint)

            def requestsPerRound = 100000
            def rounds = 10
            def cooldown = 1
            def results = requester.run("real", requestsPerRound, rounds, cooldown, executor, endpoint)

            sessionResults.endpoints[endpointName].results[versionName] = results

            println "Requests per second average: " + results.requestsPerSecond

            println "stopping..."
            requester.stopApp()
          }

          println "Done testing"
        } finally {
          println "Closing connection to $versionDir"
          connection.close()
        }
      }
    }

    println "Generating results..."

    def jsonResults = JsonOutput.prettyPrint(JsonOutput.toJson(sessionResults))
    new File(resultsDir, "results.json").text = jsonResults
    def htmlResults = new File(resultsDir, "results.html")
    htmlResults.withOutputStream {
      OutputStream out ->
        HtmlReportGenerator.generate(new ByteArrayInputStream(jsonResults.bytes), out)
    }

    Desktop.desktop.open(htmlResults)
  }

  private static void startApp(ProjectConnection connection) {
    def output = new ByteArrayOutputStream()
    def latch = new CountDownLatch(1)
    def resultHandler = new LatchResultHandler(latch)

    connection.newBuild().withArguments("-u", "run").setStandardOutput(output).setStandardError(output).run(resultHandler)

    def timeoutMins = 5
    def retryMs = 500
    def startAt = System.currentTimeMillis()
    def stopAt = startAt + (timeoutMins * 60 * 1000)

    while (latch.count && System.currentTimeMillis() < stopAt) {
      if (output.toString().lastIndexOf("Ratpack started for http://localhost:") > -1) {
        latch.countDown()
      }
      sleep retryMs
    }

    if (resultHandler.complete) {
      throw new Exception("Build finished early: ${output.toString()}")
    }

    if (resultHandler.failure) {
      throw new Exception("Build failed: ${output.toString()}", resultHandler.failure)
    }
  }

  private static ProjectConnection openConnection(File dir) {
    def connector = GradleConnector.newConnector().forProjectDirectory(dir)

    def gradleUserHome = System.getProperty("gradleUserHome")
    if (gradleUserHome) {
      connector.useGradleUserHomeDir(new File(gradleUserHome))
    }

    def gradleHome = System.getProperty("gradleHome")
    if (gradleHome) {
      connector.useInstallation(new File(gradleHome))
    }

    connector.connect()
  }
}
