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

package ratpack.error;

import ratpack.handling.Context;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * A simple server and client error handler that prints out information in plain text to the response.
 * <p>
 * <b>This is not suitable for use in production</b> as it exposes internal information about your application via stack traces.
 */
public class DebugErrorHandler implements ServerErrorHandler, ClientErrorHandler {

  /**
   * {@link Exception#printStackTrace() Prints the stacktrace} of the given exception to the response with a 500 status.
   *
   * @param context The context being processed
   * @param exception The exception that occurred
   */
  @Override
  public void error(Context context, Exception exception) {
    Writer writer = new StringWriter();
    exception.printStackTrace(new PrintWriter(writer));
    context.getResponse().status(500).send(writer.toString());
  }

  /**
   * Prints the string "Client error «statusCode»" to the response as text with the given status code.
   *
   * @param context The context
   * @param statusCode The 4xx status code that explains the problem
   */
  @Override
  public void error(Context context, int statusCode) {
    context.getResponse().status(statusCode).send(String.format("Client error %s", statusCode));
  }

}
