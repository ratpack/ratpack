package org.ratpackframework.assets.internal;

import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.util.CharsetUtil;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class TargetFileStaticAssetRequestHandler implements Handler<Routed<HttpExchange>> {

  private final File assetsDirectory;

  @Inject
  public TargetFileStaticAssetRequestHandler(@Named("staticAssetDir") File assetsDirectory) {
    this.assetsDirectory = assetsDirectory;
  }

  @Override
  public void handle(Routed<HttpExchange> routed) {
    HttpExchange httpExchange = routed.get();
    HttpRequest request = httpExchange.getRequest();
    String path = httpExchange.getPath();

    // Decode the path.
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

    File file = new File(assetsDirectory, path);

    try {
      httpExchange.setTargetFile(file.getCanonicalFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    routed.next();
  }
}
