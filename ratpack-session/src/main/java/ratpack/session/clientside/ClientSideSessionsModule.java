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

package ratpack.session.clientside;

import com.google.common.reflect.TypeToken;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import io.netty.util.CharsetUtil;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.session.clientside.internal.CookieBasedSessionStorageBindingHandler;
import ratpack.session.clientside.internal.DefaultClientSessionService;
import ratpack.session.clientside.internal.DefaultCrypto;
import ratpack.session.clientside.internal.DefaultSigner;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * An extension module that provides a cookie based store for sessions.
 * <h3>Provides</h3>
 * <ul>
 * <li>{@link ratpack.session.store.SessionStorage} - deserialized from the client's cookie</li>
 * </ul>
 * <h3>Getting the session storage</h3>
 *
 * <pre class="tested java">{@code
 * import ratpack.handling.*;
 * import ratpack.session.store.SessionStorage;
 *
 * class MyHandler implements Handler {
 *   void handle(Context ctx) {
 *     SessionStorage sessionStorage = ctx.getRequest().get(SessionStorage.class);
 *     sessionStorage.get("value", String.class).then( (value) -> { render(value.orElse("not set"));});
 *
 *   }
 * }
 * }</pre>
 * <h3>Configuration</h3>
 * <p>
 * This module also provides a programmatic configurable object that helps customize various elements.
 * <table>
 *   <caption>Configurable Elements</caption>
 *   <tr>
 *     <th>sessionName</th>
 *     <td>The name of the cookie in which the session is stored. Defaults to <strong>ratpack_session</strong>.</td>
 *   </tr>
 *   <tr>
 *     <th>secretToken</th>
 *     <td>The token used to sign the serialized session to prevent tampering. If not set, this is set to a time based value</td>
 *   </tr>
 *   <tr>
 *     <th>macAlgorithm</th>
 *     <td>The {@link javax.crypto.Mac} algorithm used to sign the serialized session with the <strong>secretToken</strong>.</td>
 *   </tr>
 *   <tr>
 *     <th>secretKey</th>
 *     <td>The secret key used in the symmetric-key encyrption/decryption with the serialized session.</td>
 *   </tr>
 *   <tr>
 *     <th>cipherAlgorithm</th>
 *     <td>The {@link javax.crypto.Cipher} algorithm used to encrypt/decrypt the serialized session, e.g. <strong>AES/CBC/PKCS5Padding</strong> which is also the default value.</td>
 *   </tr>
 * </table>
 *
 * <h3>Configuration Example</h3>
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.session.clientside.ClientSideSessionsModule;
 * import ratpack.session.store.SessionStorage;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.*;
 *
 * public class ClientSideSessionsModuleConfigExample {
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b ->
 *         b.add(ClientSideSessionsModule.class, config -> {
 *           config.setSessionName("session-name");
 *           config.setSecretToken("your token for signing");
 *           // config.setSecretKey("key for cipher");
 *           // config.setMacAlgorithm("MAC algorithm for signing");
 *           // config.setCipherAlgorithm("Cipher Algorithm");
 *         })
 *       ))
 *       .handlers(chain -> chain
 *         .get(ctx -> {
 *           SessionStorage sessionStorage = ctx.getRequest().get(SessionStorage.class);
 *           sessionStorage.get("value",String.class).then( value -> { ctx.render(value.orElse("not set"));});
 *         })
 *         .get("set/:value", ctx -> {
 *           SessionStorage sessionStorage = ctx.getRequest().get(SessionStorage.class);
 *           String value = ctx.getPathTokens().get("value");
 *           sessionStorage.set("value", value).then( success->{
 *            ctx.render(value);
 *           });
 *
 *         })
 *       )
 *     ).test(client -> {
 *       ReceivedResponse response = client.get();
 *       assertEquals("not set", response.getBody().getText());
 *       assertFalse("No cookies should be set", response.getHeaders().contains("Set-Cookie"));
 *
 *       response = client.get("set/foo");
 *       assertEquals("foo", response.getBody().getText());
 *       assertTrue("We set a value", response.getHeaders().contains("Set-Cookie"));
 *       assertTrue("Session uses our session name", response.getHeaders().get("Set-Cookie").contains("session-name"));
 *
 *       response = client.get();
 *       assertEquals("foo", response.getBody().getText());
 *       assertFalse("We did not update session", response.getHeaders().contains("Set-Cookie"));
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3>Notes</h3>
 * <p>
 * Because the session is serialized to the client, all key value pairs in the session
 * are String based. The max cookie size for a client is 4k so it's important to keep
 * this in mind when using the ClientSideSessionsModule.
 *
 * <p>
 * By default your session will be signed but not encrypted. This is because the <strong>secretKey</strong>
 * is not set by default. That is, your users will not be able to tamper with the
 * cookie but they can still read the key value pairs that you have set. If you want to render
 * the entire cookie unreadable make sure you set a <strong>secretKey</strong>
 *
 * <p>
 * When setting your own <strong>secretKey</strong> and <strong>cipherAlgorithm</strong>
 * make sure that the key length is acceptable according to the algorithm you have chosen.
 *
 */
public class ClientSideSessionsModule extends ConfigurableModule<ClientSideSessionsModule.Config> {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toProvider(CookieBasedSessionStorageHandlerDecorator.class);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  Signer provideSigner(Config config) {
    byte[] token = config.getSecretToken().getBytes(CharsetUtil.UTF_8);
    return new DefaultSigner(new SecretKeySpec(token, config.getMacAlgorithm()));
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  Crypto provideCrypto(Config config) {
    DefaultCrypto crypto = null;
    if (config.getSecretKey() != null && config.getCipherAlgorithm() != null) {
      byte[] key = config.getSecretKey().getBytes(CharsetUtil.UTF_8);
      crypto = new DefaultCrypto(key, config.getCipherAlgorithm());
    }
    return crypto;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  SessionService provideSessionService(Config config, Signer signer, Crypto crypto) {
    SessionService sessionService = config.getSessionService();
    if (sessionService == null) {
      sessionService = new DefaultClientSessionService(signer, crypto);
    }
    return sessionService;
  }

  private static class CookieBasedSessionStorageHandlerDecorator implements Provider<HandlerDecorator> {
    private final SessionService sessionService;
    private final Config config;

    @Inject
    public CookieBasedSessionStorageHandlerDecorator(SessionService sessionService, Config config) {
      this.sessionService = sessionService;
      this.config = config;
    }

    @Override
    public HandlerDecorator get() {
      return HandlerDecorator.prepend(new CookieBasedSessionStorageBindingHandler(sessionService,
        config.getSessionName(),
        config.getPath(),
        config.getDomain(),
        config.getMaxCookieSize(),
        config.getMaxInactivityInterval()));
    }
  }

  public static class Config {
    /**
     * Used to get cookie based session configuration.
     */
    public static final TypeToken<Config> COOKIE_SESSION_CONFIG_TYPE_TOKEN = TypeToken.of(Config.class);

    private String sessionName = "ratpack_session";
    private String secretToken = Long.toString(System.currentTimeMillis() / 10000);
    private String macAlgorithm = "HmacSHA1";
    private String secretKey;
    private String cipherAlgorithm = "AES/CBC/PKCS5Padding";
    private String path = "/";
    private String domain;
    private int maxCookieSize = 2048;
    private long maxInactivityInterval = 120;
    private SessionService sessionService;

    public String getSessionName() {
      return sessionName;
    }

    public void setSessionName(String sessionName) {
      this.sessionName = sessionName;
    }

    public String getSecretToken() {
      return secretToken;
    }

    public void setSecretToken(String secretToken) {
      this.secretToken = secretToken;
    }

    public String getMacAlgorithm() {
      return macAlgorithm;
    }

    public void setMacAlgorithm(String macAlgorithm) {
      this.macAlgorithm = macAlgorithm;
    }

    public String getSecretKey() {
      return secretKey;
    }

    public void setSecretKey(String secretKey) {
      this.secretKey = secretKey;
    }

    public String getCipherAlgorithm() {
      return cipherAlgorithm;
    }

    public void setCipherAlgorithm(String cipherAlgorithm) {
      this.cipherAlgorithm = cipherAlgorithm;
    }

    /**
     * Use the session cookie only when requesting from the {@code path}.
     * <p>
     * Define the scope of the cookie.
     * <p>
     * Session should be send for every request. The {@code path} of value {@code "/"} does this.
     * @return the URI path to which session cookie will be attached to.
     */
    public String getPath() {
      return path;
    }

    /**
     * Set the {@code path} for session cookie.
     * <p>
     * Define the scope of the cookie.
     *
     * @param path a path to which session cookie will be attached to
     */
    public void setPath(String path) {
      this.path = path;
    }

    /**
     * Use the session cookie only when requesting from the {@code domain}.
     * <p>
     * Define the scope for the cookie.
     *
     * @return the URI domain to which session cookie will be attached to.
     */
    public String getDomain() {
      return domain;
    }

    /**
     * Set the {@code domain} for session cookie.
     * <p>
     * Define the scope of the cookie
     *
     * @param domain a domain to which session cokkie will be attached to
     */
    public void setDomain(String domain) {
      this.domain = domain;
    }

    /**
     * Maximum size of the session cookie. If encrypted cookie exceeds it, it will be partitioned.
     * <p>
     * According to the <a href="http://www.ietf.org/rfc/rfc2109.txt">RFC 2109</a> web cookies should be at least
     * 4096 bytes per cookie and at least 20 cookies per domain should be supported.
     * <p>
     * Default value of maximum cookie size is set to {@code 2048}.
     * @return the maximum size of the cookie session.
     */
    public int getMaxCookieSize() {
      return maxCookieSize;
    }

    /**
     * Set maximum size of the session cookie. If encrypted cookie session exceeds it, it wil be partitioned.
     * <p>
     * If it is less than {@code 1024} or greater than {@code 4096} default value will be used.
     *
     * @param maxCookieSize a maximum size of one session cookie.
     */
    public void setMaxCookieSize(int maxCookieSize) {
      if (maxCookieSize < 1024 || maxCookieSize > 4096) {
        this.maxCookieSize = 2048;
      } else {
        this.maxCookieSize = maxCookieSize;
      }
    }

    /**
     * Maximum inactivity time (in seconds) after which session will be invalidated.
     * <p>
     * If it is {@code -1} inactivity is unlimited. the {@code 0} value means that session is always invalid.
     * If time between last access and current time is less than or equal to max inactive time, session will become valid.
     *
     * @return the maximum session inactivity time
     */
    public long getMaxInactivityInterval() {
      return maxInactivityInterval;
    }

    /**
     * Set maximum inactivity time (in seconds) of the cookie session.
     *
     * @param maxInactivityInterval a maximum inactivity time of the cookie session
     */
    public void setMaxInactivityInterval(long maxInactivityInterval) {
      this.maxInactivityInterval = maxInactivityInterval;
    }

    public SessionService getSessionService() {
      return sessionService;
    }

    public void setSessionService(SessionService sessionService) {
      this.sessionService = sessionService;
    }
  }
}
