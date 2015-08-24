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
package ratpack.dropwizard.metrics;

public class ConsoleConfig extends ScheduledReporterConfigSupport<ConsoleConfig> {
  private boolean enabled = true;

  /**
   * The state of the Console publisher.
   *
   * @return the state of the Console publisher
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the state of the Console publisher.
   *
   * @param enabled True if metrics are published to the console. False otherwise
   * @return {@code this}
   */
  public ConsoleConfig enable(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

}
