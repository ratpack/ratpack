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

package ratpack.test;

import ratpack.launch.RatpackMain;
import ratpack.server.RatpackServer;
import ratpack.util.Factory;

import java.util.Properties;

public class RatpackMainServerFactory implements Factory<RatpackServer> {

  private final RatpackMain ratpackMain;

  public RatpackMainServerFactory() {
    this(new RatpackMain());
  }

  public RatpackMainServerFactory(RatpackMain ratpackMain) {
    this.ratpackMain = ratpackMain;
  }

  @Override
  public RatpackServer create() {
    Properties systemProperties = new Properties(System.getProperties());
    systemProperties.setProperty("ratpack.port", systemProperties.getProperty("ratpack.port", "0"));
    return ratpackMain.server(systemProperties, new Properties());
  }

}
