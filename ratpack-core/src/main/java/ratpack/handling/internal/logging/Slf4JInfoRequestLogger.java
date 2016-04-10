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

package ratpack.handling.internal.logging;

import org.slf4j.Logger;
import ratpack.handling.RequestLogger;
import ratpack.handling.RequestOutcome;

public class Slf4JInfoRequestLogger implements RequestLogger {

  private final Logger logger;
  private final Formatter formatter;

  public Slf4JInfoRequestLogger(Logger logger, Formatter formatter) {
    this.logger = logger;
    this.formatter = formatter;
  }

  @Override
  public void log(RequestOutcome outcome) throws Exception {
    if (logger.isInfoEnabled()) {
      logger.info(formatter.format(outcome));
    }
  }

}
