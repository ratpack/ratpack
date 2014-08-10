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

import ratpack.exec.ExecControl;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.registry.internal.SimpleMutableRegistry;

import java.util.List;

public class DefaultExecution extends SimpleMutableRegistry implements Execution {

  private final ExecController controller;
  private final List<AutoCloseable> closeables;

  public DefaultExecution(ExecController controller, List<AutoCloseable> closeables) {
    this.controller = controller;
    this.closeables = closeables;
  }

  @Override
  public ExecController getController() {
    return controller;
  }

  @Override
  public ExecControl getControl() {
    return controller.getControl();
  }

  @Override
  public void onCleanup(AutoCloseable autoCloseable) {
    closeables.add(autoCloseable);
  }

}
