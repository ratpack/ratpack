/*
 * Copyright 2012 the original author or authors.
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

package com.augusttechgroup.ratpack.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ExtractRatpackWebXml extends DefaultTask {
  
  @OutputFile
  File destination = project.file("${project.buildDir}/ratpack/WEB-INF/web.xml")
  
  private URL resource

  ExtractRatpackWebXml() {
    resource = getClass().classLoader.getResource('web.xml')

    def originJarPath = resource.file.split('!')[0]
    inputs.file originJarPath
  }

  @TaskAction
  void doExtractRatpackWebXml() {
    def resource = this.resource
    destination.withOutputStream { out -> resource.withInputStream { out << it }}
  }
}
