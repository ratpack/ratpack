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

import com.google.common.collect.ImmutableList;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;
import ratpack.path.internal.PathBindingStorage;

import java.nio.charset.Charset;
import java.nio.file.Path;

import static io.netty.util.internal.StringUtil.EMPTY_STRING;
import static ratpack.file.internal.FileRenderer.readAttributes;
import static ratpack.file.internal.FileRenderer.sendFile;

public class FileHandler implements Handler {

  private final ImmutableList<String> indexFiles;
  private final boolean cacheMetadata;

  public FileHandler(ImmutableList<String> indexFiles, boolean cacheMetadata) {
    this.indexFiles = indexFiles;
    this.cacheMetadata = cacheMetadata;
  }

  public void handle(Context context) throws Exception {
    String path = context.getExecution().get(PathBindingStorage.TYPE).peek().getPastBinding();
    String decodedPath = decodeComponent(path, CharsetUtil.UTF_8);
    Path asset = context.file(decodedPath);

    if (asset == null) {
      context.clientError(404);
    } else {
      servePath(context, asset);
    }
  }

  private void servePath(final Context context, final Path file) throws Exception {
    readAttributes(file, cacheMetadata, attributes -> {
      if (attributes == null) {
        context.next();
      } else if (attributes.isRegularFile()) {
        if (context.getRequest().getMethod().isGet()) {
          sendFile(context, file, attributes);
        } else {
          context.clientError(405);
        }
      } else if (attributes.isDirectory()) {
        if (context.getRequest().getMethod().isGet()) {
          maybeSendFile(context, file, 0);
        } else {
          context.clientError(405);
        }
      } else {
        context.next();
      }
    });
  }

  private void maybeSendFile(final Context context, final Path file, final int i) throws Exception {
    if (i == indexFiles.size()) {
      context.next();
    } else {
      String name = indexFiles.get(i);
      final Path indexFile = file.resolve(name);
      readAttributes(indexFile, cacheMetadata, attributes -> {
        if (attributes != null && attributes.isRegularFile()) {
          String path = context.getRequest().getPath();
          if (path.endsWith("/") || path.isEmpty()) {
            sendFile(context, indexFile, attributes);
          } else {
            context.redirect(currentUriWithTrailingSlash(context));
          }
        } else {
          maybeSendFile(context, file, i + 1);
        }
      });
    }
  }

  private String currentUriWithTrailingSlash(Context context) {
    Request request = context.getRequest();
    String redirectUri = "/" + request.getPath() + "/";
    String query = request.getQuery();
    if (!query.isEmpty()) {
      redirectUri += "?" + query;
    }
    return redirectUri;
  }

  /**
   * Copied from {@link QueryStringDecoder#decodeComponent(String, Charset)}, but patched to not decode "+" as " ".
   */
  @SuppressWarnings("fallthrough")
  private static String decodeComponent(final String s, final Charset charset) {
    if (s == null) {
      return EMPTY_STRING;
    }
    final int size = s.length();
    boolean modified = false;
    for (int i = 0; i < size; i++) {
      final char c = s.charAt(i);
      if (c == '%') {
        modified = true;
        break;
      }
    }
    if (!modified) {
      return s;
    }
    final byte[] buf = new byte[size];
    int pos = 0;  // position in `buf'.
    for (int i = 0; i < size; i++) {
      char c = s.charAt(i);
      switch (c) {
        case '%':
          if (i == size - 1) {
            throw new IllegalArgumentException("unterminated escape"
              + " sequence at end of string: " + s);
          }
          c = s.charAt(++i);
          if (c == '%') {
            buf[pos++] = '%';  // "%%" -> "%"
            break;
          }
          if (i == size - 1) {
            throw new IllegalArgumentException("partial escape"
              + " sequence at end of string: " + s);
          }
          c = decodeHexNibble(c);
          final char c2 = decodeHexNibble(s.charAt(++i));
          if (c == Character.MAX_VALUE || c2 == Character.MAX_VALUE) {
            throw new IllegalArgumentException(
              "invalid escape sequence `%" + s.charAt(i - 1)
                + s.charAt(i) + "' at index " + (i - 2)
                + " of: " + s);
          }
          c = (char) (c * 16 + c2);
          // Fall through.
        default:
          buf[pos++] = (byte) c;
          break;
      }
    }
    return new String(buf, 0, pos, charset);
  }

  private static char decodeHexNibble(final char c) {
    if ('0' <= c && c <= '9') {
      return (char) (c - '0');
    } else if ('a' <= c && c <= 'f') {
      return (char) (c - 'a' + 10);
    } else if ('A' <= c && c <= 'F') {
      return (char) (c - 'A' + 10);
    } else {
      return Character.MAX_VALUE;
    }
  }
}
