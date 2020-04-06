/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics.config;

public class RatpackDynatraceConfig extends RatpackStepRegistryConfig<RatpackDynatraceConfig> {
  /**
   * Dynatrace authentication token.
   */
  private String apiToken;

  /**
   * ID of the custom device that is exporting metrics to Dynatrace.
   */
  private String deviceId;

  /**
   * Technology type for exported metrics. Used to group metrics under a logical
   * technology name in the Dynatrace UI.
   */
  private String technologyType = "java";

  /**
   * URI to ship metrics to. Should be used for SaaS, self managed instances or to
   * en-route through an internal proxy.
   */
  private String uri;

  public String getApiToken() {
    return this.apiToken;
  }

  public RatpackDynatraceConfig apiToken(String apiToken) {
    this.apiToken = apiToken;
    return this;
  }

  public String getDeviceId() {
    return this.deviceId;
  }

  public RatpackDynatraceConfig deviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

  public String getTechnologyType() {
    return this.technologyType;
  }

  public RatpackDynatraceConfig technologyType(String technologyType) {
    this.technologyType = technologyType;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackDynatraceConfig uri(String uri) {
    this.uri = uri;
    return this;
  }
}
