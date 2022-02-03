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

package ratpack.core.server.internal;

import ratpack.core.server.RatpackServer;
import ratpack.func.Block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public abstract class ServerCapturer {

  private static final ThreadLocal<Deque<Consumer<? super RatpackServer>>> CAPTURER_HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

  private ServerCapturer() {
  }

  public interface Releaser extends AutoCloseable {
    @Override
    void close();
  }

  public static Releaser captureWith(Consumer<? super RatpackServer> capturer) {
    Deque<Consumer<? super RatpackServer>> capturers = CAPTURER_HOLDER.get();
    capturers.addLast(capturer);
    return () -> capturers.remove(capturer);
  }

  @SuppressWarnings("try")
  public static RatpackServer capture(Block bootstrap) throws Exception {
    class Capturer implements Consumer<RatpackServer> {
      RatpackServer server;

      @Override
      public void accept(RatpackServer ratpackServer) {
        server = ratpackServer;
      }
    }
    Capturer capturer = new Capturer();
    try (Releaser ignored = captureWith(capturer)) {
      bootstrap.execute();
    }

    return capturer.server;
  }

  public static void capture(RatpackServer server) throws Exception {
    Consumer<? super RatpackServer> capturer = CAPTURER_HOLDER.get().pollLast();
    if (capturer != null) {
      capturer.accept(server);
    }
  }

}
