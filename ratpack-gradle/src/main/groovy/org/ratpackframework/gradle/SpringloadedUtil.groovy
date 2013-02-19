package org.ratpackframework.gradle

import org.gradle.api.file.FileCollection

class SpringloadedUtil {

  final FileCollection springloadedClasspath

  private boolean lookedForSpringloaded
  private File springloadedJar

  SpringloadedUtil(FileCollection springloadedClasspath) {
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
      [String.format("-javaagent:%s", jarFile.absolutePath), "-noverify"]
    } else {
      Collections.emptyList()
    }
  }

}
