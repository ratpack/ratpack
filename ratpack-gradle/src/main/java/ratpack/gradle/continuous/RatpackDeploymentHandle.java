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

package ratpack.gradle.continuous;

import org.gradle.deployment.internal.Deployment;
import org.gradle.deployment.internal.DeploymentHandle;
import ratpack.gradle.continuous.run.RatpackAdapter;

import javax.inject.Inject;

public class RatpackDeploymentHandle implements DeploymentHandle {

  private final RatpackAdapter adapter;

  @Inject
  public RatpackDeploymentHandle(RatpackAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  public boolean isRunning() {
    return adapter.isRunning();
  }

  @Override
  public void start(Deployment deployment) {
    adapter.start();
  }

  @Override
  public void stop() {
    adapter.stop();
  }

  public void reload() {
    adapter.reload();
  }

}
