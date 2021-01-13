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

package ratpack.gradle;

import org.gradle.api.*;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.distribution.Distribution;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.jvm.tasks.Jar;
import ratpack.gradle.continuous.RatpackContinuousRun;
import ratpack.gradle.internal.IoUtil;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

public class RatpackPlugin implements Plugin<Project> {

  private static final GradleVersion GRADLE_VERSION_BASELINE = GradleVersion.version("2.6");

  public void apply(Project project) {
    GradleVersion gradleVersion = GradleVersion.version(project.getGradle().getGradleVersion());

    if (gradleVersion.compareTo(GRADLE_VERSION_BASELINE) < 0) {
      throw new GradleException("Ratpack requires Gradle version ${GRADLE_VERSION_BASELINE.version} or later");
    }

    project.getPlugins().apply(ApplicationPlugin.class);
    project.getPlugins().apply(RatpackBasePlugin.class);

    final RatpackExtension ratpackExtension = project.getExtensions().getByType(RatpackExtension.class);

    SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    mainSourceSet.getResources().srcDir((Callable<File>) ratpackExtension::getBaseDir);

    final ApplicationPluginConvention applicationPluginConvention = project.getConvention().getPlugin(ApplicationPluginConvention.class);

    if (project.getGradle().getStartParameter().isContinuous()) {
      RatpackContinuousRun run = project.getTasks().replace("run", RatpackContinuousRun.class);

      // duplicated from application plugin
      run.setDescription("Runs this project as a JVM application");
      run.setGroup("application");
      run.setClasspath(mainSourceSet.getRuntimeClasspath());
      run.getConventionMapping().map("main", applicationPluginConvention::getMainClassName);
      run.getConventionMapping().map("jvmArgs", applicationPluginConvention::getApplicationDefaultJvmArgs);
    }

    ConfigurationContainer configurationContainer = project.getConfigurations();
    Configuration implementation = Optional.ofNullable(configurationContainer.findByName("implementation"))
      .orElseGet(() -> configurationContainer.getByName("compile"));

    implementation.getDependencies().add(ratpackExtension.getCore());

    Configuration testImplementation = Optional.ofNullable(configurationContainer.findByName("testImplementation"))
      .orElseGet(() -> configurationContainer.getByName("testCompile"));

    testImplementation.getDependencies().add(ratpackExtension.getTest());

    final Jar jarTask = (Jar) project.getTasks().getByName("jar");
    Distribution mainDistribution = project.getExtensions().getByType(DistributionContainer.class).getByName("main");
    mainDistribution.contents(new Action<CopySpec>() {
      @Override
      public void execute(CopySpec copySpec) {
        copySpec.from(mainSourceSet.getOutput(), new Action<CopySpec>() {
          @Override
          public void execute(CopySpec copySpec) {
            copySpec.into("app");
          }
        });
        copySpec.eachFile(fileCopyDetails -> {
          if (fileCopyDetails.getName().equals(jarTask.getArchiveName())) {
            fileCopyDetails.exclude();
          }
        });

      }
    });

    final CreateStartScripts startScripts = (CreateStartScripts) project.getTasks().getByName("startScripts");
    startScripts.doLast(task -> {
      String jarName = jarTask.getArchiveName();
      IoUtil.setText(
        startScripts.getUnixScript(),
        IoUtil.getText(startScripts.getUnixScript())
          .replaceAll("CLASSPATH=(\")?(.+)(\")?\n", "CLASSPATH=$1\\$APP_HOME/app:$2$3\ncd \"\\$APP_HOME/app\"\n")
          .replace(":$APP_HOME/lib/" + jarName, "")
      );

      IoUtil.setText(
        startScripts.getWindowsScript(),
        IoUtil.getText(startScripts.getWindowsScript())
          .replaceAll("set CLASSPATH=?(.+)\r\n", "set CLASSPATH=%APP_HOME%/app;$1\r\ncd \"%APP_HOME%/app\"\r\n")
          .replace(":%APP_HOME%/lib/" + jarName, "")
      );
    });

    final JavaExec runTask = (JavaExec) project.getTasks().getByName("run");
    Task configureRun = project.task("configureRun");
    configureRun.doFirst(task -> runTask.systemProperty("ratpack.development", true));

    runTask.dependsOn(configureRun);
  }

}

