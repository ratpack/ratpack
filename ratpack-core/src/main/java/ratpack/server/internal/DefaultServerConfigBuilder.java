/*
 * Copyright 2014 the original author or authors.
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

package ratpack.server.internal;

import com.google.common.base.CaseFormat;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.func.Action;
import ratpack.func.Predicate;
import ratpack.launch.internal.DefaultServerConfig;
import ratpack.server.ServerConfig;
import ratpack.server.ServerEnvironment;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultServerConfigBuilder implements ServerConfig.Builder {

  private ServerEnvironment serverEnvironment;
  private FileSystemBinding baseDir;

  private int port;
  private InetAddress address;
  private boolean development;
  private int threads = ServerConfig.DEFAULT_THREADS;
  private URI publicAddress;
  private ImmutableList.Builder<String> indexFiles = ImmutableList.builder();
  private ImmutableMap.Builder<String, String> other = ImmutableMap.builder();
  private SSLContext sslContext;
  private int maxContentLength = ServerConfig.DEFAULT_MAX_CONTENT_LENGTH;
  private boolean timeResponses;
  private boolean compressResponses;
  private long compressionMinSize = ServerConfig.DEFAULT_COMPRESSION_MIN_SIZE;
  private final ImmutableSet.Builder<String> compressionMimeTypeWhiteList = ImmutableSet.builder();
  private final ImmutableSet.Builder<String> compressionMimeTypeBlackList = ImmutableSet.builder();

  private DefaultServerConfigBuilder(ServerEnvironment serverEnvironment, Optional<Path> baseDir) {
    if (baseDir.isPresent()) {
      this.baseDir = new DefaultFileSystemBinding(baseDir.get());
    }
    this.serverEnvironment = serverEnvironment;
    this.port = serverEnvironment.getPort();
    this.development = serverEnvironment.isDevelopment();
    this.publicAddress = serverEnvironment.getPublicAddress();
  }

  public static ServerConfig.Builder noBaseDir(ServerEnvironment serverEnvironment) {
    return new DefaultServerConfigBuilder(serverEnvironment, Optional.empty());
  }

  public static ServerConfig.Builder baseDir(ServerEnvironment serverEnvironment, Path baseDir) {
    return new DefaultServerConfigBuilder(serverEnvironment, Optional.of(baseDir.toAbsolutePath().normalize()));
  }

  public static ServerConfig.Builder findBaseDirProps(ServerEnvironment serverEnvironment, String propertiesPath) {
    String workingDir = StandardSystemProperty.USER_DIR.value();
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    BaseDirFinder.Result result = BaseDirFinder.find(workingDir, classLoader, propertiesPath)
      .orElseThrow(() -> new IllegalStateException("Could not find properties file '" + propertiesPath + "' in working dir '" + workingDir + "' or context class loader classpath"));
    return baseDir(serverEnvironment, result.getBaseDir()).props(result.getResource());
  }

  @Override
  public ServerConfig.Builder port(int port) {
    this.port = port;
    return this;
  }

  @Override
  public ServerConfig.Builder address(InetAddress address) {
    this.address = address;
    return this;
  }

  @Override
  public ServerConfig.Builder development(boolean development) {
    this.development = development;
    return this;
  }

  @Override
  public ServerConfig.Builder threads(int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("'threads' must be > 0");
    }
    this.threads = threads;
    return this;
  }

  @Override
  public ServerConfig.Builder publicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
    return this;
  }

  @Override
  public ServerConfig.Builder maxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
    return this;
  }

  @Override
  public ServerConfig.Builder timeResponses(boolean timeResponses) {
    this.timeResponses = timeResponses;
    return this;
  }

  @Override
  public ServerConfig.Builder compressResponses(boolean compressResponses) {
    this.compressResponses = compressResponses;
    return this;
  }

  @Override
  public ServerConfig.Builder compressionMinSize(long compressionMinSize) {
    this.compressionMinSize = compressionMinSize;
    return this;
  }

  @Override
  public ServerConfig.Builder compressionWhiteListMimeTypes(String... mimeTypes) {
    this.compressionMimeTypeWhiteList.add(mimeTypes);
    return this;
  }

  @Override
  public ServerConfig.Builder compressionWhiteListMimeTypes(List<String> mimeTypes) {
    this.compressionMimeTypeWhiteList.addAll(mimeTypes);
    return this;
  }

  @Override
  public ServerConfig.Builder compressionBlackListMimeTypes(String... mimeTypes) {
    this.compressionMimeTypeBlackList.add(mimeTypes);
    return this;
  }

  @Override
  public ServerConfig.Builder compressionBlackListMimeTypes(List<String> mimeTypes) {
    this.compressionMimeTypeBlackList.addAll(mimeTypes);
    return this;
  }

  @Override
  public ServerConfig.Builder indexFiles(String... indexFiles) {
    this.indexFiles.add(indexFiles);
    return this;
  }

  @Override
  public ServerConfig.Builder indexFiles(List<String> indexFiles) {
    this.indexFiles.addAll(indexFiles);
    return this;
  }

  @Override
  public ServerConfig.Builder ssl(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  @Override
  public ServerConfig.Builder other(String key, String value) {
    other.put(key, value);
    return this;
  }

  @Override
  public ServerConfig.Builder other(Map<String, String> other) {
    for (Map.Entry<String, String> entry : other.entrySet()) {
      other(entry.getKey(), entry.getValue());
    }
    return this;
  }

  @Override
  public ServerConfig build() {
    return new DefaultServerConfig(baseDir, port, address, development, threads,
      publicAddress, indexFiles.build(), other.build(), sslContext, maxContentLength,
      timeResponses, compressResponses, compressionMinSize,
      compressionMimeTypeWhiteList.build(), compressionMimeTypeBlackList.build());
  }

  @Override
  public ServerConfig.Builder env() {
    return env(DEFAULT_ENV_PREFIX);
  }

  @Override
  public ServerConfig.Builder env(String prefix) {
    Map<String, String> filteredEnvVars = serverEnvironment.getenv().entrySet().stream()
      .filter(entry -> entry.getKey().startsWith(prefix))
      .collect(Collectors.toMap(
        entry -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey().replace(prefix, "")),
        Map.Entry::getValue));
    return props(filteredEnvVars);
  }

  @Override
  public ServerConfig.Builder props(ByteSource byteSource) {
    Properties properties = new Properties();
    try (InputStream is = byteSource.openStream()) {
      properties.load(is);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return props(properties);
  }

  @Override
  public ServerConfig.Builder props(String path) {
    return props(Paths.get(path));
  }

  @Override
  public ServerConfig.Builder props(Path path) {
    return props(Files.asByteSource(path.toFile()));
  }

  @Override
  public ServerConfig.Builder props(Map<String, String> map) {
    Map<String, BuilderAction<?>> propertyCoercions = createPropertyCoercions();
    map.entrySet().forEach(entry -> {
      BuilderAction<?> mapping = propertyCoercions.get(entry.getKey());
      if (mapping != null) {
        try {
          mapping.apply(entry.getValue());
        } catch (Exception e) {
          throw uncheck(e);
        }
      }
    });
    return this;
  }

  @Override
  public ServerConfig.Builder props(Properties properties) {
    Map<String, String> map = Maps.newHashMapWithExpectedSize(properties.size());
    properties.entrySet().forEach(e -> map.put(e.getKey().toString(), e.getValue().toString()));
    return props(map);
  }

  @Override
  public ServerConfig.Builder props(URL url) {
    return props(Resources.asByteSource(url));
  }

  @Override
  public ServerConfig.Builder sysProps() {
    return sysProps(DEFAULT_PROP_PREFIX);
  }

  @Override
  public ServerConfig.Builder sysProps(String prefix) {
    Map<String, String> filteredProperties = filter(
      serverEnvironment.getProperties().entrySet(),
      entry -> entry.getKey().toString().startsWith(prefix)
    ).collect(
      Collectors.toMap(
        p -> p.getKey().toString().replace(prefix, ""),
        p -> p.getValue().toString()
      )
    );
    return props(filteredProperties);
  }

  private static <E> Stream<E> filter(Collection<E> collection, Predicate<E> predicate) {
    return collection.stream().filter(predicate.toPredicate());
  }

  private static class BuilderAction<T> {

    private final Function<String, T> converter;

    private final Action<T> action;

    public BuilderAction(Function<String, T> converter, Action<T> action) {
      this.converter = converter;
      this.action = action;
    }

    public void apply(String value) throws Exception {
      action.execute(converter.apply(value));
    }

  }

  private static String[] split(String s) {
    return Arrays.stream(s.split(",")).map(String::trim).toArray(String[]::new);
  }

  private static InetAddress inetAddress(String s) {
    return uncheck(() -> InetAddress.getByName(s));
  }

  private Map<String, BuilderAction<?>> createPropertyCoercions() {
    return ImmutableMap.<String, BuilderAction<?>>builder()
      .put("port", new BuilderAction<>(Integer::parseInt, DefaultServerConfigBuilder.this::port))
      .put("address", new BuilderAction<>(DefaultServerConfigBuilder::inetAddress, DefaultServerConfigBuilder.this::address))
      .put("development", new BuilderAction<>(Boolean::parseBoolean, DefaultServerConfigBuilder.this::development))
      .put("threads", new BuilderAction<>(Integer::parseInt, DefaultServerConfigBuilder.this::threads))
      .put("publicAddress", new BuilderAction<>(URI::create, DefaultServerConfigBuilder.this::publicAddress))
      .put("maxContentLength", new BuilderAction<>(Integer::parseInt, DefaultServerConfigBuilder.this::maxContentLength))
      .put("timeResponses", new BuilderAction<>(Boolean::parseBoolean, DefaultServerConfigBuilder.this::timeResponses))
      .put("compressResponses", new BuilderAction<>(Boolean::parseBoolean, DefaultServerConfigBuilder.this::compressResponses))
      .put("compressionMinSize", new BuilderAction<>(Long::parseLong, DefaultServerConfigBuilder.this::compressionMinSize))
      .put("compressionWhiteListMimeTypes", new BuilderAction<>(DefaultServerConfigBuilder::split, DefaultServerConfigBuilder.this::compressionWhiteListMimeTypes))
      .put("compressionBlackListMimeTypes", new BuilderAction<>(DefaultServerConfigBuilder::split, DefaultServerConfigBuilder.this::compressionBlackListMimeTypes))
      .put("indexFiles", new BuilderAction<>(DefaultServerConfigBuilder::split, DefaultServerConfigBuilder.this::indexFiles))
        //TODO-JOHN add support for SSLContext somehow
      .build();
  }
}
