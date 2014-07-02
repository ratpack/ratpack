/*
 * Copyright 2013 the original author or authors.
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

package ratpack.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.ide.idea.IdeaPlugin

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {
    
    def gradleVersion = project.gradle.gradleVersion.split('\\.').collect({ it.isInteger() ? it.toInteger() : 0 }).join()

    if (gradleVersion[0] == '0' || (gradleVersion as Integer) < 16) {
      throw new GradleException("Ratpack expects Gradle version 1.6 or later")
    }

    project.plugins.apply(JavaPlugin)
    project.plugins.apply(ApplicationPlugin)

    project.configurations { springloaded }

    def ratpackApp = new SpringloadedUtil(project, project.configurations['springloaded'])

    def ratpackDependencies = project.extensions.create("ratpack", RatpackDependencies, project.dependencies)

    project.dependencies {
      compile ratpackDependencies.core
      testCompile ratpackDependencies.test
    }

    def configureRun = project.task("configureRun")
    configureRun.doFirst {
      JavaExec runTask = project.tasks.findByName("run") as JavaExec
      runTask.with {
        classpath ratpackApp.springloadedClasspath
        jvmArgs ratpackApp.springloadedJvmArgs
        systemProperty "ratpack.reloadable", true
      }
    }

    JavaExec run = project.run {
      dependsOn configureRun
      workingDir = project.file("src/ratpack")
    }

    project.mainClassName = "ratpack.launch.RatpackMain"

    SourceSetContainer sourceSets = project.sourceSets
    def mainSourceSet = sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
    def testSourceSet = sourceSets[SourceSet.TEST_SOURCE_SET_NAME]
    testSourceSet.resources.srcDir(run.workingDir)

    def prepareBaseDirTask = project.tasks.create("prepareBaseDir")
    prepareBaseDirTask.with {
      group = "Ratpack"
      description = "Lifecycle task for all tasks that contribute content to 'src/ratpack' (add dependencies to this task)"
    }

    def appPluginConvention = project.getConvention().getPlugin(ApplicationPluginConvention)
    appPluginConvention.applicationDistribution.from(run.workingDir) {
      into "app"
    }

    appPluginConvention.applicationDistribution.from prepareBaseDirTask.taskDependencies
    run.dependsOn prepareBaseDirTask
    project.tasks.getByName("processTestResources").dependsOn prepareBaseDirTask

    CreateStartScripts startScripts = project.startScripts
    startScripts.with {
      doLast {
        unixScript.text = unixScript.text.replaceAll('CLASSPATH=.+\n', '$0cd "\\$APP_HOME/app"\n')
        windowsScript.text = windowsScript.text.replaceAll('CLASSPATH=.+\r\n', '$0cd "%APP_HOME%/app"\r\n')
      }
    }

    FileCollection runtimeDependencies = mainSourceSet.runtimeClasspath
    def fatJarTask = project.tasks.create("fatJar", Jar)
    fatJarTask.with {
      group = "ratpack"
      description = "Builds the Ratpack as a single executable JAR"
      inputs.files(runtimeDependencies)
      classifier = "fat"
      from run.workingDir
      from {
        runtimeDependencies.collect {
          if (it.name.endsWith(".zip") || it.name.endsWith(".jar")) {
            project.zipTree(it)
          } else {
            project.files(it)
          }
        }
      }
      manifest.attributes.put "Main-Class", "${->appPluginConvention.mainClassName}"
    }

    def runFatJarTask = project.tasks.create("run${fatJarTask.name.capitalize()}", Exec)
    runFatJarTask.with {
      group = "ratpack"
      description = "Executes the fat JAR built by the ${fatJarTask.name} task"
      dependsOn fatJarTask
      executable = "java"
      args "-jar", fatJarTask.archivePath.absolutePath
    }

    project.plugins.withType(IdeaPlugin) {
      project.rootProject.ideaWorkspace.dependsOn(configureRun)
      new IdeaConfigurer(run).execute(project)
    }
  }

}

