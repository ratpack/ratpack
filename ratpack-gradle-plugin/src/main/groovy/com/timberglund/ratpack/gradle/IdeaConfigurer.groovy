package com.timberglund.ratpack.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.util.GFileUtils

import static org.apache.tools.ant.util.FileUtils.*

class IdeaConfigurer implements Action<Project> {

  private final RatpackAppSpec ratpackApp

  IdeaConfigurer(RatpackAppSpec ratpackApp) {
    this.ratpackApp = ratpackApp
  }

  @Override
  void execute(Project project) {
    def idea = project.extensions.getByType(IdeaModel)

    idea.module.scopes.RUNTIME.plus += ratpackApp.springloadedClasspath
    idea.workspace.iws.withXml { XmlProvider provider ->
      Node node = provider.asNode()

      Node runManagerConfig = node.getByName('component').find { it.'@name' == 'RunManager' }

      // Add an application configuration
      runManagerConfig.'@selected' = 'Application.Ratpack'
      runManagerConfig.append(new XmlParser().parseText("""
            <configuration default="false" name="Ratpack" type="Application" factoryName="Application">
              <extension name="coverage" enabled="false" merge="false" />
              <option name="MAIN_CLASS_NAME" value="${ratpackApp.mainClassName}" />
              <option name="VM_PARAMETERS" value="${ratpackApp.springloadedJvmArgs.collect { "&quot;$it&quot;" }.join(" ")}"  />
              <option name="PROGRAM_PARAMETERS" value="" />
              <option name="WORKING_DIRECTORY" value="${ratpackApp.appRootRelativePath}" />
              <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
              <option name="ALTERNATIVE_JRE_PATH" value="" />
              <option name="ENABLE_SWING_INSPECTOR" value="false" />
              <option name="ENV_VARIABLES" />
              <option name="PASS_PARENT_ENVS" value="true" />
              <module name="${idea.module.name}" />
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