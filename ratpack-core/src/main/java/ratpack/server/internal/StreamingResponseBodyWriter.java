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
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

class StreamingResponseBodyWriter implements ResponseBodyWriter, ResponseWritingListener {

  private final Publisher<? extends ByteBuf> publisher;

  private boolean done;
  private Subscription subscription;
  private ChannelPromise channelPromise;

  public StreamingResponseBodyWriter(Publisher<? extends ByteBuf> publisher) {
    this.publisher = publisher;
  }

  @Override
  public void onClosed() {
    if (!done) {
      done = true;
      if (subscription != null) {
        subscription.cancel();
      }
      channelPromise.setSuccess();
    }
  }

  @Override
  public void onWritable() {
    if (!done && subscription != null) {
      subscription.request(1);
    }
  }

  @Override
  public ChannelPromise write(Channel channel) {
    channelPromise = channel.newPromise();
    publisher.subscribe(new Subscriber(channel));
    return channelPromise;
  }

  private class Subscriber implements org.reactivestreams.Subscriber<ByteBuf> {

    private final Channel channel;

    public Subscriber(Channel channel) {
      this.channel = channel;
    }

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
      if (channel.isWritable()) {
        subscription.request(1);
      }
    }

    @Override
    public void onNext(ByteBuf o) {
      o.touch();

      if (o.readableBytes() == 0) {
        o.release();
        subscription.request(1);
        return;
      }

      if (done) {
        o.release();
        return;
      }

      channel.writeAndFlush(new DefaultHttpContent(o.touch()), channel.voidPromise());
      if (channel.isWritable()) {
        subscription.request(1);
      }
    }

    @Override
    public void onError(Throwable t) {
      if (t == null) {
        throw new NullPointerException("error is null");
      }
      if (!done) {
        done = true;
        channelPromise.setFailure(t);
      }
    }

    @Override
    public void onComplete() {
      if (!done) {
        done = true;
        channel.write(LastHttpContent.EMPTY_LAST_CONTENT, channelPromise);
        channel.flush();
      }
    }
  }
}
