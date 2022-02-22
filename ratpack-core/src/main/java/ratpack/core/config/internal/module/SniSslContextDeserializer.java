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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.google.common.base.CaseFormat;
import io.netty.handler.ssl.SslContext;
import io.netty.util.DomainWildcardMappingBuilder;
import io.netty.util.Mapping;

import java.io.IOException;
import java.util.Iterator;

public class SniSslContextDeserializer extends JsonDeserializer<Mapping<String, SslContext>> {


  @Override
  public Mapping<String, SslContext> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

    ObjectCodec codec = jp.getCodec();
    ObjectNode sslNode = jp.readValueAsTree();

    SslContext defaultContext;
    try {
      defaultContext = toValue(codec, sslNode, SslContext.class);
    } catch (IllegalStateException e) {
      throw new IllegalStateException("error with default ssl context: " + e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("error with default ssl context: " + e.getMessage(), e);
    } catch (IOException e) {
      throw new IOException("error with default ssl context: " + e.getMessage(), e);
    }
    if (defaultContext == null) {
      throw new IllegalStateException("default ssl context must be specified if any ssl properties are set");
    }
    DomainWildcardMappingBuilder<SslContext> builder = new DomainWildcardMappingBuilder<>(defaultContext);
    if (sslNode != null) {
      Iterator<String> iter = sslNode.fieldNames();
      while (iter.hasNext()) {
        String domain = iter.next();
        JsonNode domainNode = sslNode.get(domain);
        if (domainNode.isObject()) {
          try {
            SslContext domainContext = toValue(codec, domainNode, SslContext.class);
            builder.add(normalizeDomainName(domain), domainContext);
          } catch (IllegalStateException e) {
            throw new IllegalStateException("error with " + domain + " ssl context: " + e.getMessage(), e);
          } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("error with " + domain + " ssl context: " + e.getMessage(), e);
          } catch (IOException e) {
            throw new IOException("error with " + domain + " ssl context: " + e.getMessage(), e);
          }
        }
      }
    }
    return builder.build();
  }

  protected String normalizeDomainName(String domain) {
    boolean prependWildcard = domain.startsWith("_");
    String normalizedDomain = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(domain);
    normalizedDomain = normalizedDomain.replaceAll("_", ".");
    return prependWildcard ? "*" + normalizedDomain : normalizedDomain;
  }

  private static <T> T toValue(ObjectCodec codec, JsonNode node, Class<T> valueType) throws JsonProcessingException {
    if (node.isPojo()) {
      Object pojo = ((POJONode) node).getPojo();
      if (valueType.isInstance(pojo)) {
        return valueType.cast(pojo);
      }
    }
    return codec.treeToValue(node, valueType);
  }
}
