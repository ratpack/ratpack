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

import com.google.common.io.ByteSource;
import ratpack.util.Exceptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class ByteSourcePropertiesConfigSource extends AbstractPropertiesConfigSource {
  private final ByteSource byteSource;

  public ByteSourcePropertiesConfigSource(Optional<String> prefix, ByteSource byteSource) {
    super(prefix);
    this.byteSource = byteSource;
  }

  @Override
  protected Properties loadProperties() throws Exception {
    Properties properties = new Properties();
    try (InputStream inputStream = byteSource.openStream()) {
      properties.load(inputStream);
    } catch (IOException ex) {
      throw Exceptions.uncheck(ex);
    }
    return properties;
  }
}
