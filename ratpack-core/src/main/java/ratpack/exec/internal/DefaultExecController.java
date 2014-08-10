/*
 * Copyright 2014 the original author or authors.
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

package ratpack.exec.internal;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DefaultExecController implements ExecController {

  private static final ThreadLocal<ExecController> THREAD_BINDING = new ThreadLocal<>();

  private final ListeningScheduledExecutorService computeExecutor;
  private final ListeningExecutorService blockingExecutor;
  private final EventLoopGroup eventLoopGroup;
  private final ExecControl control;
  private final int numThreads;

  public DefaultExecController(int numThreads) {
    this.numThreads = numThreads;
    this.eventLoopGroup = new NioEventLoopGroup(numThreads, new ExecControllerBindingThreadFactory("ratpack-compute", Thread.MAX_PRIORITY));
    this.computeExecutor = MoreExecutors.listeningDecorator(eventLoopGroup);
    this.blockingExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(new ExecControllerBindingThreadFactory("ratpack-blocking", Thread.NORM_PRIORITY)));
    this.control = new DefaultExecControl(this);
  }

  public static Optional<ExecController> getThreadBoundController() {
    return Optional.fromNullable(THREAD_BINDING.get());
  }

  public void close() {
    eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.SECONDS);
    blockingExecutor.shutdown();
  }

  @Override
  public ListeningScheduledExecutorService getExecutor() {
    return computeExecutor;
  }

  @Override
  public ListeningExecutorService getBlockingExecutor() {
    return blockingExecutor;
  }

  @Override
  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  @Override
  public ExecControl getControl() {
    return control;
  }

  private class ExecControllerBindingThreadFactory extends DefaultThreadFactory {
    public ExecControllerBindingThreadFactory(String name, int priority) {
      super(name, priority);
    }

    @Override
    public Thread newThread(final Runnable r) {
      return super.newThread(new Runnable() {
        @Override
        public void run() {
          THREAD_BINDING.set(DefaultExecController.this);
          r.run();
        }
      });
    }

  }

  @Override
  public boolean isManagedThread() {
    Optional<ExecController> threadBoundController = getThreadBoundController();
    return threadBoundController.isPresent() && threadBoundController.get() == this;
  }

  @Override
  public int getNumThreads() {
    return numThreads;
  }

}
