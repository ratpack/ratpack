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

import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.gradle.deployment.internal.DeploymentHandle;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.internal.Factory;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.WorkerProcess;
import org.gradle.process.internal.WorkerProcessBuilder;
import ratpack.gradle.continuous.run.*;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RatpackContinuousRun extends JavaExec {

  public boolean flattenClassloaders;

  @TaskAction
  @Override
  public void exec() {
    String deploymentId = getPath();
    DeploymentRegistry deploymentRegistry = getDeploymentRegistry();
    RatpackAdapter deploymentHandle = (RatpackAdapter) deploymentRegistry.get(DeploymentHandle.class, deploymentId);
    if (deploymentHandle == null) {
      RatpackAdapter proxy = (RatpackAdapter) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{DeploymentHandle.class, RatpackAdapter.class}, new ProxyBacking(new RatpackDeploymentHandle(createAdapter())));
      deploymentRegistry.register(deploymentId, (DeploymentHandle) proxy);
      proxy.start();
    } else {
      deploymentHandle.reload();
    }
  }

  private static final class ProxyBacking implements InvocationHandler {
    private final RatpackAdapter delegate;

    public ProxyBacking(RatpackAdapter delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("isRunning")) {
        return delegate.isRunning();
      } else if (method.getName().equals("start")) {
        delegate.start();
      } else if (method.getName().equals("reload")) {
        delegate.reload();
      } else if (method.getName().equals("stop")) {
        delegate.stop();
      }

      return null;
    }
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
    javaCommand.setWorkingDir(getWorkingDir());
    javaCommand.setEnvironment(getEnvironment());
    javaCommand.setJvmArgs(getJvmArgs());
    javaCommand.setSystemProperties(getSystemProperties());
    javaCommand.setMinHeapSize(getMinHeapSize());
    javaCommand.setMaxHeapSize(getMaxHeapSize());
    javaCommand.setBootstrapClasspath(getBootstrapClasspath());
    javaCommand.setEnableAssertions(getEnableAssertions());
    javaCommand.setDebug(getDebug());
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
    Set<File> classpath = getClasspath().getFiles();
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

    List<String> args = getArgs();
    return new RatpackSpec(
      nonChanging.toArray(new URL[nonChanging.size()]),
      changing.toArray(new URL[changing.size()]),
      getMain(),
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
