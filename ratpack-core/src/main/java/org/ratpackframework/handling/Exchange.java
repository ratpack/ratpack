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

package org.ratpackframework.handling;

import org.ratpackframework.context.Context;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;

import java.io.File;
import java.util.Map;

public interface Exchange {

  Request getRequest();

  Response getResponse();

  Context getContext();

  <T> T get(Class<T> type);

  <T> T maybeGet(Class<T> type);

  void next();

  void next(Handler... handlers);

  void next(Iterable<Handler> handlers);

  void nextWithContext(Object object, Handler... handlers);

  void nextWithContext(Object object, Iterable<Handler> handlers);

  void nextWithContext(Context context, Handler... handlers);

  void nextWithContext(Context context, Iterable<Handler> handlers);

  Map<String, String> getPathTokens();

  Map<String, String> getAllPathTokens();

  File file(String path);

  void error(Exception exception);

  void clientError(int statusCode);

  void withErrorHandling(Runnable runnable);

  ByMethodChain getMethods();

}
