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

import com.google.inject.Inject;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import ratpack.exec.Execution;
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.session.SessionStore;
import ratpack.session.store.RedisSessionModule;

public class RedisSessionStore implements SessionStore {

  private final RedisSessionModule.Config config;

  private TimerExposingRedisClient redisClient;
  private RedisAsyncCommands<AsciiString, ByteBuf> connection;

  @Inject
  public RedisSessionStore(RedisSessionModule.Config config) {
    this.config = config;
  }

  @Override
  public Operation store(AsciiString sessionId, ByteBuf sessionData) {
    return Promise.<Boolean>async(d ->
      connection.set(sessionId, sessionData).handleAsync((value, failure) -> {
        if (failure == null) {
          if (value != null && value.equalsIgnoreCase("OK")) {
            d.success(true);
          } else {
            d.error(new RuntimeException("Failed to set session data"));
          }
        } else {
          d.error(new RuntimeException("Failed to set session data.", failure));
        }
        return null;
      }, Execution.current().getEventLoop())
    ).operation();
  }

  @Override
  public Promise<ByteBuf> load(AsciiString sessionId) {
    return Promise.<ByteBuf>async(downstream -> {
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
    return Promise.<Long>async(d -> d.accept(connection.del(sessionId))).operation();
  }

  @Override
  public Promise<Long> size() {
    return Promise.value(-1L);
  }

  @Override
  public String getName() {
    return "Redis Session Store Service";
  }

  @Override
  public void onStart(@SuppressWarnings("deprecation") ratpack.server.StartEvent event) throws Exception {
    redisClient = new TimerExposingRedisClient(getRedisURI());
    connection = redisClient.connect(new AsciiStringByteBufRedisCodec()).async();
  }

  @Override
  public void onStop(@SuppressWarnings("deprecation") ratpack.server.StopEvent event) throws Exception {
    if (redisClient != null) {
      try {
        redisClient.getTimer().stop();
        redisClient.shutdown();
      } finally {
        redisClient = null;
      }
    }
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
