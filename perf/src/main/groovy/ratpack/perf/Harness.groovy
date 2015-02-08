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
import groovy.util.logging.Slf4j
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import ratpack.perf.support.HtmlReportGenerator
import ratpack.perf.support.LatchResultHandler
import ratpack.perf.support.Requester
import ratpack.perf.support.SessionResults

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

//@CompileStatic
@Slf4j
class Harness {

  static void main(String[] args) {
    try {
      run()
      System.exit(0)
    } catch (Throwable e) {
      log.error "", e
      System.exit(1)
    }
  }

  static void run() {
    def appsBaseDir = new File(System.getProperty("appsBaseDir", "perf/build/apps")).absoluteFile
    assert appsBaseDir.exists()

    def resultsDir = new File(System.getProperty("resultsDir", "perf/build/results/${new Date().format("yyyyMMddHHmmss")}")).absoluteFile
    if (resultsDir.exists()) {
      throw new IllegalStateException("Results dir $resultsDir exists")
    }

    def filters = new Filters([:])
    def filterString = System.getProperty("filter", "")
    if (filterString) {
      def filtersList = filterString.split(",")
      def filtersData = filtersList.groupBy { it.split(":")[0] }.collectEntries {
        [it.key, it.value.collect { it.split(":")[1] }]
      }

      def cast = (Map<String, List<String>>) filtersData
      filters = new Filters(cast)
    }

    assert resultsDir.mkdirs()

    LinkedList<String> apps = appsBaseDir.listFiles().findAll { File it -> it.directory && (!it.name.startsWith(".")) }.collect { File it -> it.name } as LinkedList<String>

    def concurrency = Math.ceil(Runtime.runtime.availableProcessors() * 2).toInteger()
    log.debug "Request concurrency: $concurrency"
    def executor = Executors.newFixedThreadPool(concurrency)
    def requester = new Requester("http://localhost:5050")

    // Make sure we can compile each of the apps…

    apps.each { String appName ->
      if (!filters.testApp(appName)) {
        log.debug "skipping $appName as it is filtered out"
        return
      }
      def dir = new File(appsBaseDir, appName)
      ["base", "head"].each { String version ->
        def versionDir = new File(dir, version)
        log.debug "Connecting to $versionDir..."
        def connection = openConnection(versionDir)
        try {
          log.info "compiling…"
          connection.newBuild().withArguments("-u", "classes").run()
        } finally {
          connection.close()
        }
      }
    }

    def warmup = new Settings(10000, 3, 1)
    def real = new Settings(100000, 10, 1)

    if (Boolean.getBoolean("smoke")) {
      warmup = new Settings(10, 1, 1)
      real = warmup
    }

    if (Boolean.getBoolean("quick")) {
      real = new Settings(10000, 5, 1)
    }

    // Start testing…

    def sessionResults = new SessionResults()

    apps.each { String appName ->
      if (!filters.testApp(appName)) {
        log.debug "skipping $appName as it is filtered out"
        return
      }

      def dir = new File(appsBaseDir, appName)
      ["base", "head"].each { String version ->
        def versionDir = new File(dir, version)
        log.debug "Connecting to $versionDir..."
        def connection = openConnection(versionDir)
        try {
          def endpoints = new JsonSlurper().parse(new File(versionDir, "endpoints.json")) as List<String>
          endpoints.each { String endpoint ->
            if (!filters.testEndpoint(appName, endpoint)) {
              log.debug "skipping $appName:$endpoint as it is filtered out"
              return
            }


            log.info "Testing endpoint: $appName $endpoint @ $version"

            String endpointName = "$appName:$endpoint"
            def versionName = version

            log.info "starting app…"
            startApp(connection, endpoint)
            log.info "app started"

            try {
              requester.run("warmup", warmup, executor, endpoint)
              def results = requester.run("real", real, executor, endpoint)

              sessionResults.endpoints[endpointName].results[versionName] = results

              log.info "Average ms per request: " + results.msPerRequest
            } catch (Throwable e) {
              log.error "Exception while testing app", e
            } finally {
              log.info "stopping..."
              requester.stopApp()
            }
          }

          log.info "Done testing"
        } finally {
          log.debug "Closing connection to $versionDir"
          connection.close()
        }
      }
    }

    log.info "Generating results..."

    def jsonResults = JsonOutput.prettyPrint(JsonOutput.toJson(sessionResults))
    new File(resultsDir, "results.json").text = jsonResults
    def htmlResults = new File(resultsDir, "results.html")
    htmlResults.withOutputStream {
      OutputStream out ->
        HtmlReportGenerator.generate(new ByteArrayInputStream(jsonResults.bytes), out)
    }

    //noinspection UnnecessaryQualifiedReference
    if (java.awt.Desktop.isDesktopSupported()) {
      //noinspection UnnecessaryQualifiedReference
      java.awt.Desktop.desktop.open(htmlResults)
    } else {
      log.info "Results available at file://${htmlResults.absolutePath}"
    }
  }

  private static void startApp(ProjectConnection connection, String endpoint) {
    def output = new ByteArrayOutputStream()
    def latch = new CountDownLatch(1)
    def resultHandler = new LatchResultHandler(latch)

    connection.newBuild()
      .setJvmArguments("-Xmx512m")
      .withArguments("-u", "run", "-Pendpoint=$endpoint")
      .setStandardOutput(output)
      .setStandardError(output).run(resultHandler)

    def timeoutMins = 1
    def retryMs = 500
    def startAt = System.currentTimeMillis()
    def stopAt = startAt + (timeoutMins * 60 * 1000)

    String outputString = ""
    while (latch.count && System.currentTimeMillis() < stopAt) {
      outputString = output.toString()
      if (outputString.lastIndexOf("Ratpack started for http://localhost:") > -1) {
        latch.countDown()
      }
      if (outputString.contains("ratpack.server.LaunchException")) {
        throw new RuntimeException("App failed to launch: $outputString")
      }
      sleep retryMs
    }

    if (resultHandler.complete) {
      throw new Exception("Build finished early: ${outputString}")
    }

    if (resultHandler.failure) {
      throw new Exception("Build failed: ${outputString}", resultHandler.failure)
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

