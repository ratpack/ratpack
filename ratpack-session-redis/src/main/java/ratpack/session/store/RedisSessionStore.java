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

package ratpack.session.store;


import com.google.common.base.Charsets;
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
import ratpack.exec.Operation;
import ratpack.exec.Promise;
import ratpack.server.StartEvent;
import ratpack.server.StopEvent;
import ratpack.session.SessionStore;

public class RedisSessionStore implements SessionStore {

  private RedisClient redisClient;
  private RedisAsyncConnection<String, String> connection;
  private RedisSessionModule.Config config;


  @Inject
  public RedisSessionStore(RedisSessionModule.Config config) {
    this.config = config;
  }

  @Override
  public Operation store(AsciiString sessionId, ByteBuf sessionData) {

    return Operation.of(() -> {
      RedisFuture<String> setResult = connection.set(config.getSessionKeyPrefix() + sessionId, sessionData.toString(Charsets.UTF_8));
      Futures.addCallback(setResult, new FutureCallback<String>() {
        @Override
        public void onSuccess(String result) {
          if (result != null && !result.equalsIgnoreCase("OK")) {
            throw new RuntimeException("Failed to set session data");
          }
        }

        @Override
        public void onFailure(Throwable t) {
          throw new RuntimeException("Failed to set session data.", t);
        }
      });
    });

  }

  @Override
  public Promise<ByteBuf> load(AsciiString sessionId) {

    Promise<String> sessionStrPromise = Promise.of(downstream -> {
      downstream.accept(connection.get(config.getSessionKeyPrefix() + sessionId));
    });

    return sessionStrPromise.map(sessionStr -> {
      if (sessionStr == null) {
        //If string is empty return an empty buffer to avoid a NPE in the copiedBuffer call
        return Unpooled.buffer(0, 0);
      }
      return Unpooled.copiedBuffer(sessionStr, Charsets.UTF_8);
    });
  }

  @Override
  public Operation remove(AsciiString sessionId) {
    return Operation.of(() -> {
      RedisFuture<Long> delResult = connection.del(config.getSessionKeyPrefix() + sessionId);

      Futures.addCallback(delResult, new FutureCallback<Long>() {
        @Override
        public void onSuccess(Long result) {
          //NOOP
        }

        @Override
        public void onFailure(Throwable t) {
          throw new RuntimeException("Failed to del session data.", t);
        }
      });

    });
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
    redisClient = new RedisClient(RedisURI.create(getConnectionString()));
    connection = redisClient.connectAsync();
  }

  @Override
  public void onStop(StopEvent event) throws Exception {
    connection.close();
    redisClient.shutdown();
  }

  private String getConnectionString() {
    String connectionString = "redis://";
    if (config.getPassword() != null) {
      connectionString += config.getPassword() + "@";
    }
    connectionString += config.getHost();
    if (config.getPort() != null) {
      connectionString += ":" + config.getPort();
    }
    return connectionString;
  }
}
