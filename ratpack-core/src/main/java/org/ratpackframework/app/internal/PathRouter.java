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

package org.ratpackframework.app.internal;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.app.Response;
import org.ratpackframework.app.Routing;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathRouter implements Handler<Routed<HttpExchange>> {

  final String method;
  final String path;

  private final TokenisedPath tokenisedPath;
  private final Handler<Response> handler;
  private final ResponseFactory responseFactory;

  public PathRouter(String path, String method, ResponseFactory responseFactory, Handler<Response> handler) {
    this.path = path;
    this.method = method.toUpperCase();
    this.responseFactory = responseFactory;
    this.handler = handler;
    this.tokenisedPath = new TokenisedPath(path);
  }

  private static class TokenisedPath {
    final List<String> names;
    final Pattern regex;

    public TokenisedPath(String path) {
      List<String> names = new LinkedList<>();
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
  public void handle(Routed<HttpExchange> routedHttpExchange) {
    HttpExchange exchange = routedHttpExchange.get();
    HttpRequest request = exchange.getRequest();
    if (!method.equals(Routing.ALL_METHODS) && !request.getMethod().getName().equals(method)) {
      routedHttpExchange.next();
      return;
    }

    Matcher matcher = tokenisedPath.regex.matcher(exchange.getPath());
    if (matcher.matches()) {
      Map<String, String> urlParams = toUrlParams(matcher);
      Response response = responseFactory.create(routedHttpExchange, urlParams);
      handler.handle(response);
    } else {
      routedHttpExchange.next();
    }
  }

  Map<String, String> toUrlParams(Matcher matcher) {
    MatchResult matchResult = matcher.toMatchResult();
    Map<String, String> params = new LinkedHashMap<>();
    int i = 1;
    for (String name : tokenisedPath.names) {
      params.put(name, matchResult.group(i++));
    }
    return params;
  }

}
