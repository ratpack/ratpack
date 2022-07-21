package ratpack.test.http.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.http.client.ProxyCredentials;
import ratpack.http.client.internal.DefaultProxyCredentials;
import ratpack.test.http.proxy.internal.Authority;
import ratpack.test.http.proxy.internal.ProxyAuthenticationHandler;
import ratpack.test.http.proxy.internal.ProxyClientHandler;
import ratpack.util.Exceptions;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ratpack.test.http.proxy.internal.ProxyHandlerNames.HTTP_CODEC_HANDLER;

// TODO Add class-level and method-level Javadoc
public class TestHttpProxyServer implements AutoCloseable {

  public static final Logger LOGGER = LoggerFactory.getLogger(TestHttpProxyServer.class);

  // TODO can we reuse Ratpack's test-scoped ExecutionContext in some way to avoid creating another set of EventLoopGroups?
  private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
  private final EventLoopGroup workerGroup = new NioEventLoopGroup();

  private Channel listeningChannel;

  private SelfSignedCertificate certificate;

  private ConcurrentMap<Authority, Integer> numTunnelsByDestination = new ConcurrentHashMap<>();

  /**
   * Starts the HTTP Proxy server without requiring authentication.
   */
  public void start() {
    this.start(false);
  }

  // TODO Refactor to use a TestProxyServerSpec instead of taking proxy server configuration parameters directly in the start method
  public void start(boolean requireSsl) {
    this.start(null, requireSsl);
  }

  /** Starts the HTTP proxy server with proxy authentication enabled.
   *
   * @param username the username clients must provide to authenticate
   * @param password the password clients must provide to authenticate
   */
  public void start(String username, String password) {
    this.start(username, password, false);
  }

  public void start(String username, String password, boolean requireSsl) {
    this.start(new DefaultProxyCredentials(username, password), requireSsl);
  }

  /** Starts the HTTP proxy server. If credentials are not null, then basic proxy authentication is required.
   *
   * @param credentials the username/password to require for proxy authentication, or null if no authentication is required
   */
  private void start(ProxyCredentials credentials, boolean requireSsl) {
    LOGGER.info("Starting test HTTP proxy server...");
    try {

      final SslContext sslContext = createSslContext(requireSsl);

      listeningChannel = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (requireSsl) {
              pipeline.addLast(sslContext.newHandler(ch.alloc()));
            }
            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
            pipeline.addLast(HTTP_CODEC_HANDLER, new HttpServerCodec());
            if (credentials != null) {
              pipeline.addLast(new ProxyAuthenticationHandler(credentials));
            }
            pipeline.addLast(new ProxyClientHandler(numTunnelsByDestination));
          }
        })
        .bind("localhost",0).sync().channel();
      LOGGER.info("Test HTTP proxy started for http://{}:{}", this.getAddress().getHostName(), this.getAddress().getPort());
    } catch (InterruptedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (CertificateException | SSLException e) {
      LOGGER.error(e.getMessage(), e);
      throw Exceptions.uncheck(e);
    }
  }

  private SslContext createSslContext(boolean requireSsl) throws CertificateException, SSLException {
    if (requireSsl) {
      certificate = new SelfSignedCertificate("testHttpProxyServer");
      return SslContextBuilder.forServer(certificate.certificate(), certificate.privateKey()).build();
    }
    return null;
  }

  public InetSocketAddress getAddress() {
    return ((InetSocketAddress) listeningChannel.localAddress());
  }

  public X509Certificate getSslCertificate() {
    if (certificate != null) {
      return certificate.cert();
    }
    return null;
  }

  public boolean proxied(URI address) {
    return proxied(address.getHost(), address.getPort());
  }

  public boolean proxied(String host, int port) {
    return numTunnelsByDestination.containsKey(new Authority(host, port));
  }

  public void stop() {
    if (listeningChannel != null && listeningChannel.isActive()) {
      LOGGER.info("Stopping test HTTP proxy server...");
      listeningChannel.close().syncUninterruptibly();
      numTunnelsByDestination.clear();   // TODO even though we're using a ConcurrentMap, this may not be 100% concurrency-safe. A race condition could exist between this clear call and any leftover client connection executions (need to confirm if closing the listenerChannel always closes all child executions).
      LOGGER.info("Test HTTP proxy server stopped.");
    }
  }

  @Override
  public void close() {
    try {
      this.stop();
    } finally {
      bossGroup.shutdownGracefully().syncUninterruptibly();
      workerGroup.shutdownGracefully().syncUninterruptibly();
    }
  }
}
