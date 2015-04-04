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
import org.gradle.BuildResult
import org.gradle.StartParameter
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.cli.CommandLineParser
import org.gradle.initialization.BuildCancellationToken
import org.gradle.initialization.DefaultCommandLineConverter
import org.gradle.initialization.GradleLauncher
import org.gradle.initialization.GradleLauncherFactory
import org.gradle.internal.nativeintegration.services.NativeServices
import org.gradle.internal.service.ServiceRegistry
import org.gradle.internal.service.ServiceRegistryBuilder
import org.gradle.internal.service.scopes.GlobalScopeServices
import org.gradle.logging.LoggingServiceRegistry
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.gradle.RatpackGroovyPlugin
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

abstract class FunctionalSpec extends Specification {
  @Rule
  TemporaryFolder dir

  static class ExecutedTask {
    Task task
    TaskState state
  }

  List<ExecutedTask> executedTasks = []

  @AutoCleanup
  ServiceRegistry services = ServiceRegistryBuilder.builder()
    .provider(new GlobalScopeServices(false))
    .parent(LoggingServiceRegistry.newEmbeddableLogging())
    .parent(NativeServices.instance)
    .build()

  GradleLauncher launcher(String... args) {
    def converter = new DefaultCommandLineConverter()
    def commandLineParser = new CommandLineParser()
    converter.configure(commandLineParser)
    def commandLine = commandLineParser.parse(args)
    StartParameter startParameter = converter.convert(commandLine, new StartParameter())
    startParameter.setProjectDir(dir.root)
    GradleLauncher launcher = services.get(GradleLauncherFactory).newInstance(startParameter, new BuildCancellationToken() {
      @Override
      boolean isCancellationRequested() {
        false
      }

      @Override
      boolean addCallback(Runnable runnable) {
        false
      }

      @Override
      void removeCallback(Runnable runnable) {

      }
    })
    executedTasks.clear()
    launcher.addListener(new TaskExecutionListener() {
      void beforeExecute(Task task) {
        getExecutedTasks() << new ExecutedTask(task: task)
      }

      void afterExecute(Task task, TaskState taskState) {
        getExecutedTasks().last().state = taskState
        taskState.metaClass.upToDate = taskState.skipMessage == "UP-TO-DATE"
      }
    })
    launcher
  }

  BuildResult run(String... args) {
    def launcher = launcher(*args)
    def result = launcher.run()
    result.rethrowFailure()
    result
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

  ExecutedTask task(String name) {
    executedTasks.find { it.task.name == name }
  }

  def setup() {
    file("settings.gradle") << "rootProject.name = 'test-app'"
    buildFile << """
      buildscript {
        repositories { jcenter() }
        dependencies { classpath 'com.github.jengelman.gradle.plugins:shadow:1.2.1' }
      }

      ext.RatpackGroovyPlugin = project.class.classLoader.loadClass('${RatpackGroovyPlugin.name}')
      apply plugin: RatpackGroovyPlugin
      apply plugin: 'com.github.johnrengelman.shadow'
      archivesBaseName = "functional-test"
      version = "1.0"
      repositories {
        maven { url "${localRepo.toURI()}" }
        jcenter()
      }
      dependencies {
        compile 'org.slf4j:slf4j-simple:1.7.12'
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
    def f = file("build/libs/functional-test-1.0-all.jar")
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

