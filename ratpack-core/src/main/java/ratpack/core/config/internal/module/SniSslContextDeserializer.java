/*
 * Copyright 2022 the original author or authors.
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

package ratpack.core.config.internal.module;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.ssl.SslContext;
import io.netty.util.DomainWildcardMappingBuilder;
import io.netty.util.Mapping;

import java.io.IOException;
import java.util.Iterator;

public class SniSslContextDeserializer extends JsonDeserializer<Mapping<String, SslContext>> {

  private final NettySslContextDeserializer sslContextDeserializer = new NettySslContextDeserializer();

  @Override
  public Mapping<String, SslContext> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {

    SslContext defaultContext;
    try {
      defaultContext = sslContextDeserializer.deserialize(jp, ctxt);
    } catch (IllegalStateException e) {
      throw new IllegalStateException("error with default ssl context: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new IOException("error with default ssl context: " + e.getMessage(), e);
    }
    if (defaultContext == null) {
      throw new IllegalStateException("default ssl context must be specified if any ssl properties are set");
    }
    DomainWildcardMappingBuilder<SslContext> builder = new DomainWildcardMappingBuilder<>(defaultContext);
    ObjectNode node = jp.readValueAsTree();
    if (node != null) {
      Iterator<String> iter = node.fieldNames();
      while (iter.hasNext()) {
        String domain = iter.next();
        JsonNode domainNode = node.get(domain);
        if (domainNode.isObject()) {
          try {
            SslContext domainContext = sslContextDeserializer.deserialize(domainNode.traverse(jp.getCodec()), ctxt);
            builder.add(domain, domainContext);
          } catch (IllegalStateException e) {
            throw new IllegalStateException("error with " + domain + " ssl context: " + e.getMessage(), e);
          } catch (IOException e) {
            throw new IOException("error with " + domain + " ssl context: " + e.getMessage(), e);
          }
        }
      }
    }
    return builder.build();
  }
}
