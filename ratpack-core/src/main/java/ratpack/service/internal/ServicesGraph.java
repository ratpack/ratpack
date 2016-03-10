/*
 * Copyright 2016 the original author or authors.
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

package ratpack.service.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import ratpack.exec.ExecController;
import ratpack.exec.Operation;
import ratpack.func.Predicate;
import ratpack.registry.Registry;
import ratpack.server.Service;
import ratpack.server.StartEvent;
import ratpack.server.StartupFailureException;
import ratpack.server.StopEvent;
import ratpack.server.internal.DefaultEvent;
import ratpack.server.internal.DefaultRatpackServer;
import ratpack.service.Dependencies;
import ratpack.service.ServiceDependencies;
import ratpack.service.ServiceDependenciesSpec;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// TODO: there is no cycle detection yet.
public class ServicesGraph {

  public static final Logger LOGGER = DefaultRatpackServer.LOGGER;

  private final List<Node> nodes;
  private final AtomicInteger toStartCount = new AtomicInteger();
  private final AtomicInteger toStopCount = new AtomicInteger();

  private final CountDownLatch startLatch = new CountDownLatch(1);
  private final CountDownLatch stopLatch = new CountDownLatch(1);
  private final ExecController execController;

  private final AtomicReference<StartupFailureException> failureRef = new AtomicReference<>();
  private volatile boolean startupFailed;

  public ServicesGraph(Registry registry) throws Exception {
    this.nodes = ImmutableList.copyOf(Iterables.transform(registry.getAll(Service.class), Node::new));
    this.toStartCount.set(nodes.size());
    this.toStopCount.set(nodes.size());
    this.execController = registry.get(ExecController.class);

    defineDependencies(registry);
  }


  private void defineDependencies(Registry registry) throws Exception {
    SpecBacking specBacking = new SpecBacking();
    for (ServiceDependencies dependencies : registry.getAll(ServiceDependencies.class)) {
      dependencies.define(specBacking);
    }
    for (Node node : nodes) {
      Dependencies dependencies = node.service.getClass().getAnnotation(Dependencies.class);
      if (dependencies != null) {
        specBacking.dependsOn(
          s -> node.service.getClass().isInstance(s),
          s -> {
            for (Class<?> dependencyType : dependencies.value()) {
              if (dependencyType.isInstance(s)) {
                return true;
              }
            }
            return false;
          }
        );
      }
    }
  }

  public synchronized void start(StartEvent startEvent) throws StartupFailureException, InterruptedException {
    if (startLatch.getCount() > 0) {
      if (nodes.isEmpty()) {
        startLatch.countDown();
      } else {
        LOGGER.info("Initializing " + nodes.size() + " services...");
        nodes.forEach(n -> {
          if (n.dependencies.isEmpty()) {
            n.start(startEvent);
          }
        });
      }
    }

    startLatch.await();
    StartupFailureException startupFailureException = failureRef.get();
    if (startupFailureException != null) {
      throw startupFailureException;
    }
  }

  public synchronized void stop(StopEvent stopEvent) throws InterruptedException {
    if (stopLatch.getCount() > 0) {
      doStop(stopEvent);
    }
    stopLatch.await();
  }

  private void doStop(StopEvent stopEvent) {
    if (nodes.isEmpty()) {
      stopLatch.countDown();
    } else {
      LOGGER.info("Stopping " + nodes.size() + " services...");
      nodes.forEach(n -> {
        if (n.dependents.isEmpty()) {
          n.stop(stopEvent);
        }
      });
    }
  }

  private void serviceDidStart(StartEvent startEvent) {
    if (toStartCount.decrementAndGet() == 0) {
      if (startupFailed) {
        doStop(new DefaultEvent(startEvent.getRegistry(), false));
        StartupFailureException exception = null;
        for (Node node : nodes) {
          if (node.startError != null) {
            StartupFailureException nodeException = new StartupFailureException("Service '" + node.service.getName() + "' failed to start", node.startError);
            if (exception == null) {
              exception = nodeException;
            } else {
              exception.addSuppressed(nodeException);
            }
          }
        }
        failureRef.set(exception);
      }
      startLatch.countDown();
    }
  }

  private void serviceDidStop() {
    if (toStopCount.decrementAndGet() == 0) {
      stopLatch.countDown();
    }
  }

  private class SpecBacking implements ServiceDependenciesSpec {
    @Override
    public ServiceDependenciesSpec dependsOn(Predicate<? super Service> dependents, Predicate<? super Service> dependencies) throws Exception {
      List<Node> dependentNodes = Lists.newArrayList();
      List<Node> dependencyNodes = Lists.newArrayList();
      for (Node node : nodes) {
        boolean dependent = false;
        if (dependents.apply(node.service)) {
          dependent = true;
          dependentNodes.add(node);
        }
        if (dependencies.apply(node.service)) {
          if (dependent) {
            throw new IllegalStateException("Service '" + node.service.getName() + "' marked as dependent and dependency");
          }
          dependencyNodes.add(node);
        }
      }
      for (Node dependencyNode : dependencyNodes) {
        for (Node dependentNode : dependentNodes) {
          dependentNode.addDependency(dependencyNode);
          dependencyNode.addDependent(dependentNode);
        }
      }
      return this;
    }
  }

  private class Node {

    private final Service service;
    private final Set<Node> dependents = Sets.newHashSet();
    private final AtomicInteger dependentsToStopCount = new AtomicInteger();
    private final Set<Node> dependencies = Sets.newHashSet();
    private final AtomicInteger dependenciesToStartCount = new AtomicInteger();

    private volatile boolean started;
    private volatile Throwable startError;

    public Node(Service service) {
      this.service = service;
    }

    public void addDependency(Node node) {
      dependencies.add(node);
      dependenciesToStartCount.incrementAndGet();
    }

    public void addDependent(Node node) {
      dependents.add(node);
      dependentsToStopCount.incrementAndGet();
    }

    public void dependencyStarted(StartEvent startEvent) {
      if (dependenciesToStartCount.decrementAndGet() == 0) {
        start(startEvent);
      }
    }

    public void dependentStopped(StopEvent stopEvent) {
      if (dependentsToStopCount.decrementAndGet() == 0) {
        stop(stopEvent);
      }
    }

    public void start(StartEvent startEvent) {
      if (startupFailed) {
        serviceDidStart(startEvent);
        dependents.forEach(n -> n.dependencyStarted(startEvent));
        return;
      }

      execController.fork()
        .onComplete(e -> {
          if (startError == null) {
            started = true;
          } else {
            startupFailed = true;
          }
          dependents.forEach(n -> n.dependencyStarted(startEvent));
          serviceDidStart(startEvent);
        })
        .onError(e ->
          startError = e
        )
        .start(e -> Operation.of(() -> service.onStart(startEvent)).then());
    }

    public void stop(StopEvent stopEvent) {
      if (!started) {
        serviceDidStop();
        dependencies.forEach(n -> n.dependentStopped(stopEvent));
        return;
      }

      started = false;
      execController.fork()
        .onComplete(e -> {
          dependencies.forEach(n -> n.dependentStopped(stopEvent));
          serviceDidStop();
        })
        .onError(e ->
          LOGGER.warn("Service '" + service.getName() + "' thrown an exception while stopping.", e)
        )
        .start(e -> Operation.of(() -> service.onStop(stopEvent)).then());
    }

  }
}
