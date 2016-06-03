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
 *
 * TODO we get a lot of these errors perf testing:
 * [ratpack-compute-1-3] WARN io.netty.channel.DefaultChannelPipeline - An exceptionCaught() event was fired, and
 * it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.
 * io.netty.handler.timeout.ReadTimeoutException
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
    return acquire(bootstrap.config().group().next().<Channel>newPromise());
  }

  @Override
  public Future<Channel> acquire(final Promise<Channel> promise) {
    try {
      ChannelFuture f = bootstrap.connect();
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
    } catch (Throwable cause) {
      promise.setFailure(cause);
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
    try {
      handler.channelReleased(channel);
      p.addListener(new FutureListener<Void>() {
        @Override
        public void operationComplete(Future<Void> future) throws Exception {
          if (future.isSuccess()) {
            promise.setSuccess(null);
          } else {
            promise.setFailure(future.cause());
          }
        }
      });
    } catch (Throwable cause) {
      promise.setFailure(cause);
    } finally {
      channel.close();
    }
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
