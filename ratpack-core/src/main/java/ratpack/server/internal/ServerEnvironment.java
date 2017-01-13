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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.server.ServerConfig;
import ratpack.util.internal.Environment;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class ServerEnvironment extends Environment {

  public static final ServerEnvironment INSTANCE = new ServerEnvironment(System.getenv(), System.getProperties());
  public static final String ADDRESS_PROPERTY = "ratpack.address";
  public static final String PORT_PROPERTY = "ratpack.port";
  public static final String INTELLIJ_MAIN = "com.intellij.rt.execution.application.AppMain";
  public static final String INTELLIJ_JUNIT = "com.intellij.rt.execution.junit.JUnitStarter";
  public static final String SUN_JAVA_COMMAND = "sun.java.command";

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerEnvironment.class);
  private static final Pattern STARTS_WITH_SCHEME_PATTERN = Pattern.compile("^(.+)://.+$");
  private static final int MAX_PORT = 65535;

  public ServerEnvironment(Map<String, String> env, Properties properties) {
    super(env, properties);
  }

  public static ServerEnvironment env() {
    return INSTANCE;
  }

  public InetAddress getAddress() {
    return get(null,
      i -> i != null,
      () -> parseAddressValue("ratpack.address system property", getProperties().getProperty(ADDRESS_PROPERTY)),
      () -> parseAddressValue("RATPACK_ADDRESS env var", getenv().get("RATPACK_ADDRESS"))
    );
  }

  private InetAddress parseAddressValue(String description, String value) {
    if (value == null) {
      return null;
    }
    try {
      return InetAddress.getByName(value);
    } catch (UnknownHostException e) {
      LOGGER.warn("Failed to parse {} value {} to InetAddress, using default of {}", description, value, null);
    }
    return null;
  }

  public Integer getPort() {
    return get(ServerConfig.DEFAULT_PORT,
      i -> i != null,
      () -> parsePortValue("ratpack.port system property", getProperties().getProperty(PORT_PROPERTY)),
      () -> parsePortValue("RATPACK_PORT env var", getenv().get("RATPACK_PORT")),
      () -> parsePortValue("PORT env var", getenv().get("PORT"))
    );
  }

  public URI getPublicAddress() {
    return get(null, i -> i != null,
      () -> parseUri("'ratpack.publicAddress' system property", getProperties().getProperty("ratpack.publicAddress")),
      () -> parseUri("'RATPACK_PUBLIC_ADDRESS' env var", getenv().get("RATPACK_PUBLIC_ADDRESS"))
    );
  }

  private static URI parseUri(String description, String value) {
    if (value != null) {
      try {
        URI uri = STARTS_WITH_SCHEME_PATTERN.matcher(value).matches() ? new URI(value) : new URI("http://" + value);
        String scheme = uri.getScheme();
        if (scheme.equals("http") || scheme.equals("https")) {
          return uri;
        } else {
          LOGGER.warn("Could not use {} value {} as it is not a http/https URI, ignoring value", description, value);
        }
      } catch (URISyntaxException e) {
        LOGGER.warn("Could not convert {} with value {} to a URI ({}), ignoring value", description, value, e.getMessage());
      }
    }
    return null;
  }

  public static Integer parsePortValue(String description, String value) {
    if (value == null) {
      return null;
    } else {
      try {
        int intValue = Integer.parseInt(value);
        if (intValue < 0 || intValue > MAX_PORT) {
          LOGGER.warn("{} value {} is outside of allowed range 0 - {}, using default of {}", description, intValue, MAX_PORT, ServerConfig.DEFAULT_PORT);
          return ServerConfig.DEFAULT_PORT;
        } else {
          return intValue;
        }
      } catch (NumberFormatException e) {
        LOGGER.warn("Failed to parse {} value {} to int, using default of {}", description, value, ServerConfig.DEFAULT_PORT);
        return ServerConfig.DEFAULT_PORT;
      }
    }
  }

}
