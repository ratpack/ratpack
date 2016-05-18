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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import io.netty.handler.codec.http.HttpMethod;
import ratpack.api.Nullable;
import ratpack.config.ConfigData;
import ratpack.config.ConfigObject;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.impose.Impositions;
import ratpack.server.internal.DefaultServerConfigBuilder;
import ratpack.server.internal.ServerEnvironment;
import ratpack.util.Types;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

/**
 * The configuration of the server.
 * <p>
 * This object represents the basic information needed to bootstrap the server (e.g. {@link #getPort()}),
 * but also provides access to any externalised config objects to be used by the application via {@link #get(String, Class)}
 * (see also: {@link #getRequiredConfig()}).
 * A server config object is-a {@link ConfigData} object.
 * <p>
 * Server config objects are programmatically built via a {@link ServerConfigBuilder}, which can be obtained via the static methods {@link #builder()}} and {@link #embedded()}.
 */
public interface ServerConfig extends ConfigData {

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<ServerConfig> TYPE = Types.token(ServerConfig.class);

  /**
   * The default port for Ratpack applications, {@value}.
   */
  int DEFAULT_PORT = 5050;

  /**
   * The default max content length.
   */
  int DEFAULT_MAX_CONTENT_LENGTH = 1048576;

  /**
   * The default number of threads an application should use.
   *
   * Calculated as {@code Runtime.getRuntime().availableProcessors() * 2}.
   */
  int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  /**
   * The default maximum chunk size to use when reading request/response bodies.
   * <p>
   * Defaults to {@value}.
   *
   * @see #getMaxChunkSize()
   */
  int DEFAULT_MAX_CHUNK_SIZE = 8192;

  Set<HttpMethod> DEFAULT_METHODS_CAN_HAVE_BODY = Sets.newHashSet(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

  /**
   * Creates a builder configured for development mode and an ephemeral port.
   *
   * @return a server config builder
   */
  static ServerConfigBuilder embedded() {
    return builder().development(true).port(0);
  }

  static ServerConfigBuilder builder() {
    return new DefaultServerConfigBuilder(ServerEnvironment.env(), Impositions.current());
  }

  static ServerConfig of(Action<? super ServerConfigBuilder> action) throws Exception {
    return action.with(builder()).build();
  }

  /**
   * The port that the application should listen to requests on.
   * <p>
   * Defaults to {@value #DEFAULT_PORT}.
   *
   * @return The port that the application should listen to requests on.
   */
  int getPort();

  /**
   * The address of the interface that the application should bind to.
   * <p>
   * A value of null causes all interfaces to be bound. Defaults to null.
   *
   * @return The address of the interface that the application should bind to.
   */
  @Nullable
  InetAddress getAddress();

  /**
   * The config objects that were declared as required when this server config was built.
   * <p>
   * Required config is declared via the {@link ServerConfigBuilder#require(String, Class)} when building.
   * All required config is made part of the base registry (which the server registry joins with),
   * which automatically makes the config objects available to the server registry.
   *
   *
   * @return the declared required config
   * @see ServerConfigBuilder#require(String, Class)
   */
  ImmutableSet<ConfigObject<?>> getRequiredConfig();

  /**
   * Whether or not the server is in "development" mode.
   * <p>
   * A flag for indicating to Ratpack internals that the app is under development; diagnostics and reloading are more important than performance and security.
   * <p>
   * In development mode Ratpack will leak internal information through diagnostics and stacktraces by sending them to the response.
   *
   * @return {@code true} if the server is in "development" mode
   */
  boolean isDevelopment();

  /**
   * The number of threads for handling application requests.
   * <p>
   * If the value is greater than 0, a thread pool (of this size) will be created for servicing requests and doing computation.
   * If the value is 0 (default) or less, a thread pool of size {@link Runtime#availableProcessors()} {@code * 2} will be used.
   * <p>
   * This effectively sizes the {@link ratpack.exec.ExecController#getExecutor()} thread pool size.
   *
   * @return the number of threads for handling application requests.
   */
  int getThreads();

  /**
   * The public address of the site used for redirects.
   *
   * @return The url of the public address
   */
  URI getPublicAddress();

  /**
   * The SSL context to use if the application will serve content over HTTPS.
   *
   * @return The SSL context or <code>null</code> if the application does not use SSL.
   */
  @Nullable
  SSLContext getSslContext();

  /**
   * Whether or not the server needs client SSL authentication {@link javax.net.ssl.SSLEngine#setNeedClientAuth(boolean)}.
   *
   * @return whether or not the server needs client SSL authentication
   */
  boolean isRequireClientSslAuth();

  /**
   * The max content length to use for the HttpObjectAggregator.
   *
   * @return The max content length as an int.
   */
  int getMaxContentLength();

  /**
   * The connect timeout of the channel.
   *
   * @return The connect timeout in milliseconds
   * @see <a href="http://netty.io/4.0/api/io/netty/channel/socket/ServerSocketChannelConfig.html#setConnectTimeoutMillis(int)" target="_blank">setConnectTimeoutMillis</a>
   */
  Optional<Integer> getConnectTimeoutMillis();

  /**
   * The maximum number of messages to read per read loop.
   * <p>
   * If this value is greater than 1, an event loop might attempt to read multiple times to procure multiple messages.
   *
   * @return The maximum number of messages to read
   * @see <a href="http://netty.io/4.0/api/io/netty/channel/socket/ServerSocketChannelConfig.html#setMaxMessagesPerRead(int)" target="_blank">setMaxMessagesPerRead</a>
   */
  Optional<Integer> getMaxMessagesPerRead();

  /**
   * The <a href="http://docs.oracle.com/javase/7/docs/api/java/net/StandardSocketOptions.html?is-external=true#SO_RCVBUF" target="_blank">StandardSocketOptions.SO_RCVBUF</a> option.
   *
   * @return The receive buffer size
   * @see <a href="http://netty.io/4.0/api/io/netty/channel/socket/ServerSocketChannelConfig.html#setReceiveBufferSize(int)" target="_blank">setReceiveBufferSize</a>
   */
  Optional<Integer> getReceiveBufferSize();

  /**
   * The maximum loop count for a write operation until <a href="http://docs.oracle.com/javase/7/docs/api/java/nio/channels/WritableByteChannel.html?is-external=true#write(java.nio.ByteBuffer)" target="_blank">WritableByteChannel.write(ByteBuffer)</a> returns a non-zero value.
   * <p>
   * It is similar to what a spin lock is used for in concurrency programming. It improves memory utilization and write throughput depending on the platform that JVM runs on.
   *
   * @return The write spin count
   * @see <a href="http://netty.io/4.0/api/io/netty/channel/socket/ServerSocketChannelConfig.html#setWriteSpinCount(int)" target="_blank">setWriteSpinCount</a>
   */
  Optional<Integer> getWriteSpinCount();

  /**
   * Whether or not the base dir of the application has been set.
   *
   * @return whether or not the base dir of the application has been set.
   */
  boolean isHasBaseDir();

  /**
   * The maximum chunk size to use when reading request (server) or response (client) bodies.
   * <p>
   * This value is used to determine the size of chunks to emit when consuming request/response bodies.
   * This generally only has an impact when consuming the body as a stream.
   * A lower value will reduce memory pressure by requiring less memory at one time,
   * but at the expense of throughput.
   * <p>
   * Defaults to {@link #DEFAULT_MAX_CHUNK_SIZE}.
   * This value is suitable for most applications.
   * If your application deals with very large bodies, you may want to increase it.
   *
   * @return the maximum chunk size
   */
  int getMaxChunkSize();

  /**
   * The base dir of the application, which is also the initial {@link ratpack.file.FileSystemBinding}.
   *
   * @return The base dir of the application.
   * @throws NoBaseDirException if this launch config has no base dir set.
   */
  FileSystemBinding getBaseDir() throws NoBaseDirException;


  /**
   * The methods that could have bodies so they need to be treated that way otherwise a body will be ignored.
   *
   * @return A set of HttpMethods that could have a body.
   */
  Set<HttpMethod> getMethodsCanHaveBody();
}
