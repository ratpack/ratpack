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

package ratpack.server;

import com.google.common.collect.ImmutableSet;
import ratpack.server.internal.DefaultCompressionConfigBuilder;

import java.util.List;

/**
 * Compression configuration settings.
 */
public interface CompressionConfig {
  /**
   * The default compression minimum size in bytes, {@value}.
   */
  public long DEFAULT_COMPRESSION_MIN_SIZE = 1024;

  /**
   * Creates a new compression config builder.
   *
   * @return a new compression config builder
   */
  static Builder of() {
    return new DefaultCompressionConfigBuilder();
  }

  /**
   * Whether or not responses should be compressed.
   *
   * @return whether or not responses should be compressed.
   */
  boolean isCompressResponses();

  /**
   * The minimum size at which responses should be compressed, in bytes.
   *
   * @return the minimum size at which responses should be compressed.
   */
  long getMinSize();

  /**
   * The response mime types which should be compressed.
   * <p>
   * If empty, defaults to all mime types not on the black list.
   *
   * @return the response mime types which should be compressed.
   */
  ImmutableSet<String> getMimeTypeWhiteList();

  /**
   * The response mime types which should not be compressed.
   * <p>
   * If empty, uses a default that excludes many commonly used compressed types.
   *
   * @return the response mime types which should not be compressed.
   */
  ImmutableSet<String> getMimeTypeBlackList();

  /**
   * Builds a new compression config.
   */
  interface Builder {
    /**
     * Whether to compress responses.
     *
     * Default value is {@code false}.
     *
     * @param compressResponses Whether to compress responses
     * @return this
     * @see CompressionConfig#isCompressResponses()
     */
    Builder compressResponses(boolean compressResponses);

    /**
     * The minimum size at which responses should be compressed, in bytes.
     *
     * @param minSize The minimum size at which responses should be compressed, in bytes
     * @return this
     * @see CompressionConfig#getMinSize()
     */
    Builder minSize(long minSize);

    /**
     * Adds the given values as compressible mime types.
     *
     * @param mimeTypes the compressible mime types.
     * @return this
     * @see CompressionConfig#getMimeTypeWhiteList()
     */
    Builder whiteListMimeTypes(String... mimeTypes);

    /**
     * Adds the given values as compressible mime types.
     *
     * @param mimeTypes the compressible mime types.
     * @return this
     * @see CompressionConfig#getMimeTypeWhiteList()
     */
    Builder whiteListMimeTypes(List<String> mimeTypes);

    /**
     * Adds the given values as non-compressible mime types.
     *
     * @param mimeTypes the non-compressible mime types.
     * @return this
     * @see CompressionConfig#getMimeTypeBlackList()
     */
    Builder blackListMimeTypes(String... mimeTypes);

    /**
     * Adds the given values as non-compressible mime types.
     *
     * @param mimeTypes the non-compressible mime types.
     * @return this
     * @see CompressionConfig#getMimeTypeBlackList()
     */
    Builder blackListMimeTypes(List<String> mimeTypes);

    /**
     * Builds the compression config.
     *
     * @return the compression config
     */
    CompressionConfig build();
  }
}
