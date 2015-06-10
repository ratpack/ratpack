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
package ratpack.session.clientside;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import io.netty.util.CharsetUtil;
import ratpack.guice.ConfigurableModule;
import ratpack.session.SessionStore;
import ratpack.session.clientside.internal.*;

import javax.crypto.spec.SecretKeySpec;

/**
 * An extension module that provides a client side session store - cookie based.
 * <p>
 * This module depends on {@link ratpack.session.SessionModule} and <b>MUST</b> be added to the module list <b>AFTER</b> {@link ratpack.session.SessionModule}.
 *
 * <h3>Example usage</h3>
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.session.Session;
 * import ratpack.session.SessionKey;
 * import ratpack.session.SessionModule;
 * import ratpack.session.clientside.ClientSideSessionModule;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.*;
 *
 * public class ClientSideSessionModuleExample {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registry(Guice.registry(b -> b
 *         .module(SessionModule.class, config -> {
 *           //config.path("/").domain("www.example.com");
 *         })
 *         .module(ClientSideSessionModule.class, config -> {
 *           config.setSessionCookieName("session_name");
 *           config.setSecretToken("your token for signing");
 *           //config.setSecretKey("key for cipher");
 *           //config.setMacAlgorithm("MAC algorithm for signing");
 *           //config.setCipherAlgorithm("Cipher Algorithm");
 *           //config.setMaxSessionCookieSize(1024);
 *           //config.setMaxInactivityInterval(Duration.ofSeconds(60));
 *         })
 *       ))
 *       .handlers(chain -> chain
 *         .get(ctx -> {
 *           ctx.get(Session.class).getData()
 *             .map(d -> d.get(SessionKey.ofType("value", "value")).orElse("not set"))
 *             .then(ctx::render);
 *         })
 *         .get("set/:value", ctx -> {
 *           ctx.get(Session.class).getData().then(d -> {
 *             d.set("value", ctx.getPathTokens().get("value"));
 *             ctx.render(d.get(SessionKey.ofType("value", "value")).orElse("not set"));
 *           });
 *         })
 *       )
 *     )
 *     .test(client -> {
 *       ReceivedResponse response = client.get();
 *       assertEquals("not set", response.getBody().getText());
 *       assertFalse("No cookies should be set", response.getHeaders().getAll("Set-Cookie").contains("session_name"));
 *
 *       response = client.get("set/foo");
 *       assertEquals("foo", response.getBody().getText());
 *       assertTrue("We set a value and our session name", response.getHeaders().getAll("Set-Cookie")
 *          .stream()
 *          .anyMatch(c -> c.startsWith("session_name")));
 *
 *       response = client.get();
 *       assertEquals("foo", response.getBody().getText());
 *       assertFalse("We did not update session", response.getHeaders().getAll("Set-Cookie")
 *          .stream()
 *          .anyMatch(c -> c.startsWith("session_name")));
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3>Notes</h3>
 * <p>
 * The max cookie size for a client is 4k so it's important to keep
 * this in mind when using the {@link ClientSideSessionModule}
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
 * <p>
 * When working in multi instances environment the
 * {@code secretToken} has to be the same for every ratpack instance configuration.
 */
public class ClientSideSessionModule extends ConfigurableModule<ClientSideSessionConfig> {

  @Override
  protected void configure() {
    bind(SessionStore.class).to(ClientSideSessionStore.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  Signer signer(ClientSideSessionConfig config) {
    byte[] token = config.getSecretToken().getBytes(CharsetUtil.UTF_8);
    return new DefaultSigner(new SecretKeySpec(token, config.getMacAlgorithm()));
  }

  @Provides
  @Singleton
  Crypto crypto(ClientSideSessionConfig config) {
    if (config.getSecretKey() == null || config.getCipherAlgorithm() == null) {
      return NoCrypto.INSTANCE;
    } else {
      return new DefaultCrypto(config.getSecretKey().getBytes(CharsetUtil.UTF_8), config.getCipherAlgorithm());
    }
  }

}
