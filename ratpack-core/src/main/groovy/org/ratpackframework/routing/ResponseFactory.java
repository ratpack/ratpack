/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.routing;

import org.ratpackframework.Request;
import org.ratpackframework.Response;
import org.ratpackframework.internal.DefaultRequest;
import org.ratpackframework.internal.DefaultResponse;
import org.ratpackframework.session.internal.RequestSessionManager;
import org.ratpackframework.session.SessionConfig;
import org.ratpackframework.templating.TemplateRenderer;

import java.util.Map;

public class ResponseFactory {

  private final TemplateRenderer templateRenderer;
  private final SessionConfig sessionConfig;

  public ResponseFactory(TemplateRenderer templateRenderer, SessionConfig sessionConfig) {
    this.templateRenderer = templateRenderer;
    this.sessionConfig = sessionConfig;
  }

  public Response create(RoutedRequest routedRequest, Map<String, String> urlParams) {
    RequestSessionManager requestSessionManager = new RequestSessionManager(routedRequest.getRequest(), sessionConfig);
    Request request = new DefaultRequest(routedRequest.getRequest(), routedRequest.getErrorHandler(), urlParams, requestSessionManager);
    return new DefaultResponse(request, templateRenderer, routedRequest.getFinalizedResponseHandler());
  }

}
