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

import java.net.URI;

public class RatpackWavefrontConfig extends RatpackStepRegistryConfig<RatpackWavefrontConfig> {
  /**
   * URI to ship metrics to.
   */
  private URI uri = URI.create("https://longboard.wavefront.com");

  /**
   * Unique identifier for the app instance that is the source of metrics being
   * published to Wavefront. Defaults to the local host name.
   */
  private String source;

  /**
   * API token used when publishing metrics directly to the Wavefront API host.
   */
  private String apiToken;

  /**
   * Global prefix to separate metrics originating from this app's white box
   * instrumentation from those originating from other Wavefront integrations when
   * viewed in the Wavefront UI.
   */
  private String globalPrefix;

  public String getUri() {
    return this.uri.toString();
  }

  public RatpackWavefrontConfig uri(URI uri) {
    this.uri = uri;
    return this;
  }

  public String getSource() {
    return this.source;
  }

  public RatpackWavefrontConfig source(String source) {
    this.source = source;
    return this;
  }

  public String getApiToken() {
    return this.apiToken;
  }

  public RatpackWavefrontConfig apiToken(String apiToken) {
    this.apiToken = apiToken;
    return this;
  }

  public String getGlobalPrefix() {
    return this.globalPrefix;
  }

  public RatpackWavefrontConfig globalPrefix(String globalPrefix) {
    this.globalPrefix = globalPrefix;
    return this;
  }
}
