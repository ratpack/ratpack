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
