/*
 * Copyright 2013 the original author or authors.
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

package ratpack.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import ratpack.util.internal.IoUtils;
import ratpack.util.internal.ReleasingAction;

import java.io.IOException;
import java.io.InputStream;

import static ratpack.util.ExceptionUtils.uncheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the version of the Ratpack core at runtime.
 */
public class RatpackVersion {
  private final static Logger LOGGER = LoggerFactory.getLogger(RatpackVersion.class);

  private RatpackVersion() {
  }

  /**
   * The version of Ratpack.
   *
   * @return The version of Ratpack
   */
  public static String getVersion() {
    ClassLoader classLoader = RatpackVersion.class.getClassLoader();
    final InputStream resourceAsStream = classLoader.getResourceAsStream("ratpack/ratpack-version.txt");
    ReadAction action = new ReadAction(resourceAsStream);
    try {
      action.execute(Unpooled.buffer());
    } catch (Exception e) {
      LOGGER.error("", e);
    }
    return action.content;
  }

  private static class ReadAction extends ReleasingAction<ByteBuf> {

    private final InputStream resourceAsStream;
    String content;

    public ReadAction(InputStream resourceAsStream) {
      this.resourceAsStream = resourceAsStream;
    }

    @Override
    protected void doExecute(ByteBuf buffer) {
      try {
        IoUtils.writeTo(resourceAsStream, buffer);
        content = buffer.toString(CharsetUtil.UTF_8);
      } catch (IOException e) {
        throw uncheck(e);
      }
    }
  }
}
