package org.ratpackframework.http;

import org.ratpackframework.Action;
import org.ratpackframework.file.internal.DirectoryStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileStaticAssetRequestHandler;
import org.ratpackframework.file.internal.FileSystemContextHandler;
import org.ratpackframework.file.internal.TargetFileStaticAssetRequestHandler;
import org.ratpackframework.http.internal.MethodHandler;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.path.internal.PathHandler;
import org.ratpackframework.path.internal.TokenPathBinding;
import org.ratpackframework.routing.Routing;
import org.ratpackframework.routing.RoutingHandler;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public abstract class Handlers {

  public static Handler context(final Object context, final Action<? super Routing> action) {
    return new Handler() {
      @Override
      public void handle(Exchange exchange) {
        exchange.nextWithContext(context, routes(action));
      }
    };
  }

  public static Handler routes(Action<? super Routing> action) {
    return new RoutingHandler(action);
  }

  public static Handler fsContext(String path, Handler handler) {
    return new FileSystemContextHandler(new File(path), handler);
  }

  public static Handler fsContext(String path, Action<? super Routing> action) {
    return fsContext(path, routes(action));
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

  public static Handler path(String path, Action<? super Routing> action) {
    return path(path, routes(action));
  }

  public static Handler path(String path, Handler handler) {
    return pathBinding(new TokenPathBinding(path, false), handler);
  }

  public static Handler exactPath(String path, Action<? super Routing> action) {
    return exactPath(path, routes(action));
  }

  public static Handler exactPath(String path, Handler handler) {
    return pathBinding(new TokenPathBinding(path, true), handler);
  }

  public static Handler pathBinding(PathBinding pathBinding, Handler handler) {
    return new PathHandler(pathBinding, handler);
  }

  public static Handler method(Collection<String> methods, Action<Routing> routingAction) {
    return method(methods, routes(routingAction));
  }

  public static Handler method(Collection<String> methods, Handler handler) {
    return new MethodHandler(methods, handler);
  }
}
