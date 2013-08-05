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

package org.ratpackframework.handling.internal;

import io.netty.channel.ChannelHandlerContext;
import org.ratpackframework.error.ClientErrorHandler;
import org.ratpackframework.error.ServerErrorHandler;
import org.ratpackframework.file.FileSystemBinding;
import org.ratpackframework.handling.ByAcceptsResponder;
import org.ratpackframework.handling.ByMethodResponder;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.registry.NotInRegistryException;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.ObjectHoldingChildRegistry;
import org.ratpackframework.render.NoSuchRendererException;
import org.ratpackframework.render.RenderController;
import org.ratpackframework.url.PublicAddress;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DefaultContext implements Context {

  private final Request request;
  private final Response response;

  private final ChannelHandlerContext channelHandlerContext;

  private final Handler next;
  private final Registry<Object> registry;

  public DefaultContext(Request request, Response response, ChannelHandlerContext channelHandlerContext, Registry<Object> registry, Handler next) {
    this.request = request;
    this.response = response;
    this.channelHandlerContext = channelHandlerContext;
    this.registry = registry;
    this.next = next;
  }

  public Request getRequest() {
    return request;
  }

  public Response getResponse() {
    return response;
  }

  public <O> O get(Class<O> type) throws NotInRegistryException {
    return registry.get(type);
  }

  public <O> O maybeGet(Class<O> type) {
    return registry.maybeGet(type);
  }

  public void next() {
    next.handle(this);
  }

  public void insert(List<Handler> handlers) {
    doNext(this, registry, handlers, 0, next);
  }

  public void insert(Registry<Object> registry, List<Handler> handlers) {
    doNext(this, registry, handlers, 0, next);
  }

  public <P, T extends P> void insert(Class<P> publicType, T implementation, List<Handler> handlers) {
    doNext(this, new ObjectHoldingChildRegistry<Object>(registry, publicType, implementation), handlers, 0, next);
  }

  public void insert(Object object, List<Handler> handlers) {
    doNext(this, new ObjectHoldingChildRegistry<Object>(registry, object), handlers, 0, next);
  }

  public Map<String, String> getPathTokens() {
    return get(PathBinding.class).getTokens();
  }

  public Map<String, String> getAllPathTokens() {
    return get(PathBinding.class).getAllTokens();
  }

  public File file(String path) {
    return get(FileSystemBinding.class).file(path);
  }


  public void render(Object object) throws NoSuchRendererException {
    get(RenderController.class).render(this, object);
  }

  public void redirect(String location) {
    response.redirect(generateRedirectLocation(location));
  }

  public void redirect(int code, String location) {
    response.redirect(code, generateRedirectLocation(location));
  }

  public void error(Exception exception) {
    ServerErrorHandler serverErrorHandler = get(ServerErrorHandler.class);
    serverErrorHandler.error(this, exception);
  }

  public void clientError(int statusCode) {
    get(ClientErrorHandler.class).error(this, statusCode);
  }

  public void withErrorHandling(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception e) {
      if (e instanceof HandlerException) {
        ((HandlerException) e).getContext().error((Exception) e.getCause());
      } else {
        error(e);
      }
    }
  }

  public ByMethodResponder getMethods() {
    return new DefaultByMethodResponder(this);
  }

  public ByAcceptsResponder getAccepts() {
    return new DefaultByAcceptsResponder(this);
  }

  protected void doNext(final Context parentContext, final Registry<Object> registry, final List<Handler> handlers, final int index, final Handler exhausted) {
    assert registry != null;
    if (index == handlers.size()) {
      try {
        exhausted.handle(parentContext);
      } catch (Exception e) {
        if (e instanceof HandlerException) {
          throw (HandlerException) e;
        } else {
          throw new HandlerException(this, e);
        }
      }

    } else {
      Handler handler = handlers.get(index);
      Handler nextHandler = new Handler() {
        public void handle(Context exchange) {
          ((DefaultContext) exchange).doNext(parentContext, registry, handlers, index + 1, exhausted);
        }
      };
      DefaultContext childExchange = new DefaultContext(request, response, channelHandlerContext, registry, nextHandler);
      try {
        handler.handle(childExchange);
      } catch (Exception e) {
        if (e instanceof HandlerException) {
          throw (HandlerException) e;
        } else {
          throw new HandlerException(childExchange, e);
        }
      }
    }
  }


  private String generateRedirectLocation(String path) {
    //Rules
    //1. Given absolute URL use it
    //2. Given Starting Slash prepend public facing domain:port if provided if not use base URL of request
    //3. Given relative URL prepend public facing domain:port plus parent path of request URL otherwise full parent path

    String generatedPath = null;

    Pattern pattern = Pattern.compile("^https?://.*");

    if (pattern.matcher(path).matches()) {
      //Rule 1 - Path is absolute
      generatedPath = path;
    } else {
      if (path.charAt(0) == '/') {
        //Rule 2 - Starting Slash
        generatedPath = getHost() + path;
      } else {
        //Rule 3
        generatedPath = getHost() + getParentPath(request.getUri()) + path;
      }
    }

    return generatedPath;
  }


  /**
   * Using any specified public url first and then falling back to the Host header. If there is no host header we will return an empty string.
   *
   * @return The host if it can be found
   */
  protected String getHost() {
    String host = "";
    if (getPublicHost() != null) {
      host = getPublicHost().toString();
    } else {
      if (request != null) {
        if (request.getHeader("Host") != null) {
          //TODO find if there is a way not to assume http
          host = "http://" + request.getHeader("Host");
        }
      }
    }

    return host;
  }

  private URL getPublicHost() {

    URL publicAdd = null;
    PublicAddress publicAddress = registry.maybeGet(PublicAddress.class);
    if (publicAddress != null) {
      publicAdd = publicAddress.getUrl();
    }

    return publicAdd;
  }

  private String getParentPath(String path) {
    String parentPath = "/";

    int indexOfSlash = path.lastIndexOf('/');
    if (indexOfSlash >= 0) {
      parentPath = path.substring(0, indexOfSlash) + '/';
    }

    if (!parentPath.startsWith("/")) {
      parentPath = "/" + parentPath;
    }
    return parentPath;
  }
}
