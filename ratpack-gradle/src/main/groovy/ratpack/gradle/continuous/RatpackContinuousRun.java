/*
 * Copyright 2015 the original author or authors.
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

package ratpack.gradle.continuous;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.internal.Factory;
import org.gradle.process.JavaExecSpec;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.WorkerProcess;
import org.gradle.process.internal.WorkerProcessBuilder;
import ratpack.gradle.continuous.run.*;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class RatpackContinuousRun extends DefaultTask {

  public JavaExecSpec execSpec;
  public boolean flattenClassloaders;

  @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
  public RatpackContinuousRun() {
    getInputs().files(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        return execSpec.getClasspath();
      }
    });
  }

  @TaskAction
  public void start() {
    if (!isContinuous()) {
      throw new GradleException("Cannot use task " + getPath() + " unless running with --continuous (or -t)");
    }

    String deploymentId = getPath();
    DeploymentRegistry deploymentRegistry = getDeploymentRegistry();
    RatpackDeploymentHandle deploymentHandle = deploymentRegistry.get(RatpackDeploymentHandle.class, deploymentId);
    if (deploymentHandle == null) {
      deploymentHandle = new RatpackDeploymentHandle(createAdapter());
      deploymentRegistry.register(deploymentId, deploymentHandle);
      deploymentHandle.start();
    } else {
      deploymentHandle.reload();
    }
  }

  private boolean isContinuous() {
    return getProject().getGradle().getStartParameter().isContinuous();
  }

  @Inject
  protected DeploymentRegistry getDeploymentRegistry() {
    throw new UnsupportedOperationException();
  }

  @Inject
  protected Factory<WorkerProcessBuilder> getWorkerProcessBuilderFactory() {
    throw new UnsupportedOperationException();
  }

  private RatpackAdapter createAdapter() {
    WorkerProcessBuilder builder = getWorkerProcessBuilderFactory().create();
    builder.setBaseName("Gradle Ratpack Worker");
    builder.sharedPackages("ratpack.gradle.continuous.run");
    JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
    javaCommand.setWorkingDir(execSpec.getWorkingDir());
    javaCommand.setEnvironment(execSpec.getEnvironment());
    javaCommand.setJvmArgs(execSpec.getJvmArgs());
    javaCommand.setSystemProperties(execSpec.getSystemProperties());
    javaCommand.setMinHeapSize(execSpec.getMinHeapSize());
    javaCommand.setMaxHeapSize(execSpec.getMaxHeapSize());
    javaCommand.setBootstrapClasspath(execSpec.getBootstrapClasspath());
    javaCommand.setEnableAssertions(execSpec.getEnableAssertions());
    javaCommand.setDebug(execSpec.getDebug());
    WorkerProcess process = builder.worker(new RatpackWorkerServer(new DefaultRatpackAdapter(createRatpackSpec()))).build();
    process.start();

    final RatpackAdapter adapter = process.getConnection().addOutgoing(RatpackAdapter.class);
    final Signal signal = new DefaultSignal();
    process.getConnection().addIncoming(Signal.class, signal);
    process.getConnection().connect();
    return new RatpackAdapter() {
      @Override
      public void start() {
        adapter.start();
        signal.await();
      }

      @Override
      public void reload() {
        adapter.reload();
        signal.await();
      }

      @Override
      public void buildError(Throwable throwable) {
        adapter.buildError(throwable);
        signal.await();
      }

      @Override
      public boolean isRunning() {
        boolean running = adapter.isRunning();
        signal.await();
        return running;
      }

      @Override
      public void stop() {
        adapter.stop();
      }
    };
  }

  private RatpackSpec createRatpackSpec() {
    Set<File> classpath = execSpec.getClasspath().getFiles();
    List<URL> changing = new ArrayList<URL>();
    List<URL> nonChanging = new ArrayList<URL>();

    String absoluteRootDirPath = getProject().getRootDir().getAbsolutePath();
    for (File file : classpath) {
      if (flattenClassloaders || file.isDirectory() || file.getAbsolutePath().startsWith(absoluteRootDirPath)) {
        changing.add(toUrl(file));
      } else {
        nonChanging.add(toUrl(file));
      }
    }

    List<String> args = execSpec.getArgs();
    return new RatpackSpec(
      nonChanging.toArray(new URL[nonChanging.size()]),
      changing.toArray(new URL[changing.size()]),
      execSpec.getMain(),
      args.toArray(new String[args.size()])
    );
  }

  private URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

}
