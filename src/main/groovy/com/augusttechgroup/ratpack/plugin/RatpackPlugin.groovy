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

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.jetty.JettyPlugin

class RatpackPlugin
  implements Plugin<Project> {


  void apply(Project project) {
    project.plugins.apply(GroovyPlugin)

    project.repositories {
      mavenCentral()
    }
    
    project.configurations {
      provided
    }
    
    project.dependencies {
      provided 'javax.servlet:servlet-api:2.5'
      runtime 'org.slf4j:slf4j-simple:1.6.3'
    }
    
    project.sourceSets {
      main {
        groovy {
          compileClasspath += project.configurations.provided
        }
      }
      app {
        groovy {
          srcDir 'src/app/groovy'
        }
        resources {
          srcDir 'src/app/resources'
        }
      }
    }

    project.war {
      webInf {
        from project.sourceSets.app.groovy
        into 'scripts'
      }
      webInf {
        from project.sourceSets.app.resources
        into 'classes'
      }
      webXml project.file('web.xml')
    }

    RatpackPlugin.classLoader.getResource()

  }
}

