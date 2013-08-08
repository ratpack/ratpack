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

package org.ratpackframework.server.internal;


import org.ratpackframework.handling.Context;
import org.ratpackframework.server.BindAddress;
import org.ratpackframework.server.PublicAddress;

import java.net.MalformedURLException;
import java.net.URL;

public class DefaultPublicAddress implements PublicAddress {

  private URL publicAddress;
  private BindAddress bindAddress;

  public DefaultPublicAddress(URL publicAddress, BindAddress bindAddress) {
    this.publicAddress = publicAddress;
    this.bindAddress = bindAddress;
  }

  public URL getUrl(Context context) {
    URL currentUrl = null;
    if (publicAddress == null) {
      try {
        //TODO is it always http?
        currentUrl = new URL("http", bindAddress.getHost(), bindAddress.getPort(), "");
      } catch (MalformedURLException e) {
        //TODO deal with exception
      }
    } else {
      currentUrl = publicAddress;
    }
    return currentUrl;
  }
}