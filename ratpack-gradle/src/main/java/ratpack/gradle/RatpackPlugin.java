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
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.jvm.tasks.Jar;
import ratpack.gradle.internal.IoUtil;
import ratpack.gradle.internal.RatpackContinuousRunAction;
import ratpack.gradle.internal.ServiceRegistrySupplier;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class RatpackPlugin implements Plugin<Project> {

  private static final GradleVersion GRADLE_VERSION_BASELINE = GradleVersion.version("7.0");
  private static final GradleVersion GRADLE_7_1 = GradleVersion.version("7.1");

  public void apply(Project project) {
    GradleVersion gradleVersion = GradleVersion.version(project.getGradle().getGradleVersion());

    if (gradleVersion.compareTo(GRADLE_VERSION_BASELINE) < 0) {
      throw new GradleException("Ratpack requires Gradle version ${GRADLE_VERSION_BASELINE.version} or later");
    }

    project.getPlugins().apply(ApplicationPlugin.class);
    project.getPlugins().apply(RatpackBasePlugin.class);

    final RatpackExtension ratpackExtension = project.getExtensions().getByType(RatpackExtension.class);

    SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    mainSourceSet.getResources().srcDir((Callable<File>) ratpackExtension::getBaseDir);

    if (project.getGradle().getStartParameter().isContinuous()) {
      Task run = project.getTasks().getByName("run");
      Supplier<ServiceRegistry> serviceRegistrySupplier;
      serviceRegistrySupplier = new ServiceRegistrySupplier(project.getObjects());
      run.setActions(
        Collections.singletonList(
          new RatpackContinuousRunAction(
            project.getGradle().getGradleVersion(),
            project.getRootDir().getAbsolutePath(),
            serviceRegistrySupplier
          )
        )
      );
    }

    JavaExec run = (JavaExec) project.getTasks().getByName("run");
    run.systemProperty("ratpack.development", "true");

    ConfigurationContainer configurationContainer = project.getConfigurations();
    Configuration implementation = Optional.ofNullable(configurationContainer.findByName("implementation"))
      .orElseGet(() -> configurationContainer.getByName("compile"));

    implementation.getDependencies().add(ratpackExtension.getCore());

    Configuration testImplementation = Optional.ofNullable(configurationContainer.findByName("testImplementation"))
      .orElseGet(() -> configurationContainer.getByName("testCompile"));

    testImplementation.getDependencies().add(ratpackExtension.getTest());

    final Jar jarTask = (Jar) project.getTasks().getByName("jar");
    Distribution mainDistribution = project.getExtensions().getByType(DistributionContainer.class).getByName("main");
    SourceSetOutput mainSourceSetOutput = mainSourceSet.getOutput();
    Supplier<String> jarNameSupplier;
    Property<String> archiveFileName = jarTask.getArchiveFileName();
    jarNameSupplier = new Supplier<String>() {
      @Override
      public String get() {
        return archiveFileName.get();
      }
    };

    //noinspection Convert2Lambda
    mainDistribution.contents(new Action<CopySpec>() {
      @Override
      public void execute(CopySpec copySpec) {
        String jarName = jarNameSupplier.get();
        //noinspection Convert2Lambda
        copySpec.from(mainSourceSetOutput, new Action<CopySpec>() {
          @Override
          public void execute(CopySpec copySpec) {
            copySpec.into("app");
          }
        });
        copySpec.eachFile(fileCopyDetails -> {
          if (fileCopyDetails.getName().equals(jarName)) {
            fileCopyDetails.exclude();
          }
        });
      }
    });


    //noinspection Convert2Lambda
    project.getTasks().getByName("startScripts").doLast(new Action<Task>() {
      @Override
      public void execute(Task task) {
        CreateStartScripts startScripts = (CreateStartScripts) task;
        String jarName = jarNameSupplier.get();
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
      }
    });

  }

}

