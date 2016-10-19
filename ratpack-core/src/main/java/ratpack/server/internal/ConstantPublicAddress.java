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

package ratpack.server.internal;

import ratpack.server.PublicAddress;
import ratpack.util.Exceptions;

import java.net.URI;
import java.net.URISyntaxException;

public class ConstantPublicAddress implements PublicAddress {

  private final URI uri;

  public ConstantPublicAddress(URI uri) {
    try {
      this.uri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
    } catch (URISyntaxException e) {
      throw Exceptions.uncheck(e);
    }
  }

  @Override
  public URI get() {
    return uri;
  }

}
