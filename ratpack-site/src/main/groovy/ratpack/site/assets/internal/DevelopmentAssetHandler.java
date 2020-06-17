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


import asset.pipeline.ratpack.internal.AssetProperties;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Response;

import java.util.Date;

import static asset.pipeline.AssetPipeline.serveAsset;
import static asset.pipeline.AssetPipeline.serveUncompiledAsset;
import static ratpack.exec.registry.Registry.single;

public class DevelopmentAssetHandler implements Handler {
  private static final String COMPILE_PARAM = "compile";

  @Override
  public void handle(Context ctx) throws Exception {
    final AssetProperties props = ctx.get(AssetProperties.class);

    byte[] fileContents = getFileContents(ctx, props);

    if (fileContents != null) {
      setNoCache(ctx, fileContents.length);
      ctx.getResponse().contentTypeIfNotSet(props.getFormat());
      ctx.getResponse().send(fileContents);
    } else if (props.getIndexedPath() != null) {
      ctx.insert(single(new AssetProperties(props.getIndexedPath(), null, props.getFormat(), props.getEncoding())), this);
    } else {
      ctx.next();
    }
  }

  private byte[] getFileContents(Context context, AssetProperties props) {
    byte[] fileContents = shouldCompile(context)
      ? serveAsset(props.getPath(), props.getFormat(), null, props.getEncoding())
      : serveUncompiledAsset(props.getPath(), props.getFormat(), null, props.getEncoding());

    return fileContents != null && props.getIndexedPath() != null
      ? getFileContents(context, props)
      : fileContents;

  }

  private static boolean shouldCompile(Context context) {
    return !"false".equals(context.getRequest().getQueryParams().get(COMPILE_PARAM));
  }

  private static void setNoCache(Context context, long contentLength) {
    Response response = context.getResponse();
    response.getHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
    response.getHeaders().set("Pragma", "no-cache"); // HTTP 1.0.
    response.getHeaders().setDate("Expires", new Date(0)); // Proxies.
    response.getHeaders().set("Content-Length", Long.toString(contentLength));
  }


}
