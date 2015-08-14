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

package ratpack.config.internal.source;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import ratpack.config.ConfigDataBuilder;
import ratpack.config.ConfigSource;
import ratpack.util.internal.Paths2;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public abstract class JacksonConfigSource implements ConfigSource {
  private final DeferredByteSource deferredByteSource;

  public JacksonConfigSource(Path path) {
    this.deferredByteSource = builder -> {
      Path baseDir = builder.getBaseDir();
      Path resolvedPath = baseDir == null || path.isAbsolute() ? path : baseDir.resolve(path);
      return Paths2.asByteSource(resolvedPath);
    };
  }

  public JacksonConfigSource(URL url) {
    this.deferredByteSource = builder -> Resources.asByteSource(url);
  }

  public JacksonConfigSource(ByteSource byteSource) {
    this.deferredByteSource = builder -> byteSource;
  }

  @Override
  public ObjectNode loadConfigData(ConfigDataBuilder configDataBuilder) throws Exception {
    try (InputStream inputStream = deferredByteSource.resolve(configDataBuilder).openStream()) {
      ObjectMapper objectMapper = configDataBuilder.getObjectMapper();
      JsonParser parser = getFactory(objectMapper).createParser(inputStream);
      ObjectNode parsedNode = objectMapper.readTree(parser);
      parsedNode.remove("baseDir");
      return parsedNode;
    }
  }

  protected abstract JsonFactory getFactory(ObjectMapper objectMapper);

  private interface DeferredByteSource {
    ByteSource resolve(ConfigDataBuilder builder);
  }
}
