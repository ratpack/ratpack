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

import java.util.HashMap;
import java.util.Map;

public class RatpackHumioConfig extends RatpackStepRegistryConfig<RatpackHumioConfig> {
  /**
   * Humio API token.
   */
  private String apiToken;

  /**
   * Humio tags describing the data source in which metrics will be stored. Humio tags
   * are a distinct concept from Micrometer's tags. Micrometer's tags are used to divide
   * metrics along dimensional boundaries.
   */
  private Map<String, String> tags = new HashMap<>();

  /**
   * URI to ship metrics to. If you need to publish metrics to an internal proxy
   * en-route to Humio, you can define the location of the proxy with this.
   */
  private String uri = "https://cloud.humio.com";

  public String getApiToken() {
    return this.apiToken;
  }

  public RatpackHumioConfig apiToken(String apiToken) {
    this.apiToken = apiToken;
    return this;
  }

  public Map<String, String> getTags() {
    return this.tags;
  }

  public RatpackHumioConfig tags(Map<String, String> tags) {
    this.tags = tags;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackHumioConfig uri(String uri) {
    this.uri = uri;
    return this;
  }
}
