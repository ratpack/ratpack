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
import ratpack.server.ServerConfig;
import ratpack.server.internal.ServerEnvironment;
import ratpack.server.internal.DefaultServerConfigBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;

public class ServerConfigDeserializer extends JsonDeserializer<ServerConfig> {
  private final ServerEnvironment serverEnvironment;

  public ServerConfigDeserializer(ServerEnvironment serverEnvironment) {
    this.serverEnvironment = serverEnvironment;
  }

  @Override
  public ServerConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ObjectCodec codec = jp.getCodec();
    ObjectNode serverNode = jp.readValueAsTree();
    ServerConfig.Builder builder = builderForBasedir(serverNode, ctxt);
    if (serverNode.hasNonNull("port")) {
      builder.port(serverNode.get("port").asInt());
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
    if (serverNode.hasNonNull("ssl")) {
      builder.ssl(codec.treeToValue(serverNode.get("ssl"), SSLContext.class));
    }
    return builder.build();
  }

  private ServerConfig.Builder builderForBasedir(ObjectNode serverNode, DeserializationContext ctxt) throws IOException {
    JsonNode baseDirNode = serverNode.get("baseDir");
    if (baseDirNode != null) {
      if (baseDirNode.isTextual()) {
        return DefaultServerConfigBuilder.baseDir(serverEnvironment, Paths.get(baseDirNode.asText()));
      } else {
        throw ctxt.mappingException(ServerConfig.class, baseDirNode.asToken());
      }
    }
    JsonNode baseDirPropsNode = serverNode.get("baseDirProps");
    if (baseDirPropsNode != null) {
      if (baseDirPropsNode.isTextual()) {
        String propertiesPath = Optional.ofNullable(Strings.emptyToNull(baseDirPropsNode.asText())).orElse(ServerConfig.Builder.DEFAULT_PROPERTIES_FILE_NAME);
        return DefaultServerConfigBuilder.findBaseDirProps(serverEnvironment, propertiesPath);
      } else {
        throw ctxt.mappingException(ServerConfig.class, baseDirPropsNode.asToken());
      }
    }
    return DefaultServerConfigBuilder.noBaseDir(serverEnvironment);
  }

  @SuppressWarnings("unchecked")
  private static ImmutableList<String> toList(ObjectCodec codec, JsonNode node) throws IOException {
    return codec.treeToValue(node, ImmutableList.class);
  }
}
