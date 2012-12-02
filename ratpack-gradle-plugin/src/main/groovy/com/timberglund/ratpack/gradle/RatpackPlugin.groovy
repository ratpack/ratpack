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
 * http://timberglund.com
 *
 */

package com.timberglund.ratpack.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.jetty.JettyPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.plugins.jetty.JettyPluginConvention

class RatpackPlugin implements Plugin<Project> {

  void apply(Project project) {
    def meta = RatpackPluginMeta.fromResource(getClass().classLoader)

    project.configure(project) {
      plugins.apply(GroovyPlugin)
      plugins.apply(WarPlugin)
      plugins.apply(JettyPlugin)

      def extension = extensions.create("ratpack", RatpackExtension, convention.findPlugin(JettyPluginConvention))
      extension.httpPort = 5000
      
      repositories {
        mavenCentral()
      }

      configurations {
        provided
      }

      dependencies {
        provided "javax.servlet:servlet-api:${meta.servletApiVersion}"
        runtime 'org.slf4j:slf4j-simple:1.6.3'
        compile "com.augusttechgroup:ratpack:${meta.ratpackVersion}"
      }

      sourceSets {
        app {
          compileClasspath += configurations.provided
          groovy {
            srcDir 'src/app/scripts'
          }
          resources {
            srcDir 'src/app/templates'
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
      }

      jettyRun {
        group = 'Ratpack'
        dependsOn << extractRatpackWebXml
        scanTargets = sourceSets.app.groovy.srcDirs
        webAppSourceDirectory = file(extractRatpackWebXml.destination).parentFile
        classpath += sourceSets.app.groovy + sourceSets.app.resources
        webXml = extractRatpackWebXml.destination
        contextPath = '/'
      }

      task('groovyCompileWatcher', type: GroovyCompileWatcherTask) {
        dependsOn << project.tasks.compileGroovy
        compileGroovy = project.tasks.compileGroovy
        group = 'Ratpack'
      }

      task('runRatpack', type: JavaExec) {
        dependsOn << project.tasks.groovyCompileWatcher
        owner.group = 'Ratpack'
        main = 'com.bleedingwolf.ratpack.RatpackRunner'
        args = ['src/app/scripts']
        classpath(runtimeClasspath + configurations.provided)
        // Put SpringLoaded here
        // JVM args to point to SpringLoaded JAR
        //   -javaagent:[absolute path to JAR], -noverify
        //   -Dspringloaded=profile=grails
        // 'org.springsource.springloaded:springloaded-core:1.1.0'
        //    in http://repo.grails.org/grails/repo
      }
    }
  }
}

