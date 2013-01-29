package org.ratpackframework.routing.internal;

import org.ratpackframework.Request;
import org.ratpackframework.internal.DefaultRequest;
import org.ratpackframework.responder.Responder;
import org.ratpackframework.responder.internal.ResponderFactory;
import org.ratpackframework.routing.Router;
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
      Request wrappedRequest = new DefaultRequest(request, routedRequest.getErrorHandler(), toUrlParams(matcher));
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
