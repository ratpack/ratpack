/*
 * Copyright 2013 the original author or authors.
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

package ratpack.server.internal;

import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.launch.LaunchConfig;
import ratpack.server.RatpackServer;

import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;

public class ServiceBackedServer implements RatpackServer {

  private final RatpackService ratpackService;
  private final LaunchConfig launchConfig;

  public ServiceBackedServer(RatpackService ratpackService, LaunchConfig launchConfig) {
    this.ratpackService = ratpackService;
    this.launchConfig = launchConfig;
  }

  public LaunchConfig getLaunchConfig() {
    return launchConfig;
  }

  public boolean isRunning() {
    return ratpackService.isRunning();
  }

  public void start() throws Exception {
    try {
      ratpackService.start().get();
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw toException(e.getCause());
    }
  }

  public void stop() throws Exception {
    try {
      ratpackService.stop().get();
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw toException(e.getCause());
    }
  }

  @Override
  public String getScheme() {
    return ratpackService.getScheme();
  }

  public int getBindPort() {
    return ratpackService.getBindPort();
  }

  public String getBindHost() {
    return ratpackService.getBindHost();
  }

}
