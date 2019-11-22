package ratpack.micrometer.metrics.config

import io.micrometer.influx.InfluxConsistency
import spock.lang.Specification

class MappedInfluxConfigSpec extends Specification {
  def 'map consistency from string'() {
    given:
    def ratpackConfig = new RatpackInfluxConfig().consistency(input)

    expect:
    new MappedInfluxConfig(ratpackConfig).consistency() == output

    where:
    input | output
    'any' | InfluxConsistency.ANY
    'AnY' | InfluxConsistency.ANY
    'dne' | InfluxConsistency.ONE // fallback
    null  | InfluxConsistency.ONE
  }
}
