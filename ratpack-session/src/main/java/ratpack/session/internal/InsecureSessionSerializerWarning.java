/*
 * Copyright 2021 the original author or authors.
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

package ratpack.session.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.session.SessionSerializer;

public class InsecureSessionSerializerWarning {

  public static final Logger LOGGER = LoggerFactory.getLogger(InsecureSessionSerializerWarning.class);

  private InsecureSessionSerializerWarning() {
  }

  public static void log(Class<? extends SessionSerializer> implType) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(
        implType.getName() + " does not implement type filtering, which makes your application vulnerable if session data can be provided by untrusted sources (see https://portswigger.net/web-security/deserialization)."
          + " Please contact the author of " + implType.getName() + " and urge them to update it to support type filtering."
      );
    }
  }
}
