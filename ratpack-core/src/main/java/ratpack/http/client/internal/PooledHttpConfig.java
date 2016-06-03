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
  public static final boolean DEFAULT_POOLED = false;
  public static final long DEFAULT_CONNECTION_TIMEOUT_NANOS = 30000000000l;
  public static final int DEFAULT_MAX_CONNECTIONS = 32;
  public static final long DEFAULT_READ_TIMEOUT_NANOS = 5000000000l;
  private boolean pooled = DEFAULT_POOLED;
  private long connectionTimeoutNanos = DEFAULT_CONNECTION_TIMEOUT_NANOS;
  private long readTimeoutNanos = DEFAULT_READ_TIMEOUT_NANOS;
  private int maxConnections = DEFAULT_MAX_CONNECTIONS;
  private boolean decompressResponse = true;

  public boolean isPooled() {
    return pooled;
  }

  public void setPooled(boolean pooled) {
    this.pooled = pooled;
  }

  public long getConnectionTimeoutNanos() {
    return connectionTimeoutNanos;
  }

  public void setConnectionTimeoutNanos(long connectionTimeoutNanos) {
    this.connectionTimeoutNanos = connectionTimeoutNanos;
  }

  public long getReadTimeoutNanos() {
    return readTimeoutNanos;
  }

  public void setReadTimeoutNanos(long readTimeoutNanos) {
    this.readTimeoutNanos = readTimeoutNanos;
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
