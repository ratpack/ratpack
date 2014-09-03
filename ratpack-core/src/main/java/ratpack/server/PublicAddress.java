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

import ratpack.handling.Context;

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
 * {@link ratpack.launch.LaunchConfigBuilder#publicAddress(java.net.URI)}.  Alternatively, you can register an instance of
 * your own implementation.
 */
public interface PublicAddress {

  /**
   * The advertised public address.
   *
   * @param context The context that the public address is being determined for.
   * @return the public address for the context.
   */
  URI getAddress(Context context);
}
