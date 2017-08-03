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

package ratpack.http.client.internal

import spock.lang.Specification

class HttpClientStatsSpec extends Specification {

  void 'it should collect HttpClientStats'() {
    given:
    Map<String, HostStats> statsPerHost = [
      'ratpack.io': new HostStats(5, 95),
      'alpha.ratpack.io': new HostStats(5, 95),
      'omega.ratpack.io': new HostStats(5, 95),
    ]

    when:
    HttpClientStats stats = new HttpClientStats(statsPerHost)

    then:
    assert stats.statsPerHost == statsPerHost
    assert stats.totalActiveConnectionCount == 15
    assert stats.totalIdleConnectionCount == 285
    assert stats.totalConnectionCount == 300
    assert stats.statsPerHost.get('ratpack.io').totalConnectionCount == 100
    assert stats.statsPerHost.get('ratpack.io').idleConnectionCount == 95
    assert stats.statsPerHost.get('ratpack.io').activeConnectionCount == 5
  }
}
