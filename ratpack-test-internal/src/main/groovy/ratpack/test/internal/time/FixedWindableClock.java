package ratpack.test.internal.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;

/**
 * Similar to a java.time.Clock.FixedClock, but allows you to wind time forwards and backwards.
 */
public final class FixedWindableClock extends Clock {

  private Instant now;
  private final ZoneId zoneId;

  public FixedWindableClock(Instant now, ZoneId zoneId) {
    this.now = now;
    this.zoneId = zoneId;
  }

  @Override
  public ZoneId getZone() {
    return zoneId;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new FixedWindableClock(now, zone);
  }

  @Override
  public Instant instant() {
    return now;
  }

  public void windClock(TemporalAmount amount) {
    now = now.plus(amount);
  }

}
