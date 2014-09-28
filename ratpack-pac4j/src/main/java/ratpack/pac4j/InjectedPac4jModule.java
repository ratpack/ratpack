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

package ratpack.pac4j;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Types;
import org.pac4j.core.client.Client;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.profile.UserProfile;
import ratpack.pac4j.internal.AbstractPac4jModule;

import java.lang.reflect.Type;

/**
 * An extension module that provides support for authentication via pac4j.
 * <p>
 * If you don't need/want to perform dependency injection on either the {@link org.pac4j.core.client.Client} or {@link ratpack.pac4j.Authorizer}, use {@link ratpack.pac4j.Pac4jModule} instead.
 * <p>
 * To use this module, you need to register it as well as a custom module that binds a {@link org.pac4j.core.client.Client} and an {@link ratpack.pac4j.Authorizer}.
 *
 * @param <C> The {@link org.pac4j.core.credentials.Credentials} type
 * @param <U> The {@link org.pac4j.core.profile.UserProfile} type
 */
public final class InjectedPac4jModule<C extends Credentials, U extends UserProfile> extends AbstractPac4jModule<C, U> {
  private final Type credentialsType;
  private final Type userProfileType;

  /**
   * Constructs a new instance.
   *
   * @param credentialsType The credentials type to use for authentication
   * @param userProfileType The user profile type to use for authentication
   */
  public InjectedPac4jModule(Class<C> credentialsType, Class<U> userProfileType) {
    this.credentialsType = credentialsType;
    this.userProfileType = userProfileType;
  }

  @Override
  protected Client<C, U> getClient(Injector injector) {
    return injector.getInstance(getClientKey());
  }

  @Override
  protected Authorizer getAuthorizer(Injector injector) {
    return injector.getInstance(Authorizer.class);
  }

  @SuppressWarnings("unchecked")
  private Key<Client<C, U>> getClientKey() {
    return (Key<Client<C, U>>) Key.get(Types.newParameterizedType(Client.class, credentialsType, userProfileType));
  }
}
