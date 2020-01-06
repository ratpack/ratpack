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

public class RatpackKairosConfig extends RatpackStepRegistryConfig<RatpackKairosConfig> {
  /**
   * URI of the KairosDB server.
   */
  private String uri = "http://localhost:8080/api/v1/datapoints";

  /**
   * Login user of the KairosDB server.
   */
  private String userName;

  /**
   * Login password of the KairosDB server.
   */
  private String password;

  public String getUri() {
    return this.uri;
  }

  public RatpackKairosConfig uri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getUserName() {
    return this.userName;
  }

  public RatpackKairosConfig userName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getPassword() {
    return this.password;
  }

  public RatpackKairosConfig password(String password) {
    this.password = password;
    return this;
  }
}
