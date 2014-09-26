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

package ratpack.hystrix.internal;

import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.UnmanagedThreadException;

import java.util.Optional;

class HystrixRegistryBackedRequestVariable<T> implements HystrixRequestVariable<T> {

  private HystrixRequestVariableLifecycle<T> rv;

  public HystrixRegistryBackedRequestVariable(HystrixRequestVariableLifecycle<T> rv) {
    this.rv = rv;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get() {
    return getExecution()
      .map(e -> {
        HystrixCommandCache commandCache = e.maybeGet(HystrixCommandCache.class);

        if (commandCache == null) {
          commandCache = new HystrixCommandCache();
          e.register(commandCache);
        }

        Object command = commandCache.get(this);
        if (command == null) {
          command = rv.initialValue();
          commandCache.putIfAbsent(this, command);
        }

        return (T) command;
      })
      .orElseThrow(UnmanagedThreadException::new);
  }

  private Optional<Execution> getExecution() {
    return ExecController.current().map(c -> c.getControl().getExecution());
  }

  @Override
  public T initialValue() {
    return rv.initialValue();
  }

  @Override
  public void shutdown(T value) {
    rv.shutdown(value);
  }
}
