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

import java.util.Map;

public class HttpClientStats {

  private final Map<String, HostStats> statsPerHost;

  public HttpClientStats(Map<String, HostStats> statsPerHost) {
    this.statsPerHost = statsPerHost;
  }

  public Map<String, HostStats> getStatsPerHost() {
    return statsPerHost;
  }

  /**
   * @return The sum of {@link #getTotalActiveConnectionCount()} and {@link #getTotalIdleConnectionCount()},
   * a long representing the total number of connections in the connection pool.
   */
  public long getTotalConnectionCount() {
    return getTotalActiveConnectionCount() + getTotalIdleConnectionCount();
  }

  /**
   * @return A long representing the number of active connections in the connection pool.
   */
  public long getTotalActiveConnectionCount() {
    return statsPerHost
      .values()
      .stream()
      .mapToLong(HostStats::getActiveConnectionCount)
      .sum();
  }

  /**
   * @return A long representing the number of idle connections in the connection pool.
   */
  public long getTotalIdleConnectionCount() {
    return statsPerHost
      .values()
      .stream()
      .mapToLong(HostStats::getIdleConnectionCount)
      .sum();
  }

  @Override
  public String toString() {
    return "There are " + getTotalConnectionCount()
      + " total connections, " + getTotalActiveConnectionCount()
      + " are active and " + getTotalIdleConnectionCount() + " are idle.";
  }

}
