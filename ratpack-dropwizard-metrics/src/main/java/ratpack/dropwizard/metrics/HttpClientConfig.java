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

package ratpack.dropwizard.metrics;

public class HttpClientConfig {

  private boolean enabled;
  private int pollingFrequencyInSeconds = 30;

  /**
   * The state of HttpClient metrics.
   *
   * @return the state of the HttpClient metrics
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the state of HttpClient metrics.
   * <p>
   * Default is false.
   * </p>
   * @param enabled True if HttpClient metrics are published. False otherwise
   * @return {@code this}
   */
  public HttpClientConfig enable(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * The frequency in seconds of which the HttpClient metrics will be refreshed.
   * <p>
   * Default is 30 seconds.
   * </p>
   *
   * @return Number of seconds to wait between polling HttpClient for metrics.
   */
  public int getPollingFrequencyInSeconds() {
    return this.pollingFrequencyInSeconds;
  }

  /**
   * The frequency in seconds of which the HttpClient metrics will be refreshed.
   * @param seconds Frequency in seconds of which to refresh HttpClient metrics.
   * @return this
   */
  public HttpClientConfig pollingFrequencyInSeconds(int seconds) {
    this.pollingFrequencyInSeconds = seconds;
    return this;
  }
}
