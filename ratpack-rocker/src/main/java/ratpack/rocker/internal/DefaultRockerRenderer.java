/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rocker.internal;

import com.fizzed.rocker.ContentType;
import com.fizzed.rocker.RockerModel;
import com.fizzed.rocker.runtime.ArrayOfByteArraysOutput;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.AsciiString;
import ratpack.handling.Context;
import ratpack.render.RendererException;
import ratpack.render.RendererSupport;
import ratpack.rocker.RockerRenderer;

import java.util.List;

@Singleton
public final class DefaultRockerRenderer extends RendererSupport<RockerModel> implements RockerRenderer {

  private static final AsciiString HTML = AsciiString.of("text/html;charset=UTF-8");
  private static final AsciiString TEXT = AsciiString.of("text/plain;charset=UTF-8");

  public static final RockerRenderer INSTANCE = new DefaultRockerRenderer();

  @Override
  public void render(Context context, RockerModel rockerModel) throws Exception {
    try {
      ArrayOfByteArraysOutput output = rockerModel.render(ArrayOfByteArraysOutput.FACTORY);

      List<byte[]> arrays = output.getArrays();
      ByteBuf byteBuf;
      int size = arrays.size();
      if (size == 0) {
        byteBuf = Unpooled.EMPTY_BUFFER;
      } else if (size == 1) {
        byteBuf = Unpooled.wrappedBuffer(arrays.get(0));
      } else {
        byteBuf = new CompositeByteBuf(
          UnpooledByteBufAllocator.DEFAULT,
          false,
          size,
          Iterables.transform(arrays, Unpooled::wrappedBuffer)
        );
      }

      AsciiString contentType = output.getContentType() == ContentType.HTML ? HTML : TEXT;
      context.getResponse()
        .contentTypeIfNotSet(contentType)
        .send(byteBuf);

    } catch (Exception e) {
      // https://github.com/fizzed/rocker/issues/30
      // Ratpack will try to toString() the rockerModel object, to create an exception message for the RenderException
      // This will obscure the underlying exception. Log here so we can actually see it.
      // This can be removed when the above issue is rectified.
      throw new RendererException("Error rendering template " + rockerModel.getClass().getName(), e);
    }
  }

}
