package org.ratpackframework.gradle

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

class RatpackDependencies {

  public static final String GROUP = "org.ratpack-framework.netty"
  private final version = getClass().classLoader.getResource("org/ratpackframework/ratpack-version.txt").text.trim()

  private final DependencyHandler dependencies

  RatpackDependencies(DependencyHandler dependencies) {
    this.dependencies = dependencies
  }

  Dependency getCore() {
    dependency("ratpack-core")
  }

  Dependency getGroovy() {
    dependency("ratpack-groovy")
  }

  Dependency dependency(String name) {
    dependencies.create("${GROUP}:${name}:${version}")
  }
}
