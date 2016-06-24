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

class PooledHttpConfigSpec extends Specification {

  void 'test config'() {
    given:
    int connectTimeout = 2000
    int maxConnections = 1000
    int readTimeout = 1000
    boolean decompress = false
    boolean pooled = true

    when:
    PooledHttpConfig config = new PooledHttpConfig(connectionTimeoutNanos: connectTimeout, maxConnections: maxConnections, readTimeoutNanos: readTimeout, decompressResponse: decompress, pooled: pooled)

    then:
    config.connectionTimeoutNanos == connectTimeout
    config.maxConnections == maxConnections
    config.readTimeoutNanos == readTimeout
    config.decompressResponse == decompress
    config.pooled == pooled
  }
}
