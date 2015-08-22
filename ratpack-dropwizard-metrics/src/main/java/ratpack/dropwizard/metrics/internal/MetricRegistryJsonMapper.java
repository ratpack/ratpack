/*
 * Copyright 2014 the original author or authors.
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

package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.*;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import ratpack.func.Function;

import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class MetricRegistryJsonMapper implements Function<MetricRegistry, ByteBuf> {

  private final ObjectMapper mapper;
  private final ByteBufAllocator byteBufAllocator;

  public MetricRegistryJsonMapper(ByteBufAllocator byteBufAllocator, MetricFilter filter) {
    this.byteBufAllocator = byteBufAllocator;
    this.mapper = new ObjectMapper().registerModule(
      new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false, filter));
  }

  @Override
  public ByteBuf apply(MetricRegistry metricRegistry) throws Exception {
    ByteBuf byteBuf = byteBufAllocator.ioBuffer();
    try {
      OutputStream out = new ByteBufOutputStream(byteBuf);
      mapper.writeValue(out, metricRegistry);
      return byteBuf;
    } catch (Exception e) {
      byteBuf.release();
      throw e;
    }
  }

}
