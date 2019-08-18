/*
 * Copyright 2017 the original author or authors.
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

package ratpack.gradle.continuous

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.deployment.internal.DeploymentRegistry
import org.gradle.internal.Factory
import org.gradle.process.internal.JavaExecHandleBuilder
import org.gradle.util.VersionNumber
import ratpack.gradle.continuous.run.*

import javax.inject.Inject
import java.lang.reflect.*

class RatpackContinuousRun extends JavaExec {

  private static final V2_13 = VersionNumber.parse("2.13")
  private static final V2_14 = VersionNumber.parse("2.14")
  private static final V4_2 = VersionNumber.parse("4.2")

  public boolean flattenClassloaders

  private final gradleVersion

  RatpackContinuousRun() {
    def realVersionNumber = VersionNumber.parse(project.gradle.gradleVersion)
    this.gradleVersion = new VersionNumber(realVersionNumber.major, realVersionNumber.minor, realVersionNumber.micro, null)
  }

  @TaskAction
  @Override
  void exec() {
    String deploymentId = getPath()
    DeploymentRegistry deploymentRegistry = getDeploymentRegistry()
    def loader = getClass().getClassLoader()
    def deploymentHandleClass = loader.loadClass("org.gradle.deployment.internal.DeploymentHandle")
    if (gradleVersion < V4_2) {
      RatpackAdapter deploymentHandle = (RatpackAdapter) deploymentRegistry.get(deploymentHandleClass, deploymentId)
      if (deploymentHandle == null) {
        RatpackAdapter proxy = (RatpackAdapter) Proxy.newProxyInstance(loader, [deploymentHandleClass, RatpackAdapter] as Class<?>[], new ProxyBacking(createAdapter()))
        deploymentRegistry.register(deploymentId, proxy)
        proxy.start()
      } else {
        deploymentHandle.reload()
      }
    } else {
      RatpackDeploymentHandle deploymentHandle = deploymentRegistry.get(deploymentId, RatpackDeploymentHandle)
      if (deploymentHandle == null) {
        deploymentRegistry.start(deploymentId, DeploymentRegistry.ChangeBehavior.NONE, RatpackDeploymentHandle, createAdapter())
      } else {
        deploymentHandle.reload()
      }
    }
  }

  private static final class ProxyBacking implements InvocationHandler {
    private final RatpackAdapter delegate

    ProxyBacking(RatpackAdapter delegate) {
      this.delegate = delegate
    }

    @Override
    Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("isRunning")) {
        return delegate.isRunning()
      } else if (method.getName().equals("start")) {
        delegate.start()
      } else if (method.getName().equals("reload")) {
        delegate.reload()
      } else if (method.getName().equals("stop")) {
        delegate.stop()
      }

      return null
    }
  }


  @Inject
  protected DeploymentRegistry getDeploymentRegistry() {
    throw new UnsupportedOperationException()
  }

  private Object getWorkerProcessBuilderFactory() {
    Type type
    if (gradleVersion < V2_13) {
      type = new ParameterizedType() {
        @Override
        Type[] getActualTypeArguments() {
          [getClass().classLoader.loadClass("org.gradle.process.internal.WorkerProcessBuilder")]
        }

        @Override
        Type getRawType() {
          return Factory
        }

        @Override
        Type getOwnerType() {
          return null
        }
      }
    } else if (gradleVersion < V2_14) {
      type = getClass().classLoader.loadClass("org.gradle.process.internal.WorkerProcessFactory")
    } else {
      type = getClass().classLoader.loadClass("org.gradle.process.internal.worker.WorkerProcessFactory")
    }
    (project as ProjectInternal).getServices().get(type)
  }

  private RatpackAdapter createAdapter() {
    def builder
    if (gradleVersion < V2_13) {
      builder = getWorkerProcessBuilderFactory().create()
      builder.worker(createServer())
    } else {
      builder = getWorkerProcessBuilderFactory().create(createServer())
    }
    configureWorkerProcessBuilder(builder)

    def process = builder.build()

    process.start()

    final RatpackAdapter adapter = process.getConnection().addOutgoing(RatpackAdapter)
    final Signal signal = new DefaultSignal()
    process.getConnection().addIncoming(Signal, signal)
    process.getConnection().connect()
    return new RatpackAdapter() {
      @Override
      void start() {
        adapter.start()
        signal.await()
      }

      @Override
      void reload() {
        adapter.reload()
        signal.await()
      }

      @Override
      void buildError(Throwable throwable) {
        adapter.buildError(throwable)
        signal.await()
      }

      @Override
      boolean isRunning() {
        boolean running = adapter.isRunning()
        signal.await()
        return running
      }

      @Override
      void stop() {
        adapter.stop()
      }
    }
  }

  private void configureWorkerProcessBuilder(builder) {
    builder.setBaseName("Gradle Ratpack Worker")
    builder.sharedPackages("ratpack.gradle.continuous.run")
    JavaExecHandleBuilder javaCommand = builder.getJavaCommand()
    javaCommand.setWorkingDir(getWorkingDir())
    javaCommand.setEnvironment(getEnvironment())
    javaCommand.setJvmArgs(getJvmArgs())
    javaCommand.setSystemProperties(getSystemProperties())
    javaCommand.setMinHeapSize(getMinHeapSize())
    javaCommand.setMaxHeapSize(getMaxHeapSize())
    javaCommand.setBootstrapClasspath(getBootstrapClasspath())
    javaCommand.setEnableAssertions(getEnableAssertions())
    javaCommand.setDebug(getDebug())
  }

  private RatpackWorkerServer createServer() {
    new RatpackWorkerServer(new DefaultRatpackAdapter(createRatpackSpec()))
  }

  private RatpackSpec createRatpackSpec() {
    Set<File> classpath = getClasspath().getFiles()
    List<URL> changing = new ArrayList<URL>()
    List<URL> nonChanging = new ArrayList<URL>()

    String absoluteRootDirPath = getProject().getRootDir().getAbsolutePath()
    for (File file : classpath) {
      if (flattenClassloaders || file.isDirectory() || file.getAbsolutePath().startsWith(absoluteRootDirPath)) {
        changing.add(toUrl(file))
      } else {
        nonChanging.add(toUrl(file))
      }
    }

    List<String> args = getArgs()
    return new RatpackSpec(
      nonChanging.toArray(new URL[nonChanging.size()]),
      changing.toArray(new URL[changing.size()]),
      getMain(),
      args.toArray(new String[args.size()])
    )
  }

  private static URL toUrl(File file) {
    return file.toURI().toURL()
  }

}
