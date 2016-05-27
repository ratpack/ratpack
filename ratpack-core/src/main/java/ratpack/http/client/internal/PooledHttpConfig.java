/*
 * Copyright 2016 the original author or authors.
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

package ratpack.http.client.internal;

public class PooledHttpConfig {
  public static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 30000;
  public static final int DEFAULT_MAX_CONNECTIONS = 32;
  public static final int DEFAULT_READ_TIMEOUT_MILLIS = 5000;

  private int connectionTimeoutMillis = DEFAULT_CONNECTION_TIMEOUT_MILLIS;
  private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;
  private int maxConnections = DEFAULT_MAX_CONNECTIONS;
  private boolean decompressResponse = true;

  public int getConnectionTimeoutMillis() {
    return connectionTimeoutMillis;
  }

  public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
    this.connectionTimeoutMillis = connectionTimeoutMillis;
  }

  public int getReadTimeoutMillis() {
    return readTimeoutMillis;
  }

  public void setReadTimeoutMillis(int readTimeoutMillis) {
    this.readTimeoutMillis = readTimeoutMillis;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public void setMaxConnections(int maxConnections) {
    this.maxConnections = maxConnections;
  }

  public boolean isDecompressResponse() {
    return decompressResponse;
  }

  public void setDecompressResponse(boolean decompressResponse) {
    this.decompressResponse = decompressResponse;
  }
}
