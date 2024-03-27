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

package ratpack.gradle.internal;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.internal.Factory;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.process.internal.JavaExecHandleBuilder;
import ratpack.gradle.GradleVersion;
import ratpack.gradle.continuous.RatpackDeploymentHandle;
import ratpack.gradle.continuous.run.*;

import java.io.File;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RatpackContinuousRunAction implements Action<Task> {
  private static final GradleVersion V2_13 = GradleVersion.version("2.13");
  private static final GradleVersion V2_14 = GradleVersion.version("2.14");
  private static final GradleVersion V4_2 = GradleVersion.version("4.2");
  private static final String FLATTEN_CLASSLOADERS = "ratpack.flattenClassloaders";

  private final GradleVersion gradleVersion;
  private final String absoluteRootDirPath;
  private final Supplier<ServiceRegistry> serviceRegistrySupplier;

  public RatpackContinuousRunAction(
    String gradleVersion,
    String absoluteRootDirPath,
    Supplier<ServiceRegistry> serviceRegistrySupplier
  ) {
    this.serviceRegistrySupplier = serviceRegistrySupplier;
    this.gradleVersion = GradleVersion.version(gradleVersion);
    this.absoluteRootDirPath = absoluteRootDirPath;
  }

  @Override
  public void execute(Task untyped) {
    JavaExec task = (JavaExec) untyped;
    ServiceRegistry services = serviceRegistrySupplier.get();
    String deploymentId = task.getPath();
    DeploymentRegistry deploymentRegistry = services.get(DeploymentRegistry.class);
    ClassLoader loader = getClass().getClassLoader();
    Class<?> deploymentHandleClass = loadClass(loader, "org.gradle.deployment.internal.DeploymentHandle");
    if (gradleVersion.compareTo(V4_2) < 0) {

      RatpackAdapter deploymentHandle = (RatpackAdapter) Invoker.of(DeploymentRegistry.class, "get", Class.class, deploymentHandleClass)
        .invoke(deploymentRegistry, deploymentHandleClass, deploymentId);

      if (deploymentHandle == null) {
        RatpackAdapter proxy = (RatpackAdapter) Proxy.newProxyInstance(loader, new Class<?>[]{deploymentHandleClass, RatpackAdapter.class}, new ProxyBacking(createAdapter(task, services)));
        Invoker.of(DeploymentRegistry.class, "register", String.class, Object.class)
          .invoke(deploymentRegistry, deploymentId, proxy);
        proxy.start();
      } else {
        deploymentHandle.reload();
      }
    } else {
      RatpackDeploymentHandle deploymentHandle = deploymentRegistry.get(deploymentId, RatpackDeploymentHandle.class);
      if (deploymentHandle == null) {
        deploymentRegistry.start(deploymentId, DeploymentRegistry.ChangeBehavior.NONE, RatpackDeploymentHandle.class, createAdapter(task, services));
      } else {
        deploymentHandle.reload();
      }

    }
  }

  private static Class<?> loadClass(ClassLoader loader, String name) {
    try {
      return loader.loadClass(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private Object getWorkerProcessBuilderFactory(ServiceRegistry services) {
    Type type;
    if (gradleVersion.compareTo(V2_13) < 0) {
      type = new ParameterizedType() {
        @Override
        public Type[] getActualTypeArguments() {
          return new Class<?>[]{loadClass(getClass().getClassLoader(), "org.gradle.process.internal.WorkerProcessBuilder")};
        }

        @Override
        public Type getRawType() {
          return Factory.class;
        }

        @Override
        public Type getOwnerType() {
          return null;
        }

      };
    } else if (gradleVersion.compareTo(V2_14) < 0) {
      type = loadClass(getClass().getClassLoader(), "org.gradle.process.internal.WorkerProcessFactory");
    } else {
      type = loadClass(getClass().getClassLoader(), "org.gradle.process.internal.worker.WorkerProcessFactory");
    }

    return services.get(type);
  }

  private RatpackAdapter createAdapter(JavaExec task, ServiceRegistry services) {
    Object builder;
    if (gradleVersion.compareTo(V2_13) < 0) {
      builder = Invoker.invokeParamless(Factory.class, getWorkerProcessBuilderFactory(services), "create");
      Invoker.of("org.gradle.process.internal.worker.WorkerProcessBuilder", "worker", Action.class).invoke(builder, createServer(task));
    } else {
      Class<?> factoryClass;
      if (gradleVersion.compareTo(V2_14) < 0) {
        factoryClass = loadClass(getClass().getClassLoader(), "org.gradle.process.internal.WorkerProcessFactory");
      } else {
        factoryClass = loadClass(getClass().getClassLoader(), "org.gradle.process.internal.worker.WorkerProcessFactory");
      }
      builder = Invoker.of(factoryClass, "create", Action.class).invoke(getWorkerProcessBuilderFactory(services), createServer(task));
    }

    Class<?> workerProcessBuilderClass = loadClass(getClass().getClassLoader(), "org.gradle.process.internal.worker.WorkerProcessBuilder");
    configureWorkerProcessBuilder(workerProcessBuilderClass, builder, task);

    Object process = Invoker.invokeParamless(workerProcessBuilderClass, builder, "build");
    Invoker.invokeParamless("org.gradle.process.internal.worker.WorkerProcess", process, "start");

    Object connection = Invoker.invokeParamless("org.gradle.process.internal.worker.WorkerProcess", process, "getConnection");
    final RatpackAdapter adapter = (RatpackAdapter) Invoker.of("org.gradle.internal.remote.ObjectConnection", "addOutgoing", Class.class)
      .invoke(connection, RatpackAdapter.class);
    final Signal signal = new DefaultSignal();
    Invoker.of("org.gradle.internal.remote.ObjectConnection", "addIncoming", Class.class, Object.class)
      .invoke(connection, Signal.class, signal);
    Invoker.invokeParamless("org.gradle.internal.remote.ObjectConnection", connection, "connect");
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

  private void configureWorkerProcessBuilder(Class<?> clazz, Object builder, JavaExec task) {
    Invoker.of(clazz, "setBaseName", String.class).invoke(builder, "Gradle Ratpack Worker");
    Invoker.of(clazz, "sharedPackages", String[].class).invoke(builder, (Object) new String[]{"ratpack.gradle.continuous.run"});
    JavaExecHandleBuilder javaCommand = (JavaExecHandleBuilder) Invoker.invokeParamless(clazz, builder, "getJavaCommand");
    javaCommand.setWorkingDir(task.getWorkingDir());
    javaCommand.setEnvironment(task.getEnvironment());
    javaCommand.setJvmArgs(task.getJvmArgs());
    javaCommand.setSystemProperties(task.getSystemProperties());
    javaCommand.setMinHeapSize(task.getMinHeapSize());
    javaCommand.setMaxHeapSize(task.getMaxHeapSize());
    javaCommand.setBootstrapClasspath(task.getBootstrapClasspath());
    javaCommand.setEnableAssertions(task.getEnableAssertions());
    javaCommand.setDebug(task.getDebug());
  }

  private RatpackWorkerServer createServer(JavaExec task) {
    return new RatpackWorkerServer(new DefaultRatpackAdapter(createRatpackSpec(task)));
  }

  private RatpackSpec createRatpackSpec(JavaExec task) {
    Set<File> classpath = task.getClasspath().getFiles();
    List<URL> changing = new ArrayList<>();
    List<URL> nonChanging = new ArrayList<>();
    boolean flattenClassloaders = task.getExtensions().getExtraProperties().has(FLATTEN_CLASSLOADERS);
    for (File file : classpath) {
      if (flattenClassloaders || file.isDirectory() || file.getAbsolutePath().startsWith(absoluteRootDirPath)) {
        changing.add(toUrl(file));
      } else {
        nonChanging.add(toUrl(file));
      }
    }

    List<String> args = task.getArgs();
    return new RatpackSpec(nonChanging.toArray(new URL[0]),
      changing.toArray(new URL[0]),
      task.getMainClass().get(),
      args.toArray(new String[0])
    );
  }

  private static URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  final private static class ProxyBacking implements InvocationHandler {
    public ProxyBacking(RatpackAdapter delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      switch (method.getName()) {
        case "isRunning":
          return delegate.isRunning();
        case "start":
          delegate.start();
          break;
        case "reload":
          delegate.reload();
          break;
        case "stop":
          delegate.stop();
          break;
      }

      return null;
    }

    private final RatpackAdapter delegate;
  }

}
