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

import com.google.inject.Injector;
import com.google.inject.Provides;
import io.netty.util.CharsetUtil;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.handling.Handler;
import ratpack.session.clientside.internal.CookieBasedSessionStorageBindingHandler;
import ratpack.session.clientside.internal.DefaultClientSessionService;
import ratpack.session.clientside.internal.DefaultCrypto;
import ratpack.session.clientside.internal.DefaultSigner;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Singleton;

/**
 * An extension module that provides a cookie based store for sessions.
 * <h3>Provides</h3>
 * <ul>
 * <li>{@link ratpack.session.store.SessionStorage} - deserialized from the client's cookie</li>
 * </ul>
 * <h3>Getting the session storage</h3>
 * <p>
 * This module {@linkplain #decorate(com.google.inject.Injector, ratpack.handling.Handler) decorates the handler} to make
 * the {@link ratpack.session.store.SessionStorage} available during request processing.
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.session.store.SessionStorage;
 *
 * class MyHandler implements Handler {
 *   void handle(Context ctx) {
 *     SessionStorage session = ctx.get(SessionStorage.class);
 *   }
 * }
 * </pre>
 */
public class ClientSideSessionsModule extends ConfigurableModule<ClientSideSessionsModule.Config> implements HandlerDecoratingModule {

  @Override
  protected void configure() {
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

  /**
   * Makes {@link ratpack.session.store.SessionStorage} available in the context registry.
   *
   * @param injector The injector created from all the application modules
   * @param handler The application handler
   * @return A handler that provides a {@link ratpack.session.store.SessionStorage} impl in the context registry
   */
  public Handler decorate(Injector injector, Handler handler) {
    return new CookieBasedSessionStorageBindingHandler(injector.getInstance(SessionService.class), injector.getInstance(Config.class).getSessionName(), handler);
  }

  public static class Config {
    private String sessionName = "ratpack_session";
    private String secretToken = Long.toString(System.currentTimeMillis() / 10000);
    private String macAlgorithm = "HmacSHA1";
    private String secretKey;
    private String cipherAlgorithm = "AES/CBC/PKCS5Padding";
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

    public SessionService getSessionService() {
      return sessionService;
    }

    public void setSessionService(SessionService sessionService) {
      this.sessionService = sessionService;
    }
  }
}
