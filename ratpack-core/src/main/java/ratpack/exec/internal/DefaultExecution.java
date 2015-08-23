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

import com.google.common.reflect.TypeToken;
import io.netty.channel.EventLoop;
import ratpack.exec.ExecController;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Block;
import ratpack.registry.internal.SimpleMutableRegistry;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class DefaultExecution extends SimpleMutableRegistry implements Execution {

  private final ExecutionBacking executionBacking;
  private final EventLoop eventLoop;
  private final ExecController controller;
  private final List<AutoCloseable> closeables;

  public DefaultExecution(ExecutionBacking executionBacking, EventLoop eventLoop, ExecController controller, List<AutoCloseable> closeables) {
    this.executionBacking = executionBacking;
    this.eventLoop = eventLoop;
    this.controller = controller;
    this.closeables = closeables;
  }

  @Override
  public ExecController getController() {
    return controller;
  }

  @Override
  public EventLoop getEventLoop() {
    return eventLoop;
  }

  @Override
  public void onComplete(AutoCloseable closeable) {
    closeables.add(closeable);
  }

  @Override
  public <O> Execution addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    super.addLazy(type, supplier);
    return this;
  }

  @Override
  public <O> Execution add(TypeToken<? super O> type, O object) {
    super.add(type, object);
    return this;
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, Block continuation) throws Exception {
    executionBacking.addInterceptor(execInterceptor);
    executionBacking.intercept(ExecInterceptor.ExecType.COMPUTE, Collections.singletonList(execInterceptor).iterator(), continuation);
  }

}
