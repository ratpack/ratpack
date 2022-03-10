/*
 * Copyright 2021 the original author or authors.
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

package ratpack.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaApplication;

import java.util.Optional;

public class RatpackGroovyPlugin implements Plugin<Project> {

  private static final String MAIN_CLASS_NAME = "ratpack.groovy.GroovyRatpackMain";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(RatpackPlugin.class);
    project.getPlugins().apply(GroovyPlugin.class);

    project.getExtensions().getByType(JavaApplication.class).getMainClass().convention(MAIN_CLASS_NAME);

    RatpackExtension ratpackExtension = project.getExtensions().getByType(RatpackExtension.class);

    ConfigurationContainer configurationContainer = project.getConfigurations();
    Configuration implementation = Optional.ofNullable(configurationContainer.findByName("implementation"))
      .orElseGet(() -> configurationContainer.getByName("compile"));

    implementation.getDependencies().add(ratpackExtension.getGroovy());

    Configuration testImplementation = Optional.ofNullable(configurationContainer.findByName("testImplementation"))
      .orElseGet(() -> configurationContainer.getByName("testCompile"));

    testImplementation.getDependencies().add(ratpackExtension.getGroovyTest());
  }

}
