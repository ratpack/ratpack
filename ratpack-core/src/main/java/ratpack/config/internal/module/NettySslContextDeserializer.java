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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import ratpack.ssl.internal.SslContexts;
import ratpack.util.Exceptions;

import javax.net.ssl.KeyManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

public class NettySslContextDeserializer extends JsonDeserializer<SslContext> {

  @SuppressWarnings("Duplicates")
  @Override
  public SslContext deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ObjectNode node = jp.readValueAsTree();

    try {
      String keyStoreFile = node.path("keystoreFile").asText();
      String keyStorePassword = node.path("keystorePassword").asText();
      String trustStoreFile = node.path("truststoreFile").asText();
      String trustStorePassword = node.path("truststorePassword").asText();

      if (keyStoreFile.isEmpty()) {
        throw new IllegalStateException(
          "keystoreFile must be set if any ssl properties are set"
        );
      } else if (keyStorePassword.isEmpty()) {
        throw new IllegalStateException(
          "keystorePassword must be set if any ssl properties are set"
        );
      } else if (!trustStoreFile.isEmpty() && trustStorePassword.isEmpty()) {
        throw new IllegalStateException(
          "truststorePassword must be specified when truststoreFile is specified"
        );
      }

      KeyManagerFactory keyManagerFactory;
      try (InputStream is = Files.newInputStream(Paths.get(keyStoreFile))) {
        keyManagerFactory = SslContexts.keyManagerFactory(is, keyStorePassword.toCharArray());
      }

      SslContextBuilder builder = SslContextBuilder.forServer(keyManagerFactory);

      if (!trustStoreFile.isEmpty()) {
        try (InputStream is = Files.newInputStream(Paths.get(trustStoreFile))) {
          builder.trustManager(SslContexts.trustManagerFactory(is, trustStorePassword.toCharArray()));
        }
      }

      return builder.build();
    } catch (GeneralSecurityException ex) {
      throw Exceptions.uncheck(ex);
    }
  }
}
