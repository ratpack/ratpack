package org.ratpackframework.file.internal;

import org.jboss.netty.util.CharsetUtil;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.context.Context;
import org.ratpackframework.http.Request;
import org.ratpackframework.file.FileSystemContext;
import org.ratpackframework.path.PathContext;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class TargetFileStaticAssetRequestHandler implements Handler {

  private final Handler delegate;

  public TargetFileStaticAssetRequestHandler(Handler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    Context context = exchange.getContext();
    FileSystemContext fileSystemContext = context.require(FileSystemContext.class);

    Request request = exchange.getRequest();

    String path = request.getPath();
    PathContext pathContext = context.get(PathContext.class);
    if (pathContext != null) {
      path = pathContext.getPastBinding();
    }

    // Decode the pathRoutes.
    try {
      path = URLDecoder.decode(path, CharsetUtil.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      try {
        path = URLDecoder.decode(path, CharsetUtil.ISO_8859_1.name());
      } catch (UnsupportedEncodingException e1) {
        throw new RuntimeException(e1);
      }
    }

    // Convert file separators.
    path = path.replace('/', File.separatorChar);

    if (path.contains(File.separator + '.') ||
        path.contains('.' + File.separator) ||
        path.startsWith(".") || path.endsWith(".")) {
      return;
    }

    FileSystemContext childContext = fileSystemContext.context(path);
    exchange.nextWithContext(childContext, delegate);
  }
}
