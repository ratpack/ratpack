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

package ratpack.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import ratpack.configuration.internal.DefaultLaunchConfigFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Configuration for a Ratpack application.  To add custom configuration, subclass this class and add additional fields as needed.
 */
public class Configuration {
  @Valid
  @NotNull
  private LaunchConfigFactory launchConfigFactory = new DefaultLaunchConfigFactory();

  @JsonProperty("launchConfig")
  public LaunchConfigFactory getLaunchConfigFactory() {
    return launchConfigFactory;
  }

  @JsonProperty("launchConfig")
  public void setLaunchConfigFactory(LaunchConfigFactory launchConfigFactory) {
    this.launchConfigFactory = launchConfigFactory;
  }
}
