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

package ratpack.micrometer.metrics.config


import io.micrometer.statsd.StatsdFlavor
import spock.lang.Specification

class MappedStatsdConfigSpec extends Specification {
  def 'map flavor from string'() {
    given:
    def ratpackConfig = new RatpackStatsdConfig().flavor(input)

    expect:
    new MappedStatsdConfig(ratpackConfig).flavor() == output

    where:
    input  | output
    'etsy' | StatsdFlavor.ETSY
    'EtSy' | StatsdFlavor.ETSY
    'dne'  | StatsdFlavor.DATADOG // fallback
    null   | StatsdFlavor.DATADOG
  }
}
