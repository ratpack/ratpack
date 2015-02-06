/*
 * Copyright 2015 the original author or authors.
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

package ratpack.server.internal;

import com.google.common.collect.ImmutableSet;
import ratpack.server.CompressionConfig;

import java.util.List;

public class DefaultCompressionConfigBuilder implements CompressionConfig.Builder {

  private boolean compressResponses;
  private long minSize = CompressionConfig.DEFAULT_COMPRESSION_MIN_SIZE;
  private final ImmutableSet.Builder<String> mimeTypeWhiteList = ImmutableSet.builder();
  private final ImmutableSet.Builder<String> mimeTypeBlackList = ImmutableSet.builder();

  @Override
  public CompressionConfig.Builder compressResponses(boolean compressResponses) {
    this.compressResponses = compressResponses;
    return this;
  }

  @Override
  public CompressionConfig.Builder minSize(long minSize) {
    this.minSize = minSize;
    return this;
  }

  @Override
  public CompressionConfig.Builder whiteListMimeTypes(String... mimeTypes) {
    this.mimeTypeWhiteList.add(mimeTypes);
    return this;
  }

  @Override
  public CompressionConfig.Builder whiteListMimeTypes(List<String> mimeTypes) {
    this.mimeTypeWhiteList.addAll(mimeTypes);
    return this;
  }

  @Override
  public CompressionConfig.Builder blackListMimeTypes(String... mimeTypes) {
    this.mimeTypeBlackList.add(mimeTypes);
    return this;
  }

  @Override
  public CompressionConfig.Builder blackListMimeTypes(List<String> mimeTypes) {
    this.mimeTypeBlackList.addAll(mimeTypes);
    return this;
  }

  @Override
  public CompressionConfig build() {
    return new DefaultCompressionConfig(compressResponses, minSize, mimeTypeWhiteList.build(), mimeTypeBlackList.build());
  }

}
