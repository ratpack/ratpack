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

package ratpack.error.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;

public class DefaultProductionErrorHandler implements ErrorHandler {

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultProductionErrorHandler.class);

  @Override
  public void error(Context context, int statusCode) throws Exception {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(getMsg(ClientErrorHandler.class, "client error", context));
    }
    context.getResponse().status(statusCode).send();
  }

  @Override
  public void error(Context context, Throwable throwable) throws Exception {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn(getMsg(ServerErrorHandler.class, "server error", context) + "\n", throwable);
    }
    context.getResponse().status(500).send();
  }

  private String getMsg(Class<?> handlerClass, String type, Context context) {
    return "Default production error handler used to render " + type + ", please add a " + handlerClass.getName() + " instance to your application "
      + "(method: " + context.getRequest().getMethod() + ", uri: " + context.getRequest().getRawUri() + ")";
  }

}
