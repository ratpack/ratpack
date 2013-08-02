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

public class DefaultServerErrorHandler implements ServerErrorHandler {

  public void error(Context context, Exception exception) {
    System.err.println("UNHANDLED EXCEPTION: " + context.getRequest().getUri());
    exception.printStackTrace(System.err);
    context.getResponse().status(500).send();
  }

}
