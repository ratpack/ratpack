/*
 * Copyright 2018 the original author or authors.
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

package ratpack.dropwizard.metrics;

import io.netty.buffer.ByteBufAllocator;

/**
 * Configuration for collecting metrics about {@link ByteBufAllocator}.
 *
 * @since 1.6
 */
public class ByteBufAllocatorConfig {
  private boolean enabled = true;
  private boolean detailed = true;

  /**
   * The state of the ByteBufAllocator metric collector
   *
   * @return the state of the {@link ByteBufAllocator} metric collector.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the state of the {@link ByteBufAllocator} metric collector.
   *
   * @param enabled True if {@link ByteBufAllocator} metrics should be collected. False otherwise
   * @return {@code this}
   */
  public ByteBufAllocatorConfig enable(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * The state of detailed metric collection for the {@link ByteBufAllocator} metric collector.
   *
   * @return The state of detailed metric collection for the {@link ByteBufAllocator} metric collector.
   */
  public boolean isDetailed() {
    return detailed;
  }

  /**
   * Set the state of detailed metric collectrion for the {@link ByteBufAllocator} metric collector.
   *
   * @param detailed The state of detailed metric collection for the {@link ByteBufAllocator} metric collector.
   * @return {@code this}
   */
  public ByteBufAllocatorConfig detail(boolean detailed) {
    this.detailed = detailed;
    return this;
  }
}
