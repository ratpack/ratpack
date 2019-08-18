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
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.file.FileSystemBinding;
import ratpack.server.ServerConfig;
import ratpack.server.internal.ServerConfigData;
import ratpack.server.internal.ServerEnvironment;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class ServerConfigDataDeserializer extends JsonDeserializer<ServerConfigData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfigDataDeserializer.class);

  private final InetAddress address;
  private final int port;
  private final boolean development;
  private final URI publicAddress;
  private final Supplier<FileSystemBinding> baseDirSupplier;

  public ServerConfigDataDeserializer(InetAddress address, int port, boolean development, URI publicAddress, Supplier<FileSystemBinding> baseDirSupplier) {
    this.address = address;
    this.port = port;
    this.development = development;
    this.publicAddress = publicAddress;
    this.baseDirSupplier = baseDirSupplier;
  }

  @Override
  public ServerConfigData deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ObjectCodec codec = jp.getCodec();
    ObjectNode serverNode = jp.readValueAsTree();
    ServerConfigData data = new ServerConfigData(baseDirSupplier.get(), address, port, development, publicAddress);
    if (serverNode.hasNonNull("port")) {
      data.setPort(parsePort(serverNode.get("port")));
    }
    if (serverNode.hasNonNull("address")) {
      data.setAddress(toValue(codec, serverNode.get("address"), InetAddress.class));
    }
    if (serverNode.hasNonNull("idleTimeout")) {
      data.setIdleTimeout(toValue(codec, serverNode.get("idleTimeout"), Duration.class));
    }
    if (serverNode.hasNonNull("development")) {
      data.setDevelopment(serverNode.get("development").asBoolean(false));
    }
    if (serverNode.hasNonNull("threads")) {
      data.setThreads(serverNode.get("threads").asInt(ServerConfig.DEFAULT_THREADS));
    }
    if (serverNode.hasNonNull("registerShutdownHook")) {
      data.setRegisterShutdownHook(serverNode.get("registerShutdownHook").asBoolean(true));
    }
    if (serverNode.hasNonNull("publicAddress")) {
      data.setPublicAddress(toValue(codec, serverNode.get("publicAddress"), URI.class));
    }
    if (serverNode.hasNonNull("maxContentLength")) {
      data.setMaxContentLength(serverNode.get("maxContentLength").asInt(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH));
    }
    if (serverNode.hasNonNull("maxChunkSize")) {
      data.setMaxChunkSize(serverNode.get("maxChunkSize").asInt(ServerConfig.DEFAULT_MAX_CHUNK_SIZE));
    }
    if (serverNode.hasNonNull("maxInitialLineLength")) {
      data.setMaxInitialLineLength(serverNode.get("maxInitialLineLength").asInt(ServerConfig.DEFAULT_MAX_INITIAL_LINE_LENGTH));
    }
    if (serverNode.hasNonNull("maxHeaderSize")) {
      data.setMaxHeaderSize(serverNode.get("maxHeaderSize").asInt(ServerConfig.DEFAULT_MAX_HEADER_SIZE));
    }
    if (serverNode.hasNonNull("requireClientSslAuth")) {
      data.setRequireClientSslAuth(serverNode.get("requireClientSslAuth").asBoolean(false));
    }
    if (serverNode.hasNonNull("ssl")) {
      data.setSslContext(toValue(codec, serverNode.get("ssl"), SslContext.class));
    } else if (serverNode.hasNonNull("jdkSsl")) {
      data.setSslContext(toJdkSslContext(data, toValue(codec, serverNode.get("jdkSsl"), SSLContext.class)));
    }
    if (serverNode.hasNonNull("baseDir")) {
      throw new IllegalStateException("baseDir value cannot be set via config, it must be set directly via ServerConfigBuilder.baseDir()");
    }
    if (serverNode.hasNonNull("connectTimeoutMillis")) {
      parseOptionalIntValue("connectTimeoutMillis", serverNode.get("connectTimeoutMillis")).ifPresent(data::setConnectTimeoutMillis);
    }
    if (serverNode.hasNonNull("maxMessagesPerRead")) {
      parseOptionalIntValue("maxMessagesPerRead", serverNode.get("maxMessagesPerRead")).ifPresent(data::setMaxMessagesPerRead);
    }
    if (serverNode.hasNonNull("receiveBufferSize")) {
      parseOptionalIntValue("receiveBufferSize", serverNode.get("receiveBufferSize")).ifPresent(data::setReceiveBufferSize);
    }
    if (serverNode.hasNonNull("writeSpinCount")) {
      parseOptionalIntValue("writeSpinCount", serverNode.get("writeSpinCount")).ifPresent(data::setWriteSpinCount);
    }
    if (serverNode.hasNonNull("connectQueueSize")) {
      parseOptionalIntValue("connectQueueSize", serverNode.get("connectQueueSize")).ifPresent(data::setConnectQueueSize);
    }

    return data;
  }

  @SuppressWarnings("deprecation")
  private JdkSslContext toJdkSslContext(ServerConfigData data, SSLContext jdkSslContext) {
    return new JdkSslContext(jdkSslContext, false, data.isRequireClientSslAuth() ? ClientAuth.REQUIRE : ClientAuth.NONE);
  }

  private int parsePort(JsonNode node) {
    return node.isInt() ? node.asInt() : ServerEnvironment.parsePortValue("config", node.asText());
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

  public Optional<Integer> parseOptionalIntValue(String description, JsonNode node) {
    try {
      return Optional.of(Integer.parseInt(node.asText()));
    } catch (NumberFormatException e) {
      LOGGER.warn("Failed to parse {} value {} to int", description, node.asText());
      return Optional.empty();
    }
  }
}
