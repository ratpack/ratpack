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

import org.ratpackframework.server.BindAddress;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class InetSocketAddressBackedBindAddress implements BindAddress {

  private final int port;
  private final String host;

  public InetSocketAddressBackedBindAddress(InetSocketAddress socketAddress) {
    this.port = socketAddress.getPort();
    this.host = determineHost(socketAddress);
  }

  public static String determineHost(InetSocketAddress socketAddress) {
    InetAddress address = socketAddress.getAddress();
    String hostAddress = address.getHostAddress();
    if (address.isAnyLocalAddress() || hostAddress.equals("127.0.0.1")) {
      return "localhost";
    } else {
      return hostAddress;
    }
  }

  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

}
