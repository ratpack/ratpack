package org.ratpackframework.routing.internal

import groovy.transform.CompileStatic
import org.ratpackframework.internal.DefaultRequest
import org.ratpackframework.responder.internal.ResponderFactory
import org.ratpackframework.routing.Router

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
class PathRouter implements Router {

  final String method
  final String path

  private final TokenisedPath tokenisedPath
  private final ResponderFactory responderFactory

  PathRouter(String path, String method, ResponderFactory responderFactory) {
    this.path = path
    this.method = method.toLowerCase()
    this.responderFactory = responderFactory
    this.tokenisedPath = new TokenisedPath(path)
  }

  private static class TokenisedPath {
    final List<String> names
    final Pattern regex

    TokenisedPath(String path) {
      List<String> names = new LinkedList<>()
      String regexString = path

      def placeholderPattern = Pattern.compile("(:\\w+)")
      placeholderPattern.matcher(path).each { List<String> match ->
        def name = match[1][1..-1]
        regexString = regexString.replaceFirst(match[0], "([^/?&#]+)")
        names << name
      }

      this.regex = Pattern.compile(regexString)
      this.names = names.asImmutable()
    }
  }

  @Override
  void handle(RoutedRequest routedRequest) {
    final request = routedRequest.request
    if (request.method.toLowerCase() != method) {
      routedRequest.notFoundHandler.handle(request)
      return
    }

    def matcher = tokenisedPath.regex.matcher(request.path)
    if (matcher.matches()) {
      responderFactory.createResponder(new DefaultRequest(request, routedRequest.errorHandler, toUrlParams(matcher))).respond(routedRequest.finalizedResponseHandler)
    } else {
      routedRequest.notFoundHandler.handle(request)
    }
  }

  Map<String, String> toUrlParams(Matcher matcher) {
    def params = [:]
    tokenisedPath.names.eachWithIndex { String it, Integer i ->
      params[it] = matcher.group(i + 1)
    }
    params
  }

}
