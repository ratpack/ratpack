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

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.server.ServerConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ServerEnvironment {

  public static final ServerEnvironment INSTANCE = new ServerEnvironment(System.getenv(), System.getProperties());

  public static final String DEVELOPMENT_PROPERTY = "ratpack.development";

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerEnvironment.class);
  private static final int MAX_PORT = 65535;
  public static final String PORT_PROPERTY = "ratpack.port";

  private final Map<String, String> env;
  private final Properties properties;

  public ServerEnvironment(Map<String, String> env, Properties properties) {
    this.env = env;
    this.properties = properties;
  }

  public static ServerEnvironment env() {
    return INSTANCE;
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static <T> T get(T defaultValue, Predicate<? super T> accept, Supplier<T>... suppliers) {
    return Iterables.find(Iterables.transform(Arrays.asList(suppliers), Supplier::get), accept::test, defaultValue);
  }

  public Map<String, String> getenv() {
    return env;
  }

  public Properties getProperties() {
    return properties;
  }

  public Integer getPort() {
    return get(ServerConfig.DEFAULT_PORT,
      i -> i != null,
      () -> parsePortValue("ratpack.port system property", properties.getProperty(PORT_PROPERTY)),
      () -> parsePortValue("RATPACK_PORT env var", env.get("RATPACK_PORT")),
      () -> parsePortValue("PORT env var", env.get("PORT"))
    );
  }

  public boolean isDevelopment() {
    return Boolean.parseBoolean(
      get("false", i -> i != null,
        () -> properties.getProperty(DEVELOPMENT_PROPERTY),
        () -> env.get("RATPACK_DEVELOPMENT")
      )
    );
  }

  public URI getPublicAddress() {
    return get(null, i -> i != null,
      () -> parseUri("'ratpack.publicAddress' system property", properties.getProperty("ratpack.publicAddress")),
      () -> parseUri("'RATPACK_PUBLIC_ADDRESS' env var", env.get("RATPACK_PUBLIC_ADDRESS"))
    );
  }

  private static URI parseUri(String description, String value) {
    if (value != null) {
      try {
        URI uri = new URI(value);
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
