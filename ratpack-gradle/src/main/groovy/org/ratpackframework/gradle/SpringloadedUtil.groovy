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
