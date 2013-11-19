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

package ratpack.server.internal;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPromise;
import ratpack.handling.internal.ContextClose;
import ratpack.util.Action;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CloseEventHandler implements EventHandler<ContextClose> {

  private final static Logger LOGGER = Logger.getLogger(CloseEventHandler.class.getName());

  final private ChannelPromise closePromise;
  private ContextClose context;

  public CloseEventHandler(ChannelPromise closePromise) {
    this.closePromise = closePromise;
  }

  @Override
  public void notify(ContextClose context) {
    this.context = context;
    closePromise.setSuccess();
  }

  @Override
  public void addListener(final Action<? super ContextClose> callback) {
    closePromise.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) {
        try {
          callback.execute(context);
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Exception raised by callback: ", e);
        }
      }
    });
  }

}
