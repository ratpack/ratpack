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

package org.ratpackframework.error.internal;

import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.handling.Context;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class PrintingServerErrorHandler implements ServerErrorHandler {

  public void error(Context context, Exception exception) {
    Writer writer = new StringWriter();
    try {
      writer.write(exception.toString());
      exception.printStackTrace(new PrintWriter(writer));
    } catch (IOException e) {
      throw new RuntimeException(e); // impossible
    }

    context.getResponse().status(500).send(writer.toString());
  }
}
