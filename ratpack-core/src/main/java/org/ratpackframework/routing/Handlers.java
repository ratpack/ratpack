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

package org.ratpackframework.routing;

import org.ratpackframework.file.internal.DirectoryStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileSystemContextHandler;
import org.ratpackframework.file.internal.TargetFileStaticAssetRequestHandler;
import org.ratpackframework.http.internal.MethodHandler;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.path.internal.PathHandler;
import org.ratpackframework.path.internal.TokenPathBinding;
import org.ratpackframework.routing.internal.RoutingHandler;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public abstract class Handlers {

  public static Handler context(final Object context, final RoutingBuilder<? super Routing> builder) {
    return context(context, routes(builder));
  }

  public static Handler context(final Object context, final Handler handler) {
    return new Handler() {
      public void handle(Exchange exchange) {
        exchange.nextWithContext(context, handler);
      }
    };
  }

  public static Handler routes(RoutingBuilder<? super Routing> builder) {
    return new RoutingHandler(builder);
  }

  public static Handler fsContext(String path, Handler handler) {
    return new FileSystemContextHandler(new File(path), handler);
  }

  public static Handler fsContext(String path, RoutingBuilder<? super Routing> builder) {
    return fsContext(path, routes(builder));
  }

  public static Handler assets(String path, Handler notFound) {
    return assets(path, new String[0], notFound);
  }

  public static Handler assets(String path, String... indexFiles) {
    return assets(path, indexFiles, noop());
  }

  public static Handler assets(String path, String[] indexFiles, final Handler notFound) {
    Handler fileHandler = new FileStaticAssetRequestHandler();
    Handler directoryHandler = new DirectoryStaticAssetRequestHandler(Arrays.asList(indexFiles), fileHandler);
    Handler contextSetter = new TargetFileStaticAssetRequestHandler(directoryHandler);

    return fsContext(path, chain(contextSetter, notFound));
  }

  public static Handler assetsPath(String uriPath, String fsPath, Handler notFound) {
    return path(uriPath, assets(fsPath, notFound));
  }

  public static Handler assetsPath(String uriPath, String fsPath, String... indexFiles) {
    return path(uriPath, assets(fsPath, indexFiles));

  }

  public static Handler assetsPath(String uriPath, String fsPath, String[] indexFiles, final Handler notFound) {
    return path(uriPath, assets(fsPath, indexFiles, notFound));
  }

  public static Handler chain(final Handler... handlers) {
    return new Handler() {
      public void handle(Exchange exchange) {
        exchange.next(handlers);
      }
    };
  }

  public static Handler noop() {
    return new Handler() {
      public void handle(Exchange exchange) {
        exchange.next();
      }
    };
  }

  public static Handler path(String path, RoutingBuilder<? super Routing> builder) {
    return path(path, routes(builder));
  }

  public static Handler path(String path, Handler handler) {
    return pathBinding(new TokenPathBinding(path, false), handler);
  }

  public static Handler exactPath(String path, RoutingBuilder<? super Routing> builder) {
    return exactPath(path, routes(builder));
  }

  public static Handler exactPath(String path, Handler handler) {
    return pathBinding(new TokenPathBinding(path, true), handler);
  }

  public static Handler pathBinding(PathBinding pathBinding, Handler handler) {
    return new PathHandler(pathBinding, handler);
  }

  public static Handler method(Collection<String> methods, RoutingBuilder<? super Routing> builder) {
    return method(methods, routes(builder));
  }

  public static Handler method(Collection<String> methods, Handler handler) {
    return new MethodHandler(methods, handler);
  }
}
