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

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;
import ratpack.http.Request;

/**
 * A simple server and client error handler that prints out information in plain text to the response.
 * <p>
 * <b>This is not suitable for use in production</b> as it exposes internal information about your application via stack traces.
 */
public class DefaultDevelopmentErrorHandler implements ErrorHandler {

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultDevelopmentErrorHandler.class);

  /**
   * {@link Exception#printStackTrace() Prints the stacktrace} of the given exception to the response with a 500 status.
   *
   *  @param context The context being processed
   * @param throwable The exception that occurred
   */
  @Override
  public void error(Context context, Throwable throwable) {
    LOGGER.error("exception thrown for request to " + context.getRequest().getRawUri(), throwable);
    context.getResponse().status(500);

    new ErrorPageRenderer() {
      @Override
      protected void render() {
        render(context, "Internal Error", w -> {
          messages(w, "Internal Error", () -> {
            Request request = context.getRequest();
            meta(w, m -> m
                .put("URI:", request.getRawUri())
                .put("Method:", request.getMethod().getName())
            );
          });
          stack(w, null, throwable);
        });

      }
    };
  }

  /**
   * Prints the string "Client error «statusCode»" to the response as text with the given status code.
   *
   * @param context The context
   * @param statusCode The 4xx status code that explains the problem
   */
  @Override
  public void error(Context context, int statusCode) {
    HttpResponseStatus status = HttpResponseStatus.valueOf(statusCode);
    Request request = context.getRequest();
    LOGGER.error(statusCode + " client error for request to " + request.getRawUri());
    context.getResponse().status(statusCode);

    new ErrorPageRenderer() {
      protected void render() {
        render(context, status.reasonPhrase(), w ->
            messages(w, "Client Error", () ->
                meta(w, m -> m
                    .put("URI:", request.getRawUri())
                    .put("Method:", request.getMethod().getName())
                    .put("Status Code:", status.code())
                    .put("Phrase:", status.reasonPhrase())
                )
            )
        );
      }
    };
  }


}