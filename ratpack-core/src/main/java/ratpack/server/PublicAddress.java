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

import java.net.URI;

/**
 * The advertised public address.  This is most commonly used to perform redirects or assemble absolute URLs.
 * <p>
 * The default implementation uses a variety of strategies to attempt to provide the desired result most of the time.
 * Information used includes:
 * <ul>
 *   <li>Configured public address URI (optional)</li>
 *   <li>X-Forwarded-Host header (if included in request)</li>
 *   <li>X-Forwarded-Proto or X-Forwarded-Ssl headers (if included in request)</li>
 *   <li>Absolute request URI (if included in request)</li>
 *   <li>Host header (if included in request)</li>
 *   <li>Service's bind address and scheme (http vs. https)</li>
 * </ul>
 *
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
   *
   * @param uri the uri
   * @return a public address
   */
  static PublicAddress of(URI uri) {
    return new ConstantPublicAddress(uri);
  }

  /**
   * The advertised public address.
   *
   * @param ctx the handling context at the time the public address is needed
   * @return the public address for the context
   */
  URI get(Context ctx);

  /**
   * Creates a URL builder using the public address as the base.
   *
   * @param ctx the handling context at the time the public address is needed
   * @return a URL builder
   */
  default HttpUrlBuilder builder(Context ctx) {
    return HttpUrlBuilder.base(get(ctx));
  }

  /**
   * Creates a URL by building a URL based on the public address.
   *
   * @param ctx the handling context at the time the public address is needed
   * @param action the additions to the public address
   * @return the built url
   * @throws Exception any thrown by {@code action}
   */
  default URI get(Context ctx, Action<? super HttpUrlBuilder> action) throws Exception {
    return action.with(builder(ctx)).build();
  }

  /**
   * Creates a URL by appending the given <i>path</i> to the public address
   *
   * @param ctx the handling context at the time the public address is needed
   * @param path the path to append to the public address
   * @return the public address with the given path appended
   */
  default URI get(Context ctx, String path) {
    return builder(ctx).path(path).build();
  }

}
