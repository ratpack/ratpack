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

package ratpack.file.internal;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpHeaderNames;
import ratpack.exec.Blocking;
import ratpack.file.MimeTypes;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderer;
import ratpack.render.RendererSupport;
import ratpack.util.Exceptions;
import ratpack.util.Types;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.Optional;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;

public class FileRenderer extends RendererSupport<Path> {

  private final boolean cacheMetadata;

  public static final TypeToken<Renderer<Path>> TYPE = Types.intern(new TypeToken<Renderer<Path>>() {});

  public static final Renderer<Path> CACHING = new FileRenderer(true);
  public static final Renderer<Path> NON_CACHING = new FileRenderer(false);

  private FileRenderer(boolean cacheMetadata) {
    this.cacheMetadata = cacheMetadata;
  }

  private static Cache<Path, Optional<BasicFileAttributes>> cache;

  @Override
  public void render(Context ctx, Path targetFile) throws Exception {
    readAttributes(targetFile, cacheMetadata, attributes -> {
      if (attributes == null || !attributes.isRegularFile()) {
        ctx.clientError(404);
      } else {
        sendFile(ctx, targetFile, attributes);
      }
    });
  }

  public static void sendFile(Context context, Path file, BasicFileAttributes attributes) {
    Date date = new Date(attributes.lastModifiedTime().toMillis());

    context.lastModified(date, () -> {
      final String ifNoneMatch = context.getRequest().getHeaders().get(HttpHeaderNames.IF_NONE_MATCH);
      Response response = context.getResponse();
      if (ifNoneMatch != null && ifNoneMatch.trim().equals("*")) {
        response.status(NOT_MODIFIED.code()).send();
        return;
      }

      response.contentTypeIfNotSet(() -> context.get(MimeTypes.class).getContentType(file.getFileName().toString()));
      response.getHeaders().set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(attributes.size()));
      try {
        response.sendFile(file);
      } catch (Exception e) {
        throw Exceptions.uncheck(e);
      }
    });
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

  public static void readAttributes(Path file, boolean cacheMetadata, Action<? super BasicFileAttributes> then) throws Exception {
    if (cacheMetadata) {
      Optional<BasicFileAttributes> basicFileAttributes = getCache().getIfPresent(file);
      if (basicFileAttributes == null) {
        Blocking.get(getter(file)).then(a -> {
          getCache().put(file, Optional.ofNullable(a));
          then.execute(a);
        });
      } else {
        then.execute(basicFileAttributes.orElse(null));
      }
    } else {
      Blocking.get(getter(file)).then(then);
    }
  }

  private static Cache<Path, Optional<BasicFileAttributes>> getCache() {
    if (cache == null) {
      cache = Caffeine.newBuilder().maximumSize(10000).build();
    }
    return cache;
  }

}
