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

package org.ratpackframework.routing.internal;

import org.ratpackframework.Request;
import org.ratpackframework.internal.DefaultRequest;
import org.ratpackframework.responder.Responder;
import org.ratpackframework.responder.internal.ResponderFactory;
import org.ratpackframework.routing.Router;
import org.ratpackframework.session.internal.RequestSessionManager;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathRouter implements Router {

  final String method;
  final String path;

  private final TokenisedPath tokenisedPath;
  private final ResponderFactory responderFactory;

  public PathRouter(String path, String method, ResponderFactory responderFactory) {
    this.path = path;
    this.method = method.toLowerCase();
    this.responderFactory = responderFactory;
    this.tokenisedPath = new TokenisedPath(path);
  }

  private static class TokenisedPath {
    final List<String> names;
    final Pattern regex;

    public TokenisedPath(String path) {
      List<String> names = new LinkedList<String>();
      String regexString = path;

      Pattern placeholderPattern = Pattern.compile("(:\\w+)");
      Matcher matchResult = placeholderPattern.matcher(path);
      while (matchResult.find()) {
        String name = matchResult.group();
        regexString = regexString.replaceFirst(name, "([^/?&#]+)");
        names.add(name.substring(1));
      }

      this.regex = Pattern.compile(regexString);
      this.names = Collections.unmodifiableList(names);
    }
  }

  @Override
  public void handle(RoutedRequest routedRequest) {
    final HttpServerRequest request = routedRequest.getRequest();
    if (!request.method.toLowerCase().equals(method)) {
      routedRequest.getNotFoundHandler().handle(request);
      return;
    }

    Matcher matcher = tokenisedPath.regex.matcher(request.path);
    if (matcher.matches()) {
      RequestSessionManager requestSessionManager = routedRequest.getSessionManager().getRequestSessionManager(request);
      Request wrappedRequest = new DefaultRequest(request, routedRequest.getErrorHandler(), toUrlParams(matcher), requestSessionManager);
      Responder responder = responderFactory.createResponder(wrappedRequest);
      responder.respond(routedRequest.getFinalizedResponseHandler());
      request.resume();
    } else {
      routedRequest.getNotFoundHandler().handle(request);
    }
  }

  Map<String, String> toUrlParams(Matcher matcher) {
    MatchResult matchResult = matcher.toMatchResult();
    Map<String, String> params = new LinkedHashMap<String, String>();
    int i = 1;
    for (String name : tokenisedPath.names) {
      params.put(name, matchResult.group(i++));
    }
    return params;
  }

}
