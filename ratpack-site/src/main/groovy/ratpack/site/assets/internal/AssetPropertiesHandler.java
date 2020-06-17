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


import asset.pipeline.ratpack.AssetPipelineModule;
import asset.pipeline.ratpack.internal.AssetProperties;
import ratpack.file.MimeTypes;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.path.PathBinding;

import java.nio.charset.Charset;

import static ratpack.exec.registry.Registry.single;

public class AssetPropertiesHandler implements Handler {
  @Override
  public void handle(Context ctx) throws Exception {
    AssetPipelineModule.Config config = ctx.get(AssetPipelineModule.Config.class);

    String path = getPath(ctx, config.getUrl());
    String indexedPath = getIndexedPath(path, config.getIndexFile());
    String format = indexedPath == null ? getFormat(ctx, path) : getFormat(ctx, indexedPath);
    String encoding = getEncoding(ctx);
    ctx.next(single(new AssetProperties(path, indexedPath, format, encoding)));
  }

  private static String getIndexedPath(String path, String indexFile) {
    if (path.endsWith("/") || "".equals(path)) {
      return "/".equals(path) ? "/"+indexFile : String.format("%s/%s", path, indexFile);
    } else {
      return null;
    }
  }

  private static String getFormat(Context context, String path) {
    return context.get(MimeTypes.class).getContentType(path);
  }

  private static String getEncoding(Context context) {
    String encoding = context.getRequest().getQueryParams().get("encoding");
    if(encoding == null) {
      encoding = Charset.defaultCharset().name();
    }
    return encoding;
  }

  private static String getPath(Context context, String baseAssetUrl) {
    String path = normalizePath(context.maybeGet(PathBinding.class)
      .map(PathBinding::getPastBinding)
      .orElse(context.getRequest().getPath()));

    if (path.startsWith(baseAssetUrl) && path.length() > baseAssetUrl.length()) {
      return path.substring(baseAssetUrl.length());
    } else if (path.startsWith(baseAssetUrl) && baseAssetUrl.equals(path)) {
      return "/";
    } else {
      return path;
    }
  }

  private static String normalizePath(String path) {
    String[] parts = path.split("/");
    String fileName = parts[parts.length-1];
    if (!fileName.contains(".")) {
      path = path + '/';
    }
    return path;
  }
}
