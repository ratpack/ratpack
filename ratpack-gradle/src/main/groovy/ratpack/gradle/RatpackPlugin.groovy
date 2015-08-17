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
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.plugins.ide.idea.IdeaPlugin

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {

    def gradleVersions = project.gradle.gradleVersion.split('\\.').collect { it.isInteger() ? it.toInteger() : 0 }
    def major = gradleVersions[0]

    if (major < 2) {
      throw new GradleException("Ratpack requires Gradle version 2.0 or later")
    }

    project.plugins.apply(JavaPlugin)
    project.plugins.apply(ApplicationPlugin)
    project.plugins.apply(RatpackBasePlugin)

    project.configurations { springloaded }

    def ratpackApp = new SpringloadedUtil(project, project.configurations['springloaded'])

    RatpackExtension ratpackExtension = project.extensions.findByType(RatpackExtension)

    project.dependencies {
      compile ratpackExtension.core
      testCompile ratpackExtension.test
    }

    def configureRun = project.task("configureRun")
    configureRun.doFirst {
      JavaExec runTask = project.tasks.findByName("run") as JavaExec
      runTask.with {
        classpath ratpackApp.springloadedClasspath
        jvmArgs ratpackApp.springloadedJvmArgs
        systemProperty "ratpack.development", true
        systemProperty "ratpack.baseDir.override", ratpackExtension.baseDir.absolutePath
      }
    }

    JavaExec run = project.run {
      dependsOn configureRun
    }

    SourceSetContainer sourceSets = project.sourceSets
    def mainSourceSet = sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
    mainSourceSet.resources.srcDir {
      ratpackExtension.baseDir
    }

    def prepareBaseDirTask = project.tasks.create("prepareBaseDir")
    prepareBaseDirTask.with {
      group = "Ratpack"
      description = "Lifecycle task for all tasks that contribute content to Ratpack base directory (default: 'src/ratpack') (add dependencies to this task)"
    }

    def appPluginConvention = project.getConvention().getPlugin(ApplicationPluginConvention)
    appPluginConvention.applicationDistribution.from({ ratpackExtension.baseDir }) {
      into "app"
    }

    appPluginConvention.applicationDistribution.from prepareBaseDirTask.taskDependencies
    project.tasks.getByName("processResources").dependsOn prepareBaseDirTask

    CreateStartScripts startScripts = project.startScripts
    startScripts.with {
      doLast {
        unixScript.text = unixScript.text.replaceAll('CLASSPATH=.+\n', '$0cd "\\$APP_HOME/app"\n')
        windowsScript.text = windowsScript.text.replaceAll('CLASSPATH=.+\r\n', '$0cd "%APP_HOME%/app"\r\n')
      }
    }

    project.plugins.withType(IdeaPlugin) {
      project.rootProject.ideaWorkspace.dependsOn(configureRun)
      new IdeaConfigurer(run).execute(project)
    }
  }

}

