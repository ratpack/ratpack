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

public class RatpackSignalFxConfig extends RatpackStepRegistryConfig<RatpackSignalFxConfig> {
  /**
   * SignalFX access token.
   */
  private String accessToken;

  /**
   * URI to ship metrics to.
   */
  private String uri = "https://ingest.signalfx.com";

  /**
   * Uniquely identifies the app instance that is publishing metrics to SignalFx.
   * Defaults to the local host name.
   */
  private String source;

  public String getAccessToken() {
    return this.accessToken;
  }

  public RatpackSignalFxConfig accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackSignalFxConfig uri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getSource() {
    return this.source;
  }

  public RatpackSignalFxConfig source(String source) {
    this.source = source;
    return this;
  }
}
