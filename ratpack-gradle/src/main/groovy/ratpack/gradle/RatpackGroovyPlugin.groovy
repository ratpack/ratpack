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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.JavaExec

class RatpackGroovyPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.plugins.apply(RatpackPlugin)
    project.plugins.apply(GroovyPlugin)

    project.mainClassName = "ratpack.groovy.launch.GroovyRatpackMain"

    def ratpackDependencies = new RatpackDependencies(project.dependencies)

    def gradleVersions = project.gradle.gradleVersion.split('\\.').collect { it.isInteger() ? it.toInteger() : 0 }

    project.dependencies {
      if ((gradleVersions[0] >= 1) && (gradleVersions[1] >= 4)) {
        compile ratpackDependencies.groovy
      } else {
        groovy ratpackDependencies.groovy        
      }
      testCompile ratpackDependencies.groovyTest
    }
  }

}
