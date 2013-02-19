/*
 * Copyright 2012 Tim Berglund
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
 *
 * Tim Berglund
 * http://timberglund.com
 *
 */

package org.ratpackframework.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.plugins.ide.idea.IdeaPlugin

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {
    project.plugins.apply(JavaPlugin)
    project.plugins.apply(ApplicationPlugin)

    project.configurations { springloaded }

    def ratpackApp = new SpringloadedUtil(project.configurations['springloaded'])

    def ratpackDependencies = new RatpackDependencies(project.dependencies)

    project.dependencies {
      compile ratpackDependencies.core
    }

    def configureRun = project.task("configureRun")
    configureRun.doFirst {
      project.run {
        classpath ratpackApp.springloadedClasspath
        jvmArgs ratpackApp.springloadedJvmArgs
      }
    }

    JavaExec run = project.run {
      dependsOn configureRun
      workingDir = project.file("src/ratpack")
    }

    project.installApp {
      from run.workingDir
    }

    project.plugins.withType(IdeaPlugin) {
      project.rootProject.ideaWorkspace.dependsOn(configureRun)
      new IdeaConfigurer(run).execute(project)
    }
  }

}

