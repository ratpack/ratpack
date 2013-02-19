package org.ratpackframework.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin

class RatpackGroovyPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.plugins.apply(RatpackPlugin)
    project.plugins.apply(GroovyPlugin)

    project.mainClassName = "org.ratpackframework.groovy.bootstrap.RatpackMain"

    def ratpackDependencies = new RatpackDependencies(project.dependencies)

    project.dependencies {
      groovy ratpackDependencies.groovy
    }
  }

}
