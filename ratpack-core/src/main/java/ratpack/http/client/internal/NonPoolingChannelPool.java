/*
 * Copyright 2016 the original author or authors.
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

package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import ratpack.func.Action;

/**
 * Creates a channel pool that does no pooling.
 */
public final class NonPoolingChannelPool implements ChannelPool {

  private final ChannelPoolHandler handler;
  private final Bootstrap bootstrap;

  public NonPoolingChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler) {
    this.handler = handler;
    this.bootstrap = bootstrap;
    this.bootstrap.handler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        handler.channelCreated(ch);
      }
    });
  }

  @Override
  public Future<Channel> acquire() {
    return acquire(bootstrap.config().group().next().<Channel>newPromise());
  }

  @Override
  public Future<Channel> acquire(final Promise<Channel> promise) {
    try {
      ChannelFuture f = bootstrap.connect();
      if (f.isDone()) {
        notifyConnect(f, promise);
      } else {
        f.addListener(f1 -> notifyConnect(f, promise));
      }
    } catch (Throwable cause) {
      promise.setFailure(cause);
    }
    return promise;
  }

  @Override
  public Future<Void> release(Channel channel) {
    return release(channel, null);
  }

  @Override
  public Future<Void> release(final Channel channel, final Promise<Void> promise) {
    try {
      handler.channelReleased(channel);
    } catch (Exception e) {
      Action.noop();
    }
    return promise;
  }

  @Override
  public void close() {
  }

  private static void notifyConnect(ChannelFuture future, Promise<Channel> promise) {
    if (future.isSuccess()) {
      promise.setSuccess(future.channel());
    } else {
      promise.setFailure(future.cause());
    }
  }

}
