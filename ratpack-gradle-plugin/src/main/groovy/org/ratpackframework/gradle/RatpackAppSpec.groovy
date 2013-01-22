package org.ratpackframework.gradle

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

import static org.apache.tools.ant.util.FileUtils.getRelativePath

class RatpackAppSpec {

  final Project project
  final File appRoot

  final FileCollection compileClasspath
  final FileCollection runtimeClasspath
  final FileCollection springloadedClasspath

  private boolean lookedForSpringloaded
  private File springloadedJar

  RatpackAppSpec(Project project, File appRoot, FileCollection compileClasspath, FileCollection runtimeClasspath, FileCollection springloadedClasspath) {
    this.project = project
    this.appRoot = appRoot
    this.compileClasspath = compileClasspath
    this.runtimeClasspath = runtimeClasspath
    this.springloadedClasspath = springloadedClasspath
  }

  File getSpringloadedJar() {
    if (!lookedForSpringloaded) {
      springloadedJar = JarFinder.find("org.springsource.loaded.SpringLoaded", springloadedClasspath.files)
      lookedForSpringloaded = true
    }
    springloadedJar
  }

  List<String> getSpringloadedJvmArgs() {
    def jarFile = getSpringloadedJar()
    if (jarFile) {
      ["-Dspringloaded=profile=grails", String.format("-javaagent:%s", jarFile.absolutePath), "-noverify"]
    } else {
      Collections.emptyList()
    }
  }

  String getMainClassName() {
    'org.ratpackframework.RatpackMain'
  }

  String getAppRootRelativePath() {
    getRelativePath(project.projectDir, appRoot)
  }
}
