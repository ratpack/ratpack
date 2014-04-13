/*
 * Copyright 2014 the original author or authors.
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

package ratpack.test;

import io.netty.buffer.ByteBufAllocator;
import ratpack.api.Nullable;
import ratpack.file.FileSystemBinding;
import ratpack.exec.Background;
import ratpack.exec.Foreground;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link LaunchConfig} implementation designed to be used when unit testing.
 * <p>
 * Methods of this class return sensible default values where possible, and throw {@link UnsupportedOperationException} when that is not possible.
 * If your test requires specific values, simply override the relevant method.
 */
public class MockLaunchConfig implements LaunchConfig {

  /**
   * Throws {@link java.lang.UnsupportedOperationException}
   *
   * @return n/a.
   */
  @Override
  public FileSystemBinding getBaseDir() {
    throw new UnsupportedOperationException();
  }

  /**
   * Throws {@link java.lang.UnsupportedOperationException}
   *
   * @return n/a.
   */
  @Override
  public HandlerFactory getHandlerFactory() {
    return null;
  }

  /**
   * Returns 0.
   *
   * @return 0
   */
  @Override
  public int getPort() {
    return 0;
  }

  /**
   * Returns null.
   *
   * @return null
   */
  @Nullable
  @Override
  public InetAddress getAddress() {
    return null;
  }

  /**
   * Returns false.
   *
   * @return false.
   */
  @Override
  public boolean isReloadable() {
    return false;
  }

  /**
   * Returns 0.
   *
   * @return 0
   */
  @Override
  public int getThreads() {
    return 0;
  }

  /**
   * Throws {@link java.lang.UnsupportedOperationException}
   *
   * @return n/a.
   */
  @Override
  public Background getBackground() {
    return null;
  }

  /**
   * Throws {@link java.lang.UnsupportedOperationException}
   *
   * @return n/a.
   */
  @Override
  public Foreground getForeground() {
    return null;
  }

  /**
   * Throws {@link java.lang.UnsupportedOperationException}
   *
   * @return n/a.
   */
  @Override
  public ByteBufAllocator getBufferAllocator() {
    return null;
  }

  /**
   * Returns {@code new URI("http://localhost:5050")}.
   *
   * @return {@code new URI("http://localhost:5050")}
   */
  @Override
  public URI getPublicAddress() {
    try {
      return new URI("http://localhost:5050");
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns {@code Collections.emptyList()}.
   *
   * @return {@code Collections.emptyList()}
   */
  @Override
  public List<String> getIndexFiles() {
    return Collections.emptyList();
  }

  /**
   * Returns null.
   *
   * @return null.
   */
  @Nullable
  @Override
  public SSLContext getSSLContext() {
    return null;
  }

  /**
   * Returns the given {@code defaultValue},
   *
   * @param key The property key
   * @param defaultValue The value to return if the property was not set
   * @return the given {@code defaultValue},
   */
  @Override
  public String getOther(String key, String defaultValue) {
    return null;
  }

  /**
   * Returns {@code Collections.emptyMap()}
   *
   * @param prefix Property name prefix that should be used for filtering
   * @return {@code Collections.emptyMap()}
   */
  @Override
  public Map<String, String> getOtherPrefixedWith(String prefix) {
    return Collections.emptyMap();
  }

  /**
   * Returns 0.
   *
   * @return 0
   */
  @Override
  public int getMaxContentLength() {
    return 0;
  }

  /**
   * Returns false.
   *
   * @return false
   */
  @Override
  public boolean isTimeResponses() {
    return false;
  }

  /**
   * Returns false.
   *
   * @return false
   */
  @Override
  public boolean isCompressResponses() {
    return false;
  }

}
