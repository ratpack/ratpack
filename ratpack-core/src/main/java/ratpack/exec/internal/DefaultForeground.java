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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.EventLoopGroup;
import ratpack.exec.ExecContext;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Foreground;
import ratpack.exec.NoBoundContextException;
import ratpack.func.Action;
import ratpack.func.Supplier;
import ratpack.handling.Context;
import ratpack.handling.internal.HandlerException;
import ratpack.handling.internal.InterceptedOperation;
import ratpack.util.ExceptionUtils;

import java.util.List;

public class DefaultForeground implements Foreground {

  private final ThreadLocal<Supplier<? extends ExecContext>> contextFactoryThreadLocal = new ThreadLocal<>();
  private final ThreadLocal<Runnable> onExecFinish = new ThreadLocal<>();

  private final ListeningScheduledExecutorService listeningScheduledExecutorService;
  private final EventLoopGroup eventLoopGroup;

  public DefaultForeground(EventLoopGroup eventLoopGroup) {
    this.eventLoopGroup = eventLoopGroup;
    this.listeningScheduledExecutorService = MoreExecutors.listeningDecorator(eventLoopGroup);
  }

  @Override
  public ExecContext getContext() throws NoBoundContextException {
    Supplier<? extends ExecContext> contextFactory = contextFactoryThreadLocal.get();
    if (contextFactory == null) {
      throw new NoBoundContextException("No context is bound to the current thread (are you calling this from the background?)");
    } else {
      return contextFactory.get();
    }
  }

  @Override
  public ListeningScheduledExecutorService getExecutor() {
    return listeningScheduledExecutorService;
  }

  @Override
  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  @Override
  public void exec(Supplier<? extends ExecContext> execContextSupplier, List<ExecInterceptor> interceptors, final ExecInterceptor.ExecType execType, final Action<? super ExecContext> action) {

    try {
      contextFactoryThreadLocal.set(execContextSupplier);
      new InterceptedOperation(execType, interceptors) {
        @Override
        protected void performOperation() throws Exception {
          action.execute(getContext());
        }
      }.run();
      Runnable runnable = onExecFinish.get();
      onExecFinish.remove();
      if (runnable != null) {
        runnable.run();
      }
    } catch (Exception e) {
      if (e instanceof HandlerException) {
        Context context = ((HandlerException) e).getContext();
        context.error(ExceptionUtils.toException(e.getCause()));
      } else {
        getContext().error(e);
      }
    } finally {
      contextFactoryThreadLocal.remove();
    }
  }

  @Override
  public void onExecFinish(Runnable runnable) {
    if (onExecFinish.get() != null) {
      throw new IllegalStateException("onExecFinish callback already registered");
    }
    onExecFinish.set(runnable);
  }

}
