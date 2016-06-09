package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

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
