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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import ratpack.server.ServerConfig;
import ratpack.server.internal.ServerConfigData;
import ratpack.server.internal.ServerEnvironment;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;

public class ServerConfigDataDeserializer extends JsonDeserializer<ServerConfigData> {
  private final ServerEnvironment serverEnvironment;

  public ServerConfigDataDeserializer(ServerEnvironment serverEnvironment) {
    this.serverEnvironment = serverEnvironment;
  }

  @Override
  public ServerConfigData deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ObjectCodec codec = jp.getCodec();
    ObjectNode serverNode = jp.readValueAsTree();
    ServerConfigData data = new ServerConfigData(serverEnvironment);
    if (serverNode.hasNonNull("port")) {
      data.setPort(parsePort(serverNode.get("port")));
    }
    if (serverNode.hasNonNull("address")) {
      data.setAddress(toValue(codec, serverNode.get("address"), InetAddress.class));
    }
    if (serverNode.hasNonNull("development")) {
      data.setDevelopment(serverNode.get("development").asBoolean(false));
    }
    if (serverNode.hasNonNull("threads")) {
      data.setThreads(serverNode.get("threads").asInt(ServerConfig.DEFAULT_THREADS));
    }
    if (serverNode.hasNonNull("publicAddress")) {
      data.setPublicAddress(toValue(codec, serverNode.get("publicAddress"), URI.class));
    }
    if (serverNode.hasNonNull("maxContentLength")) {
      data.setMaxContentLength(serverNode.get("maxContentLength").asInt(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH));
    }
    if (serverNode.hasNonNull("ssl")) {
      data.setSslContext(toValue(codec, serverNode.get("ssl"), SSLContext.class));
    }
    if (serverNode.hasNonNull("sslClientAuth")) {
      data.setSslClientAuth(serverNode.get("sslClientAuth").asBoolean(false));
    }
    if (serverNode.hasNonNull("baseDir")) {
      data.setBaseDir(toValue(codec, serverNode.get("baseDir"), Path.class));
    }
    return data;
  }

  private int parsePort(JsonNode node) {
    return node.isInt() ? node.asInt() : serverEnvironment.parsePortValue("config", node.asText());
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
