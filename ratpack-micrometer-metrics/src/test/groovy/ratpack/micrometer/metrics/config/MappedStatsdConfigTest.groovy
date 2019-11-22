package ratpack.micrometer.metrics.config


import io.micrometer.statsd.StatsdFlavor
import spock.lang.Specification

class MappedStatsdConfigTest extends Specification {
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
