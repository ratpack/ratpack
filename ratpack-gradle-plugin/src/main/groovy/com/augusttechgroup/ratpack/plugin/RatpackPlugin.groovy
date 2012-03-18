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
 * The August Technology Group
 * http://augusttechgroup.com/tim/about
 *
 */

package com.augusttechgroup.ratpack.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.jetty.JettyPlugin
import org.gradle.api.tasks.JavaExec

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {
    project.plugins.apply(GroovyPlugin)
    project.plugins.apply(WarPlugin)
    project.plugins.apply(JettyPlugin)

//    if(!project.property('ratpackCoreVersion')) {
//      project.setProperty('ratpackCoreVersion', project.version)
//    }

    project.repositories {
      mavenCentral()
    }

    project.configurations {
      ratpack
    }

    project.dependencies {
      provided 'javax.servlet:servlet-api:2.5'
      runtime 'org.slf4j:slf4j-simple:1.6.3'
      runtime "com.augusttechgroup:ratpack-core:0.5-SNAPSHOT"
      ratpack "com.augusttechgroup:ratpack-core:0.5-SNAPSHOT"
    }

    project.sourceSets {
      compileClasspath += project.configurations.provided
      app {
        groovy {
          srcDir 'src/app/groovy'
        }
        resources {
          srcDir 'src/app/resources'
        }
      }
    }


    project.task('prepareWarResources') {
      webXmlFilename = "${project.buildDir}/war/WEB-INF/web.xml"
      def warSourceFilename = RatpackPlugin.classLoader.getResource('web.xml').file.split('!')[0]
      inputs.file warSourceFilename
      outputs.file webXmlFilename
      doLast {
        def warFile = project.file(webXmlFilename)
        warFile.parentFile.mkdirs()
        warFile.withPrintWriter { pw ->
          pw.println(RatpackPlugin.classLoader.getResource('web.xml').text)
        }
      }
    }

    project.war {
      dependsOn('prepareWarResources')
      webInf {
        from project.sourceSets.app.groovy
        into 'scripts'
      }
      webInf {
        from project.sourceSets.app.resources
        into 'classes'
      }
      webXml = project.file(project.prepareWarResources.webXmlFilename)
    }

    project.jettyRunWar {
      group = 'Ratpack'
      contextPath = '/'
      httpPort = 5000
    }

    // TODO jettyRun is broken for now. Will have to unpack parts of the core JAR for it to work
    project.jettyRun {
      group = 'Ratpack'
      dependsOn << 'prepareWarResources'
      scanTargets = [project.file(project.sourceSets.app.groovy)]
      webAppSourceDirectory = project.file(project.prepareWarResources.webXmlFilename).parentFile
      classpath += project.sourceSets.app.groovy + project.sourceSets.app.resources
      webXml = project.file("${project.prepareWarResources.webXmlFilename}/web.xml")
      contextPath = '/'
      httpPort = 5000
    }

    project.task('runRatpack', type: JavaExec) {
      group = 'Ratpack'
      main = 'com.bleedingwolf.ratpack.RatpackRunner'
      args = ['src/app/resources/scripts/app.groovy']
      classpath(project.runtimeClasspath + project.configurations.provided)
    }
  }
}

