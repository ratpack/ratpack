/*
 * Copyright 2013 the original author or authors.
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

package ratpack.handling;

import com.google.common.reflect.TypeToken;
import ratpack.handling.internal.DefaultRedirector;
import ratpack.server.PublicAddress;
import ratpack.util.Types;

/**
 * Interprets objects as a {@code Location} header value, and issues the redirect response.
 * <p>
 * The redirector is not typically called directly.
 * Instead, handlers use the {@link #redirect(Context, int, Object)} method (or similar) that obtains a redirector from the context registry.
 * Ratpack provides a {@link #standard() default implementation} that is used unless a custom implementation is available.
 * <p>
 * It is rarely necessary to implement a custom redirector.
 * One reason to do so though is to use domain objects to represent redirect destinations instead of strings.
 * For example, a custom redirector may know how to transform a {@code Person} object within your application to a relevant URL.
 * It may be more convenient to implement such a mapping between domain objects and URLs within a custom redirector.
 * Such custom redirectors typically wrap the {@link #standard()} implementation.
 *
 * @see Context#redirect(int, Object)
 */
public interface Redirector {

  /**
   * The default redirect issuing strategy.
   * <p>
   * Ratpack makes this redirector available via the base server registry, making it the default.
   * <p>
   * NOTE: prior to version 1.9, redirects were always issued as absolute URLs.
   * Relative redirects were converted to absolute using the {@link PublicAddress}.
   * As of 1.9 and later, the relative redirect is issued.
   *
   * @return the standard redirector
   * @since 1.3
   */
  static Redirector standard() {
    return DefaultRedirector.INSTANCE;
  }

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<Redirector> TYPE = Types.token(Redirector.class);

  /**
   * Deprecated, replaced by {@link #redirect(Context, int, Object)}.
   * <p>
   * This interface provides a default implementation that simply forwards to {@link #redirect(Context, int, Object)}.
   *
   * @param context the request processing context when the redirect was requested
   * @param to the location to redirect to
   * @param code the status code to issue with the redirect
   * @see #redirect(Context, int, Object)
   * @deprecated use {@link #redirect(Context, int, Object)}
   */
  @Deprecated
  default void redirect(Context context, String to, int code) {
    redirect(context, code, to);
  }

  /**
   * Issues a HTTP redirect response, transforming the given {@code to} value into a value for the {@code Location} header.
   * <p>
   * Implementations may interpret the {@code to} value in any manner.
   * See {@link #standard()} for details on the default strategy.
   *
   * @param context the request processing context when the redirect was requested
   * @param code the status code to issue with the redirect
   * @param to the location to redirect to
   * @since 1.3
   */
  void redirect(Context context, int code, Object to);

}
