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
import org.reactivestreams.Publisher;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.registry.internal.SimpleMutableRegistry;
import ratpack.stream.TransformablePublisher;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class DefaultExecution extends SimpleMutableRegistry implements Execution {

  private final EventLoop eventLoop;
  private final ExecController controller;
  private final List<AutoCloseable> closeables;

  public DefaultExecution(EventLoop eventLoop, ExecController controller, List<AutoCloseable> closeables) {
    this.eventLoop = eventLoop;
    this.controller = controller;
    this.closeables = closeables;
  }

  @Override
  public ExecController getController() {
    return controller;
  }

  @Override
  public Execution getExecution() {
    return getControl().getExecution();
  }

  public static ExecControl current() {
    return ExecControl.current();
  }

  @Override
  public ExecControl getControl() {
    return controller.getControl();
  }

  @Override
  public EventLoop getEventLoop() {
    return eventLoop;
  }

  @Override
  public void onCleanup(AutoCloseable autoCloseable) {
    closeables.add(autoCloseable);
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
  public <T> TransformablePublisher<T> stream(Publisher<T> publisher) {
    return getControl().stream(publisher);
  }

  @Override
  public ExecStarter exec() {
    return getControl().exec();
  }

  @Override
  public <T> Promise<T> promiseOf(T item) {
    return getControl().promiseOf(item);
  }

  @Override
  public <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return getControl().promise(action);
  }

  @Override
  public <T> Promise<T> blocking(Callable<T> blockingOperation) {
    return getControl().blocking(blockingOperation);
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, Block continuation) throws Exception {
    getControl().addInterceptor(execInterceptor, continuation);
  }
}
