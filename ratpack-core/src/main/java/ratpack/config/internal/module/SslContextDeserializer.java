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

package ratpack.config.internal.module;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ratpack.ssl.SSLContexts;
import ratpack.util.Exceptions;

import io.netty.handler.ssl.SslContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.file.Paths;

public class SSLContextDeserializer extends JsonDeserializer<SslContext> {
  @Override
  public SslContext deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ObjectNode node = jp.readValueAsTree();
    try {
      String certificateFile = node.path("certificate").asText();
      String keyFile = node.path("privateKey").asText();
      String keyPassword = node.path("privateKeyPassword").asText();
      return SSLContexts.create(Paths.get(certificateFile), Paths.get(keyFile), keyPassword);
    } catch (SSLException ex) {
      throw Exceptions.uncheck(ex);
    }
  }
}
