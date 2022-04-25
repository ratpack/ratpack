/*
 * Copyright 2022 the original author or authors.
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

import ratpack.exec.Execution;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

/**
 * Integration with <a href="https://github.com/reactor/BlockHound">BlockHound</a>.
 *
 * @since 2.0
 */
public class RatpackBlockHound {

  /**
   * Install and activate BlockHound. Due to https://github.com/reactor/BlockHound/issues/273
   * this <bold>must</bold> be how Ratpack integration is activated and as such SPI integration
   * is not provided.
   * <p>
   * If after installing, there are errors generated for method calls on Ratpack's blocking threads,
   * it is because the Ratpack's integration was loaded prior to Netty's resulting in https://github.com/netty/netty/issues/12125
   */
  public static void install() {
    BlockHound.install(new RatpackBlockHoundIntegration());
  }

  /**
   * Enables Ratpack integration with BlockHound by mapping Ratpack's compute and blocking threads
   * to be non-blocking and blocking respectively and declaring blocking help methods to be allowed.
   */
  public static class RatpackBlockHoundIntegration implements  BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
      builder.allowBlockingCallsInside("ratpack.core.server.internal.DefaultRatpackServer", "buildChannel");
      builder.allowBlockingCallsInside("ratpack.core.http.client.internal.DefaultHttpClient", "close");
      builder.allowBlockingCallsInside("io.netty.channel.pool.SimpleChannelPool", "close");
      builder.allowBlockingCallsInside("io.netty.channel.pool.FixedChannelPool", "close");
      builder.allowBlockingCallsInside("ratpack.exec.internal.DefaultExecController", "close");
      builder.allowBlockingCallsInside("ratpack.test.internal.BlockingHttpClient", "request");

      builder.nonBlockingThreadPredicate(current ->
        t -> Execution.isManagedThread() ? Execution.isComputeThread() : current.test(t)
      );
    }

    @Override
    public int compareTo(BlockHoundIntegration o) {
      if (o.getClass().getName().equals("io.netty.util.internal.Hidden$NettyBlockHoundIntegration")) {
        return 1;
      }
      return BlockHoundIntegration.super.compareTo(o);
    }
  }
}
