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

package ratpack.server;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.func.Action;
import ratpack.func.Predicate;
import ratpack.launch.LaunchConfig;
import ratpack.launch.internal.DefaultServerConfig;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * Builder for ServerConfig objects.
 */
public class ServerConfigBuilder {

  public static final String DEFAULT_ENV_PREFIX = "RATPACK_";
  public static final String DEFAULT_PROP_PREFIX = "ratpack.";

  private FileSystemBinding baseDir;

  private int port = ServerConfig.DEFAULT_PORT;
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

  ServerConfigBuilder() {
  }

  ServerConfigBuilder(Path baseDir) {
    this.baseDir = new DefaultFileSystemBinding(baseDir);
  }

  /**
   * Initialize a ServerConfigBuilder from the legacy launch config.
   * @param launchConfig the launch config data to initialize the builder
   * @return A new server config builder
   */
  public static ServerConfigBuilder launchConfig(LaunchConfig launchConfig) {
    ServerConfigBuilder builder;
    if (launchConfig.isHasBaseDir()) {
      builder = ServerConfig.baseDir(launchConfig.getBaseDir().getFile());
    } else {
      builder = ServerConfig.noBaseDir();
    }
    builder.port(launchConfig.getPort());
    builder.address(launchConfig.getAddress());
    builder.development(launchConfig.isDevelopment());
    builder.threads(launchConfig.getThreads());
    builder.publicAddress(launchConfig.getPublicAddress());
    builder.maxContentLength(launchConfig.getMaxContentLength());
    builder.timeResponses(launchConfig.isTimeResponses());
    builder.compressResponses(launchConfig.isCompressResponses());
    builder.compressionMinSize(launchConfig.getCompressionMinSize());
    builder.compressionWhiteListMimeTypes(launchConfig.getCompressionMimeTypeWhiteList().asList());
    builder.compressionBlackListMimeTypes(launchConfig.getCompressionMimeTypeBlackList().asList());
    builder.indexFiles(launchConfig.getIndexFiles());
    builder.ssl(launchConfig.getSSLContext());
    builder.other(launchConfig.getOtherPrefixedWith(""));
    return builder;
  }

  public ServerConfigBuilder port(int port) {
    this.port = port;
    return this;
  }

  /**
   * Sets the address to bind to.
   * <p>
   * Default value is {@code null}.
   *
   * @param address The address to bind to
   * @return this
   * @see ServerConfig#getAddress()
   */
  public ServerConfigBuilder address(InetAddress address) {
    this.address = address;
    return this;
  }

  /**
   * Whether or not the application is "development".
   * <p>
   * Default value is {@code false}.
   *
   * @param development Whether or not the application is "development".
   * @return this
   * @see ServerConfig#isDevelopment()
   */
  public ServerConfigBuilder development(boolean development) {
    this.development = development;
    return this;
  }

  /**
   * The number of threads to use.
   * <p>
   * Defaults to {@link ServerConfig#DEFAULT_THREADS}
   *
   * @param threads the size of the event loop thread pool
   * @return this
   * @see ServerConfig#getThreads()
   */
  public ServerConfigBuilder threads(int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("'threads' must be > 0");
    }
    this.threads = threads;
    return this;
  }

  /**
   * The public address of the application.
   * <p>
   * Default value is {@code null}.
   *
   * @param publicAddress The public address of the application
   * @return this
   * @see ServerConfig#getPublicAddress()
   */
  public ServerConfigBuilder publicAddress(URI publicAddress) {
    this.publicAddress = publicAddress;
    return this;
  }

  /**
   * The max number of bytes a request body can be.
   *
   * Default value is {@code 1048576} (1 megabyte).
   *
   * @param maxContentLength The max content length to accept.
   * @return this
   * @see ServerConfig#getMaxContentLength()
   */
  public ServerConfigBuilder maxContentLength(int maxContentLength) {
    this.maxContentLength = maxContentLength;
    return this;
  }

  /**
   * Whether to time responses.
   *
   * Default value is {@code false}.
   *
   * @param timeResponses Whether to time responses
   * @return this
   * @see ServerConfig#isTimeResponses()
   */
  public ServerConfigBuilder timeResponses(boolean timeResponses) {
    this.timeResponses = timeResponses;
    return this;
  }

  /**
   * Whether to compress responses.
   *
   * Default value is {@code false}.
   *
   * @param compressResponses Whether to compress responses
   * @return this
   * @see ServerConfig#isCompressResponses()
   */
  public ServerConfigBuilder compressResponses(boolean compressResponses) {
    this.compressResponses = compressResponses;
    return this;
  }

  /**
   * The minimum size at which responses should be compressed, in bytes.
   *
   * @param compressionMinSize The minimum size at which responses should be compressed, in bytes
   * @return this
   * @see ServerConfig#getCompressionMinSize()
   */
  public ServerConfigBuilder compressionMinSize(long compressionMinSize) {
    this.compressionMinSize = compressionMinSize;
    return this;
  }

  /**
   * Adds the given values as compressible mime types.
   *
   * @param mimeTypes the compressible mime types.
   * @return this
   * @see ServerConfig#getCompressionMimeTypeWhiteList()
   */
  public ServerConfigBuilder compressionWhiteListMimeTypes(String... mimeTypes) {
    this.compressionMimeTypeWhiteList.add(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as compressible mime types.
   *
   * @param mimeTypes the compressible mime types.
   * @return this
   * @see ServerConfig#getCompressionMimeTypeWhiteList()
   */
  public ServerConfigBuilder compressionWhiteListMimeTypes(List<String> mimeTypes) {
    this.compressionMimeTypeWhiteList.addAll(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as non-compressible mime types.
   *
   * @param mimeTypes the non-compressible mime types.
   * @return this
   * @see ServerConfig#getCompressionMimeTypeBlackList()
   */
  public ServerConfigBuilder compressionBlackListMimeTypes(String... mimeTypes) {
    this.compressionMimeTypeBlackList.add(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as non-compressible mime types.
   *
   * @param mimeTypes the non-compressible mime types.
   * @return this
   * @see ServerConfig#getCompressionMimeTypeBlackList()
   */
  public ServerConfigBuilder compressionBlackListMimeTypes(List<String> mimeTypes) {
    this.compressionMimeTypeBlackList.addAll(mimeTypes);
    return this;
  }

  /**
   * Adds the given values as potential index file names.
   *
   * @param indexFiles the potential index file names.
   * @return this
   * @see ServerConfig#getIndexFiles()
   */
  public ServerConfigBuilder indexFiles(String... indexFiles) {
    this.indexFiles.add(indexFiles);
    return this;
  }

  /**
   * Adds the given values as potential index file names.
   *
   * @param indexFiles the potential index file names.
   * @return this
   * @see ServerConfig#getIndexFiles()
   */
  public ServerConfigBuilder indexFiles(List<String> indexFiles) {
    this.indexFiles.addAll(indexFiles);
    return this;
  }

  /**
   * The SSL context to use if the application serves content over HTTPS.
   *
   * @param sslContext the SSL context.
   * @return this
   * @see ratpack.ssl.SSLContexts
   * @see ServerConfig#getSSLContext()
   */
  public ServerConfigBuilder ssl(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  /**
   * Add an "other" property.
   *
   * @param key The key of the property
   * @param value The value of the property
   * @return this
   * @see ServerConfig#getOther(String, String)
   */
  public ServerConfigBuilder other(String key, String value) {
    other.put(key, value);
    return this;
  }

  /**
   * Add some "other" properties.
   *
   * @param other A map of properties to add to the launch config other properties
   * @return this
   * @see ServerConfig#getOther(String, String)
   */
  public ServerConfigBuilder other(Map<String, String> other) {
    for (Map.Entry<String, String> entry : other.entrySet()) {
      other(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public ServerConfig build() {
    return new DefaultServerConfig(baseDir, port, address, development, threads,
      publicAddress, indexFiles.build(), other.build(), sslContext, maxContentLength,
      timeResponses, compressResponses, compressionMinSize,
      compressionMimeTypeWhiteList.build(), compressionMimeTypeBlackList.build());
  }

  /**
   * Adds a configuration source for environment variables starting with the prefix {@value ServerConfigBuilder#DEFAULT_ENV_PREFIX}.
   *
   * @return this
   */
  ServerConfigBuilder env() {
    return env(DEFAULT_ENV_PREFIX);
  }

  /**
   * Adds a configuration source for environment variables starting with the specified prefix.
   *
   * @param prefix the prefix which should be used to identify relevant environment variables;
   * the prefix will be removed before loading the data
   * @return this
   */
  ServerConfigBuilder env(String prefix) {
    return env(prefix, System.getenv());
  }

  private ServerConfigBuilder env(String prefix, Map<String, String> envvars) {
    Map<String, String> filteredEnvVars = envvars.entrySet().stream()
      .filter(entry -> entry.getKey().startsWith(prefix))
      .collect(Collectors.toMap(
        entry -> CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey().replace(prefix, "")),
        Map.Entry::getValue));
    Properties properties = new Properties();
    properties.putAll(filteredEnvVars);
    return props(properties);
  }

  /**
   * Adds a configuration source for a properties file.
   *
   * @param byteSource the source of the properties data
   * @return this
   */
  ServerConfigBuilder props(ByteSource byteSource) {
    Properties properties = new Properties();
    try (InputStream is = byteSource.openStream()) {
      properties.load(is);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return props(properties);
  }

  /**
   * Adds a configuration source for a properties file.
   *
   * @param path the source of the properties data
   * @return this
   */
  ServerConfigBuilder props(Path path) {
    Properties properties = new Properties();
    try (InputStream is = Files.newInputStream(path)) {
      properties.load(is);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return props(properties);
  }

  /**
   * Adds a configuration source for a properties object.
   *
   * @param properties the properties object
   * @return this
   */
  ServerConfigBuilder props(Properties properties) {
    Map<String, BuilderAction<?>> propertyCoercions = createPropertyCoercions();
    properties.entrySet().forEach(entry -> {
      String key = entry.getKey().toString();
      String value = entry.getValue().toString();
      BuilderAction<?> mapping = propertyCoercions.get(key);
      try {
        mapping.apply(value);
      } catch (Exception e) {
        throw uncheck(e);
      }
    });
    return this;
  }

  /**
   * Adds a configuration source for a properties file.
   *
   * @param url the source of the properties data
   * @return this
   */
  ServerConfigBuilder props(URL url) {
    //TODO-JOHN add test
    Properties properties = new Properties();
    try (InputStream is = url.openStream()) {
      properties.load(is);
    } catch (IOException e) {
      throw uncheck(e);
    }
    return props(properties);
  }

  /**
   * Adds a configuration source for system properties starting with the prefix {@value ServerConfigBuilder#DEFAULT_PROP_PREFIX}
   *
   * @return this
   */
  ServerConfigBuilder sysProps() {
    return sysProps(DEFAULT_PROP_PREFIX);
  }

  /**
   * Adds a configuration source for system properties starting with the specified prefix.
   *
   * @param prefix the prefix which should be used to identify relevant system properties;
   * the prefix will be removed before loading the data
   * @return this
   */
  ServerConfigBuilder sysProps(String prefix) {
    Map<String, String> filteredProperties = filter(
      System.getProperties().entrySet(),
      entry -> entry.getKey().toString().startsWith(prefix)
    ).collect(
      Collectors.toMap(
        p -> p.getKey().toString().replace(prefix, ""),
        p -> p.getValue().toString()
      )
    );
    Properties properties = new Properties();
    properties.putAll(filteredProperties);
    return props(properties);
  }

  private <E> Stream<E> filter(Collection<E> collection, Predicate<E> predicate) {
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
      .put("port", new BuilderAction<>(Integer::parseInt, ServerConfigBuilder.this::port))
      .put("address", new BuilderAction<>(ServerConfigBuilder::inetAddress, ServerConfigBuilder.this::address))
      .put("development", new BuilderAction<>(Boolean::parseBoolean, ServerConfigBuilder.this::development))
      .put("threads", new BuilderAction<>(Integer::parseInt, ServerConfigBuilder.this::threads))
      .put("publicAddress", new BuilderAction<>(URI::create, ServerConfigBuilder.this::publicAddress))
      .put("maxContentLength", new BuilderAction<>(Integer::parseInt, ServerConfigBuilder.this::maxContentLength))
      .put("timeResponses", new BuilderAction<>(Boolean::parseBoolean, ServerConfigBuilder.this::timeResponses))
      .put("compressResponses", new BuilderAction<>(Boolean::parseBoolean, ServerConfigBuilder.this::compressResponses))
      .put("compressionMinSize", new BuilderAction<>(Long::parseLong, ServerConfigBuilder.this::compressionMinSize))
      .put("compressionWhiteListMimeTypes", new BuilderAction<>(ServerConfigBuilder::split, ServerConfigBuilder.this::compressionWhiteListMimeTypes))
      .put("compressionBlackListMimeTypes", new BuilderAction<>(ServerConfigBuilder::split, ServerConfigBuilder.this::compressionBlackListMimeTypes))
      .put("indexFiles", new BuilderAction<>(ServerConfigBuilder::split, ServerConfigBuilder.this::indexFiles))
        //TODO-JOHN add support for SSLContext somehow
      .build();
  }
}
