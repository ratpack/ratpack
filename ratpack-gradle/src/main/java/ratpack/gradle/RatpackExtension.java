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

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import ratpack.gradle.internal.IoUtil;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RatpackExtension {

  public static final String GROUP = "io.ratpack";
  private final DependencyHandler dependencies;
  private File baseDir;

  public static final CompletableFuture<String> VERSION_FUTURE = new CompletableFuture<>();

  static {
    new Thread(() -> {
      try {
        URL resource = RatpackExtension.class.getClassLoader().getResource("ratpack/ratpack-version.txt");
        VERSION_FUTURE.complete(IoUtil.getText(resource).trim());
      } catch (Throwable e) {
        VERSION_FUTURE.completeExceptionally(e);
      }
    }).start();
  }

  public RatpackExtension(Project project) {
    this.dependencies = project.getDependencies();
    baseDir = project.file("src/ratpack");
  }

  public Dependency getCore() {
    return dependency("core");
  }

  public Dependency getGroovy() {
    return dependency("groovy");
  }

  public Dependency getTest() {
    return dependency("test");
  }

  public Dependency getGroovyTest() {
    return dependency("groovy-test");
  }

  public Dependency dependency(final String name) {
    return dependencies.create(GROUP + ":ratpack-" + name + ":" + getVersion());
  }

  private String getVersion() {
    try {
      return VERSION_FUTURE.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public File getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

}
