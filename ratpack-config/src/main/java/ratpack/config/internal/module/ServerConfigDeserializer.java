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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ratpack.config.internal.source.env.Environment;
import ratpack.server.ServerConfig;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Paths;

public class ServerConfigDeserializer extends JsonDeserializer<ServerConfig> {
  private final Environment environment;

  public ServerConfigDeserializer(Environment environment) {
    this.environment = environment;
  }

  @Override
  public ServerConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    String portEnv = Strings.emptyToNull(environment.getenv("PORT"));
    ObjectCodec codec = jp.getCodec();
    ObjectNode serverNode = jp.readValueAsTree();
    ServerConfig.Builder builder = builderForBasedir(serverNode, ctxt);
    if (serverNode.hasNonNull("port")) {
      builder.port(serverNode.get("port").asInt());
    } else if (portEnv != null) {
      builder.port(Integer.parseInt(portEnv));
    }
    if (serverNode.hasNonNull("address")) {
      builder.address(codec.treeToValue(serverNode.get("address"), InetAddress.class));
    }
    if (serverNode.hasNonNull("development")) {
      builder.development(serverNode.get("development").asBoolean());
    }
    if (serverNode.hasNonNull("threads")) {
      builder.threads(serverNode.get("threads").asInt());
    }
    if (serverNode.hasNonNull("publicAddress")) {
      builder.publicAddress(codec.treeToValue(serverNode.get("publicAddress"), URI.class));
    }
    if (serverNode.hasNonNull("maxContentLength")) {
      builder.maxContentLength(serverNode.get("maxContentLength").asInt());
    }
    if (serverNode.hasNonNull("timeResponses")) {
      builder.timeResponses(serverNode.get("timeResponses").asBoolean());
    }
    if (serverNode.hasNonNull("compressResponses")) {
      builder.compressResponses(serverNode.get("compressResponses").asBoolean());
    }
    if (serverNode.hasNonNull("compressionMinSize")) {
      builder.compressionMinSize(serverNode.get("compressionMinSize").asLong());
    }
    if (serverNode.hasNonNull("compressionMimeTypeWhiteList")) {
      builder.compressionWhiteListMimeTypes(toList(codec, serverNode.get("compressionMimeTypeWhiteList")));
    }
    if (serverNode.hasNonNull("compressionMimeTypeBlackList")) {
      builder.compressionBlackListMimeTypes(toList(codec, serverNode.get("compressionMimeTypeBlackList")));
    }
    if (serverNode.hasNonNull("indexFiles")) {
      builder.indexFiles(toList(codec, serverNode.get("indexFiles")));
    }
    if (serverNode.hasNonNull("ssl")) {
      builder.ssl(codec.treeToValue(serverNode.get("ssl"), SSLContext.class));
    }
    if (serverNode.hasNonNull("other")) {
      builder.other(toMap(codec, serverNode.get("other")));
    }
    return builder.build();
  }

  private static ServerConfig.Builder builderForBasedir(ObjectNode serverNode, DeserializationContext ctxt) throws IOException {
    JsonNode baseDirNode = serverNode.get("baseDir");
    if (baseDirNode == null) {
      return ServerConfig.noBaseDir();
    } else if (baseDirNode.isTextual()) {
      return ServerConfig.baseDir(Paths.get(baseDirNode.asText()));
    }
    throw ctxt.mappingException(ServerConfig.class, baseDirNode.asToken());
  }

  @SuppressWarnings("unchecked")
  private static ImmutableList<String> toList(ObjectCodec codec, JsonNode node) throws IOException {
    return codec.treeToValue(node, ImmutableList.class);
  }

  @SuppressWarnings("unchecked")
  private static ImmutableMap<String, String> toMap(ObjectCodec codec, JsonNode node) throws IOException {
    return codec.treeToValue(node, ImmutableMap.class);
  }
}
