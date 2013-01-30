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

package org.ratpackframework.internal;

import groovy.lang.Closure;
import org.ratpackframework.groovy.Request;
import org.ratpackframework.handler.ClosureHandlerAdapter;
import org.ratpackframework.handler.ErrorHandler;
import org.ratpackframework.handler.ErroredHttpServerRequest;
import org.ratpackframework.session.Session;
import org.ratpackframework.session.internal.RequestSessionManager;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

public class DefaultRequest implements Request {

  private final RequestSessionManager sessionManager;
  private ContentType contentType;

  private final HttpServerRequest vertxRequest;
  private final ErrorHandler errorHandler;

  private final Map<String, String> urlParams;
  private Map<String, ?> queryParams;

  public DefaultRequest(final HttpServerRequest vertxRequest, final ErrorHandler errorHandler, Map<String, String> urlParams, RequestSessionManager sessionManager) {
    this.vertxRequest = vertxRequest;
    this.errorHandler = errorHandler;
    this.urlParams = urlParams;
    this.sessionManager = sessionManager;

    vertxRequest.exceptionHandler(new Handler<Exception>() {
      @Override
      public void handle(Exception exception) {
        errorHandler.handle(new ErroredHttpServerRequest(vertxRequest, exception));
      }
    });
  }

  @Override
  public Map<String, ?> getQueryParams() {
    if (queryParams == null) {
      try {
        queryParams = new ParamParser().parse(URLDecoder.decode(getQuery(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return queryParams;
  }

  @Override
  public Map<String, String> getUrlParams() {
    return urlParams;
  }

  protected <T> Handler<T> errorHandler(Handler<T> handler) {
    return errorHandler.handler(vertxRequest, handler);
  }

  @Override
  public void buffer(Handler<Buffer> bufferHandler) {
    vertxRequest.bodyHandler(errorHandler(bufferHandler));
  }

  @Override
  public void text(final Handler<String> textHandler) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        textHandler.handle(event.toString(getContentType().getCharset()));
      }
    }));
  }

  @Override
  public void json(final Handler<JsonObject> jsonHandler) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        String charset = getContentType().getCharset();
        jsonHandler.handle(new JsonObject(event.toString(charset)));
      }
    }));
  }

  @Override
  public void form(final Handler<Map<String, ?>> formHandler) {
    vertxRequest.bodyHandler(errorHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        String charset = getContentType().getCharset();
        try {
          String string = event.toString(charset);
          String decoded = URLDecoder.decode(string, charset);
          Map<String, ?> map = new ParamParser().parse(decoded);
          formHandler.handle(map);
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    }));
  }

  @Override
  public void buffer(Closure<?> receiver) {
    buffer(new ClosureHandlerAdapter<Buffer>(receiver));
  }

  @Override
  public void text(final Closure<?> textReceiver) {
    buffer(new ClosureHandlerAdapter<Buffer>(textReceiver));
  }

  @Override
  public void json(final Closure<?> jsonReceiver) {
    buffer(new ClosureHandlerAdapter<Buffer>(jsonReceiver));
  }

  @Override
  public void form(final Closure<?> formReceiver) {
    buffer(new ClosureHandlerAdapter<Buffer>(formReceiver));
  }

  @Override
  public HttpServerRequest getVertxRequest() {
    return vertxRequest;
  }

  private ContentType getContentType() {
    if (contentType == null) {
      contentType = new ContentType(vertxRequest.headers().get("content-type"));
    }
    return contentType;
  }

  @Override
  public String getMethod() {
    return vertxRequest.method;
  }

  @Override
  public String getUri() {
    return vertxRequest.uri;
  }

  @Override
  public String getQuery() {
    return vertxRequest.query;
  }

  @Override
  public String getPath() {
    return vertxRequest.path;
  }

  @Override
  public Session getSession() {
    return sessionManager.getSession();
  }

}
