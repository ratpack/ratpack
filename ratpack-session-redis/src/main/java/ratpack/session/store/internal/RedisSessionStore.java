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

package ratpack.session.store.internal;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.server.StartEvent;
import ratpack.server.StopEvent;
import ratpack.session.SessionStore;
import ratpack.session.store.RedisSessionModule;

public class RedisSessionStore implements SessionStore {

  private RedisClient redisClient;
  private RedisAsyncConnection<AsciiString, ByteBuf> connection;
  private RedisSessionModule.Config config;


  @Inject
  public RedisSessionStore(RedisSessionModule.Config config) {
    this.config = config;
  }

  @Override
  public Operation store(AsciiString sessionId, ByteBuf sessionData) {

    return Promise.<Boolean>of(d -> {
      RedisFuture<String> setResult = connection.set(sessionId, sessionData);
      Futures.addCallback(setResult, new FutureCallback<String>() {
        @Override
        public void onSuccess(String result) {
          if (result != null && result.equalsIgnoreCase("OK")) {
            d.success(true);
          } else {
            d.error(new RuntimeException("Failed to set session data"));
          }
        }

        @Override
        public void onFailure(Throwable t) {
          d.error(new RuntimeException("Failed to set session data.", t));
        }
      }, Execution.current().getEventLoop());
    }).operation();

  }

  @Override
  public Promise<ByteBuf> load(AsciiString sessionId) {

    return Promise.<ByteBuf>of(downstream -> {
      downstream.accept(connection.get(sessionId));
    }).map(byteBuf -> {
      if (byteBuf == null) {
        //Must return an empty buffer never null
        return Unpooled.EMPTY_BUFFER;
      }
      return byteBuf;
    });
  }

  @Override
  public Operation remove(AsciiString sessionId) {
    return Promise.<Long>of(d -> {
      d.accept(connection.del(sessionId));
    }).operation();
  }

  @Override
  public Promise<Long> size() {
    return Promise.value(-1L);
  }


  @Override
  public String getName() {
    return "Redis Session Store";
  }

  @Override
  public void onStart(StartEvent event) throws Exception {
    redisClient = new RedisClient(getRedisURI());
    AsciiStringByteBufRedisCodec asciiStringByteBufRedisCodec = new AsciiStringByteBufRedisCodec();
    connection = redisClient.connectAsync(asciiStringByteBufRedisCodec);
  }

  @Override
  public void onStop(StopEvent event) throws Exception {
    connection.close();
    redisClient.shutdown();
  }

  private RedisURI getRedisURI() {
    RedisURI.Builder builder = RedisURI.Builder.redis(config.getHost());

    if (config.getPassword() != null) {
      builder.withPassword(config.getPassword());
    }

    if (config.getPort() != null) {
      builder.withPort(config.getPort());
    }

    return builder.build();
  }
}
