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

package ratpack.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteSource;
import ratpack.config.ConfigData;
import ratpack.config.ConfigDataBuilder;
import ratpack.config.ConfigSource;
import ratpack.config.EnvironmentParser;
import ratpack.func.Action;
import ratpack.func.Function;

import io.netty.handler.ssl.SslContext;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Builds a {@link ServerConfig}.
 *
 * @see RatpackServerSpec#serverConfig(Action)
 */
public interface ServerConfigBuilder extends ConfigDataBuilder {

  /**
   * Sets the root of the filesystem for the application.
   * <p>
   * The {@link ServerConfig#getBaseDir() base dir} acts as the portable file system for the application.
   * All paths within the application, resolved by Ratpack API are resolved from this point.
   * For example, {@link ratpack.handling.Chain#files(Action)} allows serving static files within the base dir.
   * <p>
   * The base dir is also used to resolve paths to file system locations when using {@link #json(String)}, {@link #yaml(String)} and {@link #props(String)}.
   * This allows config files to travel with the application within the base dir.
   * <p>
   * It is generally desirable to use the {@link BaseDir#find()} to dynamically find the base dir at runtime.
   *
   * @param baseDir the base dir
   * @return {@code this}
   */
  ServerConfigBuilder baseDir(Path baseDir);

  /**
   * Calls {@link #baseDir(Path)} after converting the given {@code File} to a {@code Path} using {@link File#toPath()}.
   *
   * @param file the base dir
   * @return {@code this}
   */
  default ServerConfigBuilder baseDir(File file) {
    return baseDir(file.toPath());
  }

  /**
   * Sets the port to listen for requests on.
   * <p>
   * Defaults to 5050.
   *
   * @param port the port to listen for requests on
   * @return {@code this}
   * @see ratpack.server.ServerConfig#getPort()
   */
  ServerConfigBuilder port(int port);

  /**
   * Sets the address to bind to.
   * <p>
   * Default value is {@code null}.
   *
   * @param address The address to bind to
   * @return {@code this}
   * @see ServerConfig#getAddress()
   */
  ServerConfigBuilder address(InetAddress address);

  /**
   * Whether or not the application is "development".
   * <p>
   * Default value is {@code false}.
   *
   * @param development Whether or not the application is "development".
   * @return {@code this}
   * @see ServerConfig#isDevelopment()
   */
  ServerConfigBuilder development(boolean development);

  /**
   * The number of threads to use.
   * <p>
   * Defaults to {@link ServerConfig#DEFAULT_THREADS}
   *
   * @param threads the size of the event loop thread pool
   * @return {@code this}
   * @see ServerConfig#getThreads()
   */
  ServerConfigBuilder threads(int threads);

  /**
   * The public address of the application.
   * <p>
   * Default value is {@code null}.
   *
   * @param publicAddress the public address of the application
   * @return {@code this}
   * @see ServerConfig#getPublicAddress()
   */
  ServerConfigBuilder publicAddress(URI publicAddress);

  /**
   * The max number of bytes a request body can be.
   *
   * Default value is {@code 1048576} (1 megabyte).
   *
   * @param maxContentLength the max content length to accept
   * @return {@code this}
   * @see ServerConfig#getMaxContentLength()
   */
  ServerConfigBuilder maxContentLength(int maxContentLength);

  /**
   * The connect timeout of the channel.
   *
   * @param connectTimeoutMillis the connect timeout in milliseconds
   * @return {@code this}
   * @see ServerConfig#getConnectTimeoutMillis()
   */
  ServerConfigBuilder connectTimeoutMillis(int connectTimeoutMillis);

  /**
   * The maximum number of messages to read per read loop.
   *
   * @param maxMessagesPerRead the max messages per read
   * @return {@code this}
   * @see ServerConfig#getMaxMessagesPerRead()
   */
  ServerConfigBuilder maxMessagesPerRead(int maxMessagesPerRead);

  /**
   * The <a href="http://docs.oracle.com/javase/7/docs/api/java/net/StandardSocketOptions.html?is-external=true#SO_RCVBUF" target="_blank">StandardSocketOptions.SO_RCVBUF</a> option.
   *
   * @param receiveBufferSize the recieve buffer size
   * @return {@code this}
   * @see ServerConfig#getReceiveBufferSize()
   */
  ServerConfigBuilder receiveBufferSize(int receiveBufferSize);

  /**
   * The maximum loop count for a write operation until <a href="http://docs.oracle.com/javase/7/docs/api/java/nio/channels/WritableByteChannel.html?is-external=true#write(java.nio.ByteBuffer)" target="_blank">WritableByteChannel.write(ByteBuffer)</a> returns a non-zero value.
   *
   * @param writeSpinCount the write spin count
   * @return {@code this}
   * @see ServerConfig#getWriteSpinCount()
   */
  ServerConfigBuilder writeSpinCount(int writeSpinCount);

  /**
   * The SSL context to use if the application serves content over HTTPS.
   *
   * @param sslContext the SSL context
   * @return {@code this}
   * @see ratpack.ssl.SSLContexts
   * @see ServerConfig#getSslContext()
   */
  ServerConfigBuilder ssl(SslContext sslContext);

  /**
   * The server needs client SSL authentication.
   *
   * @param requireClientSslAuth whether or not server needs client SSL authentication
   * @return {@code this}
   */
  ServerConfigBuilder requireClientSslAuth(boolean requireClientSslAuth);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder env();

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder env(String prefix);

  /**
   * Adds the given args as a config source.
   * <p>
   * This method is designed to be used with the args var of the {@code static main(String... args)} application entry point.
   * This allows configuration parameters to be passed to the application on the command line.
   * <p>
   * Each arg should be of the format {@code «key»=«value»}.
   * For the following example, the application has been started with the argument {@code thing.name=foo}.
   *
   * <pre class="java-args">{@code
   * import ratpack.test.embed.EmbeddedApp;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   static class Thing {
   *     public String name;
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(a -> a
   *         .serverConfig(s -> s
   *             .args(args)
   *             .require("/thing", Thing.class)
   *         )
   *         .handlers(c -> c
   *             .get(ctx -> ctx.render(ctx.get(Thing.class).name))
   *         )
   *     ).test(httpClient ->
   *         assertEquals(httpClient.getText(), "foo")
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param args the argument values
   * @return {@code this}
   * @since 1.1.0
   */
  @Override
  ServerConfigBuilder args(String[] args);

  /**
   * Invokes {@link #args(String, String, String[])}, with no prefix.
   *
   * @param separator the separator of the key and value in each arg
   * @param args the argument values
   * @return {@code this}
   * @since 1.1.0
   * @see #args(String[])
   */
  @Override
  ServerConfigBuilder args(String separator, String[] args);

  /**
   * Adds a configuration source for the given string args.
   * <p>
   * Args that do not start with the given {@code prefix} are ignored.
   * The remaining are each split using the given {@code separator} (as a literal string, not as a regex),
   * then trimmed of the prefix.
   *
   * @param prefix the prefix that each arg must have to be considered (use {@code null} or {@code ""} for no prefix)
   * @param separator the separator between the key and the value
   * @param args the argument values
   * @return {@code this}
   * @since 1.1.0
   * @see #args(String[])
   */
  @Override
  ServerConfigBuilder args(String prefix, String separator, String[] args);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder props(ByteSource byteSource);

  /**
   * Adds the properties file at the given path as a configuration source.
   * <p>
   * If a base dir is set, the path will be resolved relative to it.
   * Otherwise, it will be resolved relative to the file system root.
   *
   * @param path the path to the file
   * @return {@code this}
   */
  @Override
  ServerConfigBuilder props(String path);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder props(Path path);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder props(Properties properties);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder props(Map<String, String> map);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder props(URL url);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder sysProps();

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder sysProps(String prefix);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder onError(Action<? super Throwable> errorHandler);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder configureObjectMapper(Action<ObjectMapper> action);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder add(ConfigSource configSource);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder env(String prefix, Function<String, String> mapFunc);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder env(EnvironmentParser environmentParser);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder json(ByteSource byteSource);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder json(Path path);

  /**
   * Adds the JSON file at the given path as a configuration source.
   * <p>
   * If a base dir is set, the path will be resolved relative to it.
   * Otherwise, it will be resolved relative to the file system root.
   *
   * @param path the path to the file
   * @return {@code this}
   */
  @Override
  ServerConfigBuilder json(String path);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder json(URL url);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder yaml(ByteSource byteSource);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder yaml(Path path);

  /**
   * Adds the YAML file at the given path as a configuration source.
   * <p>
   * If a base dir is set, the path will be resolved relative to it.
   * Otherwise, it will be resolved relative to the file system root.
   *
   * @param path the path to the file
   * @return {@code this}
   */
  @Override
  ServerConfigBuilder yaml(String path);

  /**
   * {@inheritDoc}
   */
  @Override
  ServerConfigBuilder yaml(URL url);

  /**
   * Declares that it is required that the server config provide an object of the given type at the given path.
   * <p>
   * The {@link #build()} method will fail if the config is not able to provide the requested object.
   * <p>
   * All objects declared using this method will also automatically be implicitly added to the base registry.
   * <p>
   * The {@code pointer} argument is of the same format given to the {@link ConfigData#get(String, Class)} method.
   * <pre class="java">{@code
   * import org.junit.Assert;
   * import ratpack.test.embed.EmbeddedApp;
   *
   * import java.util.Collections;
   *
   * public class Example {
   *   static class MyConfig {
   *     public String value;
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     EmbeddedApp.of(a -> a
   *         .serverConfig(s -> s
   *             .props(Collections.singletonMap("config.value", "foo"))
   *             .require("/config", MyConfig.class)
   *         )
   *         .handlers(c -> c
   *             .get(ctx -> ctx.render(ctx.get(MyConfig.class).value))
   *         )
   *     ).test(httpClient ->
   *       Assert.assertEquals("foo", httpClient.getText())
   *     );
   *   }
   * }
   * }</pre>
   *
   * @param pointer a <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a> specifying the point in the configuration data to bind from
   * @param type the class of the type to bind to
   * @return {@code this}
   */
  ServerConfigBuilder require(String pointer, Class<?> type);

  /**
   * Builds the server config.
   *
   * @return a server config
   */
  @Override
  ServerConfig build();
}
