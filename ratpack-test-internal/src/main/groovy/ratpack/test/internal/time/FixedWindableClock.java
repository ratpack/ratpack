/*
 * Copyright 2018 the original author or authors.
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
