/*
 * Copyright 2013 the original author or authors.
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

package ratpack.error.internal;

import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServerErrorHandler implements ServerErrorHandler {

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultServerErrorHandler.class);

  public void error(Context context, Throwable throwable) throws Throwable {
    LOGGER.error("UNHANDLED THROWABLE: " + context.getRequest().getUri(), throwable);
    context.getResponse().status(500).send();
  }

}
