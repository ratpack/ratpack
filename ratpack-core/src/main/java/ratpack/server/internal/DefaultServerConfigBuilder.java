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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import ratpack.file.FileSystemBinding;
import ratpack.file.internal.DefaultFileSystemBinding;
import ratpack.func.Action;
import ratpack.func.Predicate;
import ratpack.server.ServerConfig;
import ratpack.server.ServerEnvironment;
import ratpack.ssl.SSLContexts;
import ratpack.util.internal.Paths2;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
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
  private SSLContext sslContext;
  private int maxContentLength = ServerConfig.DEFAULT_MAX_CONTENT_LENGTH;
  private boolean timeResponses;

  //Variables to support configuring SSL
  private InputStream sslKeystore;
  private String sslKeystorePassword = "";

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
  public ServerConfig.Builder ssl(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  @Override
  public ServerConfig build() {
    loadSSLIfConfigured();
    return new DefaultServerConfig(baseDir, port, address, development, threads,
      publicAddress, sslContext, maxContentLength, timeResponses);
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
    return props(Paths2.asByteSource(path));
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

  private ServerConfig.Builder sslKeystore(InputStream is) {
    sslKeystore = is;
    return this;
  }

  private ServerConfig.Builder sslKeystorePassword(String password) {
    this.sslKeystorePassword = password;
    return this;
  }

  private void loadSSLIfConfigured() {
    if (sslKeystore != null) {
      try (InputStream stream = sslKeystore) {
        this.ssl(SSLContexts.sslContext(stream, sslKeystorePassword));
      } catch (IOException | GeneralSecurityException e) {
        throw uncheck(e);
      }
    }
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

  /**
   * Gets a property value as an InputStream. The property value can be any of:
   * <ul>
   *   <li>An absolute file path to a file that exists.</li>
   *   <li>A valid URI.</li>
   *   <li>A classpath resource path loaded via the ClassLoader passed to the constructor.</li>
   * </ul>
   *
   * @param path the path to the resource
   * @return an InputStream or <code>null</code> if the property does not exist.
   */
  private static InputStream asStream(String path) {
    try {
      InputStream stream = null;
      if (path != null) {
        // try to treat it as a File path
        File file = new File(path);
        if (file.isFile()) {
          stream = new FileInputStream(file);
        } else {
          // try to treat it as a URL
          try {
            URL url = new URL(path);
            stream = url.openStream();
          } catch (MalformedURLException e) {
            // try to treat it as a resource path
            stream = DefaultServerConfigBuilder.class.getClassLoader().getResourceAsStream(path);
            if (stream == null) {
              throw new FileNotFoundException(path);
            }
          }
        }
      }
      return stream;
    } catch (IOException e) {
      throw uncheck(e);
    }
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
      .put("sslKeystoreFile", new BuilderAction<>(DefaultServerConfigBuilder::asStream, DefaultServerConfigBuilder.this::sslKeystore))
      .put("sslKeystorePassword", new BuilderAction<>(Function.identity(), DefaultServerConfigBuilder.this::sslKeystorePassword))
      .build();
  }
}
