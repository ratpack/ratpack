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

package ratpack.http.client;

public class HostStats {
  private final long activeConnectionCount;
  private final long idleConnectionCount;
  private final long totalConnectionCount;

  public HostStats(long activeConnectionCount, long idleConnectionCount) {
    this.activeConnectionCount = activeConnectionCount;
    this.idleConnectionCount = idleConnectionCount;
    this.totalConnectionCount = activeConnectionCount + idleConnectionCount;
  }

  public long getActiveConnectionCount() {
    return activeConnectionCount;
  }

  public long getIdleConnectionCount() {
    return idleConnectionCount;
  }

  public long getTotalConnectionCount() {
    return totalConnectionCount;
  }
}
