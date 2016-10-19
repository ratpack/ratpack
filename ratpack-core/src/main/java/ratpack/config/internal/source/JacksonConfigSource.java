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
import ratpack.config.ConfigSource;
import ratpack.file.FileSystemBinding;
import ratpack.util.internal.Paths2;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public abstract class JacksonConfigSource implements ConfigSource {
  private final ByteSource byteSource;

  public JacksonConfigSource(Path path) {
    this(Paths2.asByteSource(path));
  }

  public JacksonConfigSource(URL url) {
    this(Resources.asByteSource(url));
  }

  public JacksonConfigSource(ByteSource byteSource) {
    this.byteSource = byteSource;
  }

  @Override
  public ObjectNode loadConfigData(ObjectMapper objectMapper, FileSystemBinding fileSystemBinding) throws Exception {
    try (InputStream inputStream = byteSource.openStream()) {
      try (JsonParser parser = getFactory(objectMapper).createParser(inputStream)) {
        return objectMapper.readTree(parser);
      }
    }
  }

  protected abstract JsonFactory getFactory(ObjectMapper objectMapper);
}
