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

package com.augusttechgroup.ratpack.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.jetty.JettyPlugin
import org.gradle.api.tasks.JavaExec

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {
    def meta = RatpackPluginMeta.fromResource(getClass().classLoader)

    project.configure(project) {
      plugins.apply(GroovyPlugin)
      plugins.apply(WarPlugin)
      plugins.apply(JettyPlugin)

      repositories {
        mavenCentral()
      }

      dependencies {
        provided 'javax.servlet:servlet-api:' + meta.servletApiVersion
        runtime 'org.slf4j:slf4j-simple:1.6.3'
        runtime "com.augusttechgroup:ratpack-core:" + meta.ratpackVersion
      }

      sourceSets {
        compileClasspath += configurations.provided
        app {
          groovy {
            srcDir 'src/app/groovy'
          }
          resources {
            srcDir 'src/app/resources'
          }
        }
      }

      task('extractRatpackWebXml', type: ExtractRatpackWebXml)

      war {
        dependsOn extractRatpackWebXml
        webInf {
          from sourceSets.app.groovy
          into 'scripts'
        }
        webInf {
          from sourceSets.app.resources
          into 'classes'
        }
        webXml = extractRatpackWebXml.destination
      }

      jettyRunWar {
        group = 'Ratpack'
        contextPath = '/'
        httpPort = 5000
      }

      // TODO jettyRun is broken for now. Will have to unpack parts of the core JAR for it to work
      jettyRun {
        group = 'Ratpack'
        dependsOn << 'prepareWarResources'
        scanTargets = [file(sourceSets.app.groovy)]
        webAppSourceDirectory = file(prepareWarResources.webXmlFilename).parentFile
        classpath += sourceSets.app.groovy + sourceSets.app.resources
        webXml = file("${prepareWarResources.webXmlFilename}/web.xml")
        contextPath = '/'
        httpPort = 5000
      }

      task('runRatpack', type: JavaExec) {
        owner.group = 'Ratpack'
        main = 'com.bleedingwolf.ratpack.RatpackRunner'
        args = ['src/app/resources/scripts/app.groovy']
        classpath(runtimeClasspath + configurations.provided)
      }
    }
  }
}

