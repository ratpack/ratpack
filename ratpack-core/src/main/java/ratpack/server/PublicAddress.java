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

package ratpack.server;

import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.http.HttpUrlBuilder;
import ratpack.server.internal.ConstantPublicAddress;
import ratpack.server.internal.InferringPublicAddress;

import java.net.URI;

/**
 * The advertised public address.
 * <p>
 * This is most commonly used to perform redirects or assemble absolute URLs.
 * <p>
 * If the default implementation isn't doing what you want it to, you can usually simply configure the public address via
 * {@link ServerConfigBuilder#publicAddress(java.net.URI)}.  Alternatively, you can register an instance of
 * your own implementation.
 */
public interface PublicAddress {

  /**
   * Creates a new public address object using the given URI.
   * <p>
   * The path, query and fragment components of the URI will be stripped.
   * <p>
   * This implementation is implicitly used if a {@link ServerConfigBuilder#publicAddress(URI)} was specified.
   *
   * @param uri the uri
   * @return a public address
   */
  static PublicAddress of(URI uri) {
    return new ConstantPublicAddress(uri);
  }

  /**
   * An implementation that infers the public address from the current request.
   * <p>
   * The public address is inferred based on the following:
   * <ul>
   *   <li>X-Forwarded-Host header (if included in request)</li>
   *   <li>X-Forwarded-Proto or X-Forwarded-Ssl headers (if included in request)</li>
   *   <li>Absolute request URI (if included in request)</li>
   *   <li>Host header (if included in request)</li>
   *   <li>Protocol of request (i.e. http or https)</li>
   * </ul>
   * <p>
   * This implementation is implicitly used if no {@link ServerConfigBuilder#publicAddress(URI)} was specified.
   *
   * @param defaultScheme the scheme ({@code http} or {@code https}) if what to use can't be determined from the request
   * @return a public address
   * @since 1.2
   */
  static PublicAddress inferred(String defaultScheme) {
    return new InferringPublicAddress(defaultScheme);
  }

  /**
   * The advertised public address.
   * <p>
   * This method has been deprecated.
   * Since 1.2, the default implementation simply calls {@link #get()}.
   * Implementations should implement that method only.
   *
   * @param ctx the handling context at the time the public address is needed
   * @return the public address for the context
   * @deprecated since 1.2
   */
  @Deprecated
  default URI get(@SuppressWarnings("UnusedParameters") Context ctx) {
    return get();
  }

  /**
   * Returns the public address.
   * <p>
   * This method was introduced in 1.2 and supersedes {@link #get(Context)}.
   * The default implementation throws {@link UnsupportedOperationException}, however, all Ratpack provided implemenations
   * properly implement this method and it should be used instead of {@link #get(Context)}
   *
   * @return the public address
   * @since 1.2
   */
  default URI get() {
    throw new UnsupportedOperationException("this implementation does not support this method, use get(Context)");
  }

  /**
   * Creates a URL builder using the public address as the base.
   *
   * @param ctx the handling context at the time the public address is needed
   * @return a URL builder
   * @deprecated since 1.2, use {@link #builder()}
   */
  @Deprecated
  default HttpUrlBuilder builder(@SuppressWarnings("UnusedParameters") Context ctx) {
    return builder();
  }

  /**
   * Creates a URL builder using the public address as the base.
   *
   * @return a URL builder
   * @since 1.2
   */
  default HttpUrlBuilder builder() {
    return HttpUrlBuilder.base(get());
  }

  /**
   * Creates a URL by building a URL based on the public address.
   *
   * @param ctx the handling context at the time the public address is needed
   * @param action the additions to the public address
   * @return the built url
   * @throws Exception any thrown by {@code action}
   * @deprecated since 1.2, use {@link #get(Action)}
   */
  @Deprecated
  default URI get(@SuppressWarnings("UnusedParameters") Context ctx, Action<? super HttpUrlBuilder> action) throws Exception {
    return action.with(builder()).build();
  }

  /**
   * Creates a URL by building a URL based on the public address.
   *
   * @param action the additions to the public address
   * @return the built url
   * @throws Exception any thrown by {@code action}
   * @since 1.2
   */
  default URI get(Action<? super HttpUrlBuilder> action) throws Exception {
    return action.with(builder()).build();
  }

  /**
   * Creates a URL by appending the given <i>path</i> to the public address
   *
   * @param ctx the handling context at the time the public address is needed
   * @param path the path to append to the public address
   * @return the public address with the given path appended
   * @deprecated since 1.2, use {@link #get(String)}
   */
  @Deprecated
  default URI get(@SuppressWarnings("UnusedParameters") Context ctx, String path) {
    return builder().path(path).build();
  }

  /**
   * Creates a URL by appending the given <i>path</i> to the public address
   *
   * @param path the path to append to the public address
   * @return the public address with the given path appended
   * @since 1.2
   */
  default URI get(String path) {
    return builder().path(path).build();
  }

}
