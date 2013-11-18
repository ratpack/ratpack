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

package ratpack.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import ratpack.handling.Context;
import ratpack.util.Action;
import ratpack.util.Result;
import ratpack.util.ResultAction;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

class ErrorTemplateRenderResultAction implements ResultAction<ByteBuf> {

  private final static Logger LOGGER = Logger.getLogger(ErrorTemplateRenderResultAction.class.getName());

  private final Context context;
  private final Action<? super PrintWriter> errorMsgProducer;

  public ErrorTemplateRenderResultAction(Context context, Action<? super PrintWriter> errorMsgProducer) {
    this.context = context;
    this.errorMsgProducer = errorMsgProducer;
  }

  public void execute(Result<ByteBuf> thing) {
    if (thing.isSuccess()) {
      context.getResponse().send();
    } else {
      StringWriter stringWriter = new StringWriter();
      PrintWriter printWriter = new PrintWriter(stringWriter);

      stringWriter.append("Exception raised during render of error page ");

      errorMsgProducer.execute(printWriter);

      @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
      Exception failure = thing.getFailure();
      failure.printStackTrace(printWriter);

      LOGGER.warning(stringWriter.toString());
      context.getResponse().status(500).send("");
    }
  }
}
