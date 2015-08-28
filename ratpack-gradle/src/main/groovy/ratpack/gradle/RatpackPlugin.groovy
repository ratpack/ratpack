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
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.jvm.tasks.Jar
import ratpack.gradle.continuous.RatpackContinuousRun

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {
    def gradleVersions = project.gradle.gradleVersion.split('\\.').collect { it.isInteger() ? it.toInteger() : 0 }
    def major = gradleVersions[0]
    def minor = gradleVersions[1]

    if (major < 2 || (major == 2 && minor < 6)) {
      throw new GradleException("Ratpack requires Gradle version 2.6 or later")
    }

    project.plugins.apply(JavaPlugin)
    project.plugins.apply(ApplicationPlugin)
    project.plugins.apply(RatpackBasePlugin)

    RatpackExtension ratpackExtension = project.extensions.findByType(RatpackExtension)

    project.dependencies {
      compile ratpackExtension.core
      testCompile ratpackExtension.test
    }

    SourceSetContainer sourceSets = project.sourceSets

    def mainSourceSet = sourceSets[SourceSet.MAIN_SOURCE_SET_NAME]
    mainSourceSet.resources.srcDir { ratpackExtension.baseDir }

    Jar jarTask = project.tasks.jar as Jar
    def mainDistribution = project.extensions.getByType(DistributionContainer).getByName("main")
    mainDistribution.contents {
      from(mainSourceSet.output) {
        into "app"
      }
      eachFile {
        if (it.name == jarTask.archiveName) {
          it.exclude()
        }
      }
    }

    CreateStartScripts startScripts = project.startScripts
    startScripts.with {
      doLast {
        unixScript.text = unixScript.text.replaceAll('CLASSPATH=(")?(.+)(")?\n', 'CLASSPATH=$1\\$APP_HOME/app:$2$3\ncd "\\$APP_HOME/app"\n')
        windowsScript.text = windowsScript.text.replaceAll('CLASSPATH=(")?(.+)(")\r\n', 'CLASSPATH=$1%APP_HOME%/app;$2$3\r\ncd "%APP_HOME%/app"\r\n')
      }
    }

    JavaExec runTask = project.tasks.findByName("run") as JavaExec

    def configureRun = project.task("configureRun")
    configureRun.doFirst {
      runTask.with {
        systemProperty "ratpack.development", true
      }
    }

    runTask.dependsOn configureRun

    project.tasks.create("devRun", RatpackContinuousRun) {
      it.execSpec = runTask
      it.dependsOn configureRun
    }
  }

}

