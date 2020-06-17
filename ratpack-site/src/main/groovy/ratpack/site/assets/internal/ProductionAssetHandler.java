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
package ratpack.site.assets.internal;


import asset.pipeline.AssetPipelineConfigHolder;
import asset.pipeline.AssetPipelineResponseBuilder;
import asset.pipeline.ratpack.AssetAttributes;
import asset.pipeline.ratpack.internal.AssetProperties;
import asset.pipeline.ratpack.internal.ProductionAssetCache;
import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.core.handling.Context;
import ratpack.core.handling.Handler;
import ratpack.core.http.Response;
import ratpack.core.http.internal.HttpHeaderConstants;
import ratpack.exec.func.Factory;
import ratpack.exec.func.Action;
import ratpack.exec.Blocking;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static ratpack.exec.registry.Registry.single;

public class ProductionAssetHandler implements Handler {
  private static final String ASSET_BASE_PATH = "assets/";

  @Override
  public void handle(Context ctx) throws Exception {
    final Properties manifest = AssetPipelineConfigHolder.manifest;
    final AssetProperties props = ctx.get(AssetProperties.class);
    final ProductionAssetCache fileCache = ctx.get(ProductionAssetCache.class);
    Response response = ctx.getResponse();

    final String manifestPath = manifest.getProperty(props.getPath(), props.getPath());
    final Path asset = ctx.file(ASSET_BASE_PATH + manifestPath);
    final AssetPipelineResponseBuilder responseBuilder = new AssetPipelineResponseBuilder(props.getPath(), ctx.getRequest().getHeaders().get(HttpHeaderNames.IF_NONE_MATCH));

    //Is this in the attribute cache?
    AssetAttributes attributeCache = fileCache.get(manifestPath);
    if (attributeCache != null) {
      if (attributeCache.exists()) {
        response.contentTypeIfNotSet(props.getFormat());
        if (responseBuilder.headers != null) {
          for (Map.Entry<String, String> cursor : responseBuilder.headers.entrySet()) {
            response.getHeaders().set(cursor.getKey(), cursor.getValue());
          }
        }
        if (responseBuilder.statusCode != null) {
          response.status(responseBuilder.statusCode);
        }

        if (responseBuilder.statusCode == null || responseBuilder.statusCode != 304) {
          if (acceptsGzip(ctx) && attributeCache.gzipExists()) {
            Path gzipFile = ctx.file(ASSET_BASE_PATH + manifestPath + ".gz");
            response.getHeaders().set("Content-Encoding", "gzip");
            response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributeCache.getGzipFileSize()));
            response.sendFile(gzipFile);
          } else {
            response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributeCache.getFileSize()));
            response.noCompress().sendFile(asset);
          }
        } else {
          response.send();
        }
      } else if (attributeCache.isDirectory()) {
        doIndexFileNext(ctx, props);
      } else {
        ctx.next();
      }
    } else {
      readAttributes(asset, attributes -> {
        if (attributes == null || !attributes.isRegularFile()) {

          if (props.getIndexedPath() != null && attributes != null) {
            fileCache.put(manifestPath, new AssetAttributes(false, false, true, null, null));
            doIndexFileNext(ctx, props);
          } else {
            fileCache.put(manifestPath, new AssetAttributes(false, false, false, null, null));
            ctx.next();
          }
        } else {
          response.contentTypeIfNotSet(props.getFormat());
          if (responseBuilder.headers != null) {
            for (Map.Entry<String, String> cursor : responseBuilder.headers.entrySet()) {
              response.getHeaders().set(cursor.getKey(), cursor.getValue());
            }
          }

          if (responseBuilder.statusCode != null) {
            response.status(responseBuilder.statusCode);
          }

          if (responseBuilder.statusCode == null || responseBuilder.statusCode != 304) {
            Path gzipFile = ctx.file(ASSET_BASE_PATH + manifestPath + ".gz");
            if (acceptsGzip(ctx)) {
              readAttributes(gzipFile, gzipAttributes -> {
                if (gzipAttributes == null || !gzipAttributes.isRegularFile()) {
                  response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributes.size()));
                  fileCache.put(manifestPath, new AssetAttributes(true, false, false, attributes.size(), null));
                  response.noCompress().sendFile(asset);
                } else {
                  response.getHeaders().set("Content-Encoding", "gzip");
                  response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(gzipAttributes.size()));
                  fileCache.put(manifestPath, new AssetAttributes(true, true, false, attributes.size(), gzipAttributes.size()));
                  response.sendFile(gzipFile);
                }
              });
            } else {
              response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributes.size()));
              response.noCompress().sendFile(asset);
              readAttributes(gzipFile, gzipAttributes -> {
                if (gzipAttributes == null || !gzipAttributes.isRegularFile()) {
                  fileCache.put(manifestPath, new AssetAttributes(true, false, false, attributes.size(), null));
                } else {
                  fileCache.put(manifestPath, new AssetAttributes(true, true, false, attributes.size(), gzipAttributes.size()));
                }
              });
            }
          } else {
            response.send();
          }
        }
      });
    }
  }

  private void doIndexFileNext(Context ctx, AssetProperties props) {
    ctx.insert(single(new AssetProperties(props.getIndexedPath(), null, props.getFormat(), props.getEncoding())), this);
  }

  private boolean acceptsGzip(Context ctx) {
    String acceptsEncoding = ctx.getRequest().getHeaders().get(HttpHeaderNames.ACCEPT_ENCODING);
    return acceptsEncoding != null && Arrays.asList(acceptsEncoding.split(",")).contains("gzip");
  }

  public static void readAttributes(Path file, Action<? super BasicFileAttributes> then) throws Exception {
    Blocking.get(getter(file)).then(then);
  }

  private static Factory<BasicFileAttributes> getter(Path file) {
    return () -> {
      if (Files.exists(file)) {
        return Files.readAttributes(file, BasicFileAttributes.class);
      } else {
        return null;
      }
    };
  }
}
