/*
 * Copyright 2014 the original author or authors.
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

package ratpack.util.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public abstract class NumberUtil {

  public static BigDecimal convertNanoDiff(long startNanos, long stopNanos, TemporalUnit unit) {
    BigDecimal diffNanos = new BigDecimal(stopNanos - startNanos);
    return diffNanos.divide(BigDecimal.valueOf(unit.getDuration().toNanos()));
  }

  public static String toMillisDiffString(long startNanos, long stopNanos) {
    return convertNanoDiff(startNanos, stopNanos, ChronoUnit.MILLIS).setScale(5, RoundingMode.UP).toString();
  }

}
