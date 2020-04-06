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

public class RatpackElasticConfig extends RatpackStepRegistryConfig<RatpackElasticConfig> {
  /**
   * Host to export metrics to.
   */
  private String host = "http://localhost:9200";

  /**
   * Index to export metrics to.
   */
  private String index = "metrics";

  /**
   * Index date format used for rolling indices. Appended to the index name, preceded by
   * a '-'.
   */
  private String indexDateFormat = "yyyy-MM";

  /**
   * Name of the timestamp field.
   */
  private String timestampFieldName = "@timestamp";

  /**
   * Whether to create the index automatically if it does not exist.
   */
  private boolean autoCreateIndex = true;

  /**
   * Login user of the Elastic server.
   */
  private String userName = "";

  /**
   * Login password of the Elastic server.
   */
  private String password = "";

  public String getHost() {
    return this.host;
  }

  public RatpackElasticConfig host(String host) {
    this.host = host;
    return this;
  }

  public String getIndex() {
    return this.index;
  }

  public RatpackElasticConfig index(String index) {
    this.index = index;
    return this;
  }

  public String getIndexDateFormat() {
    return this.indexDateFormat;
  }

  public RatpackElasticConfig indexDateFormat(String indexDateFormat) {
    this.indexDateFormat = indexDateFormat;
    return this;
  }

  public String getTimestampFieldName() {
    return this.timestampFieldName;
  }

  public RatpackElasticConfig timestampFieldName(String timestampFieldName) {
    this.timestampFieldName = timestampFieldName;
    return this;
  }

  public boolean isAutoCreateIndex() {
    return this.autoCreateIndex;
  }

  public RatpackElasticConfig autoCreateIndex(boolean autoCreateIndex) {
    this.autoCreateIndex = autoCreateIndex;
    return this;
  }

  public String getUserName() {
    return this.userName;
  }

  public RatpackElasticConfig userName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getPassword() {
    return this.password;
  }

  public RatpackElasticConfig password(String password) {
    this.password = password;
    return this;
  }
}
