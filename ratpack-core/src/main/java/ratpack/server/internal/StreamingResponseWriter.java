/*
 * Copyright 2021 the original author or authors.
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Consumer;

class StreamingResponseWriter implements ResponseWriter {

  private final Publisher<? extends ByteBuf> publisher;

  private boolean done;
  private Subscription subscription;

  public StreamingResponseWriter(Publisher<? extends ByteBuf> publisher) {
    this.publisher = publisher;
  }

  @Override
  public void write(
    Channel channel,
    Consumer<? super ResponseWritingListener> listenerReceiver,
    Consumer<? super ChannelFuture> then
  ) {
    publisher.subscribe(new Subscriber<ByteBuf>() {

      @Override
      public void onSubscribe(Subscription incomingSubscription) {
        if (incomingSubscription == null) {
          throw new NullPointerException("'subscription' is null");
        }

        if (subscription != null) {
          incomingSubscription.cancel();
          return;
        }

        subscription = incomingSubscription;

        listenerReceiver.accept(new ResponseWritingListener() {
          @Override
          public void onClosed() {
            if (!done) {
              done = true;
              subscription.cancel();
              then.accept(channel.pipeline().newSucceededFuture());
            }
          }

          @Override
          public void onWritable() {
            if (!done) {
              subscription.request(1);
            }
          }
        });

        if (channel.isWritable()) {
          subscription.request(1);
        }
      }

      @Override
      public void onNext(ByteBuf o) {
        o.touch();
        if (!done) {
          channel.writeAndFlush(new DefaultHttpContent(o.touch()));
          if (channel.isWritable()) {
            subscription.request(1);
          }
        } else {
          o.release();
        }
      }

      @Override
      public void onError(Throwable t) {
        if (t == null) {
          throw new NullPointerException("error is null");
        }
        if (!done) {
          done = true;
          then.accept(channel.newFailedFuture(t));
        }
      }

      @Override
      public void onComplete() {
        if (!done) {
          done = true;
          then.accept(channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT));
        }
      }
    });
  }
}
