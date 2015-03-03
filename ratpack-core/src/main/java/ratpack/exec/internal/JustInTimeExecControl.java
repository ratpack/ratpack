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

package ratpack.exec.internal;

import org.reactivestreams.Publisher;
import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.NoArgAction;
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.Callable;

public class JustInTimeExecControl implements ExecControl {

  public static final ExecControl INSTANCE = new JustInTimeExecControl();

  private ExecControl getDelegate() {
    return ExecControl.current();
  }

  @Override
  public Execution getExecution() {
    return getDelegate().getExecution();
  }

  @Override
  public ExecController getController() {
    return getDelegate().getController();
  }

  @Override
  public void addInterceptor(ExecInterceptor execInterceptor, NoArgAction continuation) throws Exception {
    getDelegate().addInterceptor(execInterceptor, continuation);
  }

  @Override
  public <T> Promise<T> blocking(Callable<T> blockingOperation) {
    return getDelegate().blocking(blockingOperation);
  }

  @Override
  public <T> Promise<T> promise(Action<? super Fulfiller<T>> action) {
    return getDelegate().promise(action);
  }

  @Override
  public ExecStarter exec() {
    return getDelegate().exec();
  }

  @Override
  public <T> TransformablePublisher<T> stream(Publisher<T> publisher) {
    return getDelegate().stream(publisher);
  }
}
