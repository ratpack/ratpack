package ratpack.http.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

/**
 * Creates a channel pool that does no pooling.
 */
public final class NonPoolingChannelPool implements ChannelPool {

  private final ChannelPoolHandler handler;
  private final Bootstrap bootstrap;

  public NonPoolingChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler) {
    this.handler = handler;
    this.bootstrap = bootstrap.clone();
    this.bootstrap.handler(new ChannelInitializer<Channel>() {
      @Override
      protected void initChannel(Channel ch) throws Exception {
        handler.channelCreated(ch);
      }
    });
  }

  @Override
  public Future<Channel> acquire() {
    return acquire(bootstrap.group().next().<Channel>newPromise());
  }

  @Override
  public Future<Channel> acquire(final Promise<Channel> promise) {
    checkNotNull(promise, "promise");
    Bootstrap bs = bootstrap.clone();
    ChannelFuture f = bs.connect();
    if (f.isDone()) {
      notifyConnect(f, promise);
    } else {
      f.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          notifyConnect(future, promise);
        }
      });
    }
    return promise;
  }

  @Override
  public Future<Void> release(Channel channel) {
    return release(channel, channel.eventLoop().<Void>newPromise());
  }

  @Override
  public Future<Void> release(final Channel channel, final Promise<Void> promise) {
    final Promise<Void> p = channel.eventLoop().newPromise();
    p.addListener(new FutureListener<Void>() {
      @Override
      public void operationComplete(Future<Void> future) throws Exception {
        handler.channelReleased(channel);
        channel.close();
        if (future.isSuccess()) {
          promise.setSuccess(null);
        } else {
          promise.setFailure(future.cause());
        }
      }
    });
    return p;
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
