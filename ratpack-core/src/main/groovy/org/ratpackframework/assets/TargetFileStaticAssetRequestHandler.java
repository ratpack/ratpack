package org.ratpackframework.assets;

import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.util.CharsetUtil;
import org.ratpackframework.Handler;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class TargetFileStaticAssetRequestHandler implements Handler<RoutedStaticAssetRequest> {

  private final File assetsDirectory;

  @Inject
  public TargetFileStaticAssetRequestHandler(File assetsDirectory) {
    this.assetsDirectory = assetsDirectory;
  }

  @Override
  public void handle(RoutedStaticAssetRequest assetRequest) {
    HttpRequest request = assetRequest.getExchange().getRequest();
    String uri = request.getUri();

    // Decode the path.
    try {
      uri = URLDecoder.decode(uri, CharsetUtil.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      try {
        uri = URLDecoder.decode(uri, CharsetUtil.ISO_8859_1.name());
      } catch (UnsupportedEncodingException e1) {
        throw new RuntimeException(e1);
      }
    }

    // Convert file separators.
    uri = uri.replace('/', File.separatorChar);

    if (uri.contains(File.separator + '.') ||
        uri.contains('.' + File.separator) ||
        uri.startsWith(".") || uri.endsWith(".")) {
      return;
    }

    File file = new File(assetsDirectory, uri);

    try {
      assetRequest.setTargetFile(file.getCanonicalFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    assetRequest.next();
  }
}
