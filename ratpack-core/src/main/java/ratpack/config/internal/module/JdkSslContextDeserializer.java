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

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

public class JdkSslContextDeserializer extends JsonDeserializer<SSLContext> {
  @SuppressWarnings("Duplicates")
  @Override
  public SSLContext deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
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

      if (trustStoreFile.isEmpty()) {
        return SSLContexts.sslContext(Paths.get(keyStoreFile), keyStorePassword);
      } else {
        return SSLContexts.sslContext(Paths.get(keyStoreFile), keyStorePassword,
          Paths.get(trustStoreFile), trustStorePassword);
      }
    } catch (GeneralSecurityException ex) {
      throw Exceptions.uncheck(ex);
    }
  }
}
