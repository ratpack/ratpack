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

public class DefaultCompressionConfig implements CompressionConfig {

  private final boolean compressResponses;
  private final long minSize;
  private final ImmutableSet<String> mimeTypeWhiteList;
  private final ImmutableSet<String> mimeTypeBlackList;

  /**
   * Used by Jackson in ratpack-config.
   */
  @SuppressWarnings("UnusedDeclaration")
  public DefaultCompressionConfig() {
    this(false, CompressionConfig.DEFAULT_COMPRESSION_MIN_SIZE, ImmutableSet.of(), ImmutableSet.of());
  }

  public DefaultCompressionConfig(
    boolean compressResponses,
    long minSize,
    ImmutableSet<String> mimeTypeWhiteList,
    ImmutableSet<String> mimeTypeBlackList
  ) {
    this.compressResponses = compressResponses;
    this.minSize = minSize;
    this.mimeTypeWhiteList = mimeTypeWhiteList;
    this.mimeTypeBlackList = mimeTypeBlackList;
  }

  @Override
  public boolean isCompressResponses() {
    return compressResponses;
  }

  @Override
  public long getMinSize() {
    return minSize;
  }

  @Override
  public ImmutableSet<String> getMimeTypeWhiteList() {
    return mimeTypeWhiteList;
  }

  @Override
  public ImmutableSet<String> getMimeTypeBlackList() {
    return mimeTypeBlackList;
  }

}
