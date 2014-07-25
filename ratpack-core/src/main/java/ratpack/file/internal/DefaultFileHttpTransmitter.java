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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.netty.channel.Channel;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.stream.ChunkedNioStream;
import ratpack.exec.ExecControl;
import ratpack.file.MimeTypes;
import ratpack.func.Action;
import ratpack.http.MutableHeaders;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;

public class DefaultFileHttpTransmitter implements FileHttpTransmitter {

  private final FullHttpRequest request;
  private final HttpHeaders httpHeaders;
  private final Channel channel;
  private final boolean compress;
  private final long startTime;
  private final long compressionMinSize;
  private final ImmutableSet<String> compressionMimeTypeWhiteList;
  private final ImmutableSet<String> compressionMimeTypeBlackList;
  private final Action<? super Action<? super ResponseTransmitter>> transmitterAction;

  public DefaultFileHttpTransmitter(FullHttpRequest request, HttpHeaders httpHeaders, Channel channel, MimeTypes mimeTypes, boolean compress, Long compressionMinSize,
                                      ImmutableSet<String> compressionMimeTypeWhiteList, ImmutableSet<String> compressionMimeTypeBlackList, long startTime, Action<? super Action<? super ResponseTransmitter>> transmitterAction) {
    this.request = request;
    this.httpHeaders = httpHeaders;
    this.channel = channel;
    this.compress = compress;
    this.startTime = startTime;
    this.compressionMinSize = compressionMinSize;
    this.compressionMimeTypeWhiteList = compressionMimeTypeWhiteList;
    this.compressionMimeTypeBlackList = compressionMimeTypeBlackList != null ? compressionMimeTypeBlackList : defaultExcludedMimeTypes(mimeTypes);
    this.transmitterAction = transmitterAction;
  }

  private static ImmutableSet<String> defaultExcludedMimeTypes(MimeTypes mimeTypes) {
    return ImmutableSet.copyOf(
      Iterables.concat(
        Iterables.filter(mimeTypes.getKnownMimeTypes(), new Predicate<String>() {
          @Override
          public boolean apply(String type) {
            return (type.startsWith("image/") || type.startsWith("audio/") || type.startsWith("video/")) && !type.endsWith("+xml");
          }
        }),
        ImmutableSet.of("application/compress", "application/zip", "application/gzip")
      )
    );
  }

  @Override
  public void transmit(ExecControl execContext, final BasicFileAttributes basicFileAttributes, final Path file) throws Exception {
    final boolean compressThis = compress && basicFileAttributes.size() > compressionMinSize && isContentTypeCompressible();

    if (compress && !compressThis) {
      // Signal to the compressor not to compress this
      httpHeaders.set(HttpHeaderConstants.CONTENT_ENCODING, HttpHeaders.Values.IDENTITY);
    }

    if (file.getFileSystem().equals(FileSystems.getDefault()) && !compressThis) {
      execContext.blocking(new Callable<FileChannel>() {
        public FileChannel call() throws Exception {
          return new FileInputStream(file.toFile()).getChannel();
        }
      }).then(new Action<FileChannel>() {
        public void execute(FileChannel fileChannel) throws Exception {
          FileRegion defaultFileRegion = new DefaultFileRegion(fileChannel, 0, basicFileAttributes.size());
          transmit(basicFileAttributes, defaultFileRegion);
        }
      });
    } else {
      execContext.blocking(new Callable<ReadableByteChannel>() {
        public ReadableByteChannel call() throws Exception {
          return Files.newByteChannel(file);
        }
      }).then(new Action<ReadableByteChannel>() {
        public void execute(ReadableByteChannel fileChannel) throws Exception {
          transmit(basicFileAttributes, new ChunkedInputAdapter(new ChunkedNioStream(fileChannel)));
        }
      });
    }
  }

  private void transmit(final BasicFileAttributes basicFileAttributes, final Object message) throws Exception {
    transmitterAction.execute(new Action<ResponseTransmitter>() {
      @Override
      public void execute(ResponseTransmitter responseTransmitter) {
        HttpHeaders httpHeaders = new DefaultHttpHeaders(false);
        MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(httpHeaders);
        responseTransmitter.transmit(HttpResponseStatus.OK, responseHeaders, basicFileAttributes.size(), message);
      }
    });
  }

  private boolean isContentTypeCompressible() {
    final String contentType = httpHeaders.get(HttpHeaderConstants.CONTENT_TYPE);
    Predicate<String> contentTypeMatch = new PrefixMatchPredicate(contentType);
    return (compressionMimeTypeWhiteList == null || (contentType != null && Iterables.any(compressionMimeTypeWhiteList, contentTypeMatch)))
      && (contentType == null || !Iterables.any(compressionMimeTypeBlackList, contentTypeMatch));
  }

  private static class PrefixMatchPredicate implements Predicate<String> {
    private final String value;

    PrefixMatchPredicate(String value) {
      this.value = value;
    }

    @Override
    public boolean apply(String possiblePrefix) {
      return value.startsWith(possiblePrefix);
    }
  }

}
