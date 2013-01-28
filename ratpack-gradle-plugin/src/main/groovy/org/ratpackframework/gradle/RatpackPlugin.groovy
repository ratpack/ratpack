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
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.plugins.ide.idea.IdeaPlugin

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {
    def version = getClass().classLoader.getResource("org/ratpackframework/ratpack-version.txt").text.trim()

    project.plugins.apply(GroovyPlugin)
    project.plugins.apply(ApplicationPlugin)

    project.repositories {
      mavenCentral()
    }

    project.configurations {
      springloaded
    }

    def ratpackApp = new RatpackAppSpec(
        project,
        project.file("src/ratpack"),
        project.configurations['compile'],
        project.configurations['runtime'],
        project.configurations['springloaded']
    )

    project.mainClassName = ratpackApp.mainClassName

    project.dependencies {
      runtime 'org.slf4j:slf4j-simple:1.6.3'
      compile "org.ratpackframework:ratpack-core:${version}"
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
      main = ratpackApp.mainClassName
      workingDir = project.file("src/ratpack")
    }

    project.installApp {
      from ratpackApp.appRoot
    }

    project.plugins.withType(IdeaPlugin) {
      new IdeaConfigurer(ratpackApp, run).execute(project)
    }
  }

}

