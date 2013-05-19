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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.process.JavaExecSpec

class IdeaConfigurer implements Action<Project> {

  private JavaExecSpec runSpec

  IdeaConfigurer(JavaExecSpec runSpec) {
    this.runSpec = runSpec
  }

  @Override
  void execute(Project project) {
    def ideaModule = project.extensions.getByType(IdeaModel).module
    def ideaWorkspace = project.rootProject.extensions.getByType(IdeaModel).workspace

//    ideaModule.scopes.RUNTIME.plus += ratpackApp.springloadedClasspath
    ideaWorkspace.iws.withXml { XmlProvider provider ->
      Node node = provider.asNode()

      Node runManagerConfig = node.getByName('component').find { it.'@name' == 'RunManager' }

      def jvmArgs = new ArrayList<>(runSpec.allJvmArgs)
      def cpArg = jvmArgs.indexOf("-cp")
      if (cpArg < 0) {
        cpArg = jvmArgs.indexOf("-classpath")
      }

      if (cpArg >= 0) {
        jvmArgs.remove(cpArg) // -cp
        jvmArgs.remove(cpArg) // associated value
      }

      runManagerConfig.append(new XmlParser().parseText("""
            <configuration default="false" name="Ratpack Run (${ideaModule.name})" type="Application" factoryName="Application">
              <extension name="coverage" enabled="false" merge="false" />
              <option name="MAIN_CLASS_NAME" value="${runSpec.main}" />
              <option name="VM_PARAMETERS" value="${jvmArgs.collect { "&quot;$it&quot;" }.join(" ")}"  />
              <option name="PROGRAM_PARAMETERS" value="" />
              <option name="WORKING_DIRECTORY" value="${runSpec.workingDir.absolutePath}" />
              <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
              <option name="ALTERNATIVE_JRE_PATH" value="" />
              <option name="ENABLE_SWING_INSPECTOR" value="false" />
              <option name="ENV_VARIABLES" />
              <option name="PASS_PARENT_ENVS" value="true" />
              <module name="${ideaModule.name}" />
              <envs />
              <RunnerSettings RunnerId="Debug">
                <option name="DEBUG_PORT" value="63810" />
                <option name="TRANSPORT" value="0" />
                <option name="LOCAL" value="true" />
              </RunnerSettings>
              <RunnerSettings RunnerId="Run" />
              <ConfigurationWrapper RunnerId="Debug" />
              <ConfigurationWrapper RunnerId="Run" />
              <method />
            </configuration>
        """))
    }

  }
}