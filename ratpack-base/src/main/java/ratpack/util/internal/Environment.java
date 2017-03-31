/*
 * Copyright 2017 the original author or authors.
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

package ratpack.util.internal;

import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Environment {

  public static final Environment INSTANCE = new Environment(System.getenv(), System.getProperties());
  public static final String DEVELOPMENT_PROPERTY = "ratpack.development";
  public static final String INTELLIJ_MAIN = "com.intellij.rt.execution.application.AppMain";
  public static final String INTELLIJ_JUNIT = "com.intellij.rt.execution.junit.JUnitStarter";
  public static final String GROOVY_MAIN = "org.codehaus.groovy.tools.GroovyStarter";
  public static final String SUN_JAVA_COMMAND = "sun.java.command";

  private static final Logger LOGGER = LoggerFactory.getLogger(Environment.class);

  private final Map<String, String> env;
  private final Properties properties;

  public Environment(Map<String, String> env, Properties properties) {
    this.env = env;
    this.properties = properties;
  }

  public static Environment env() {
    return INSTANCE;
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  protected static <T> T get(T defaultValue, Predicate<? super T> accept, Supplier<T>... suppliers) {
    return Iterables.find(Iterables.transform(Arrays.asList(suppliers), Supplier::get), accept::test, defaultValue);
  }

  public Map<String, String> getenv() {
    return env;
  }

  public Properties getProperties() {
    return properties;
  }

  public boolean isDevelopment() {
    return Boolean.parseBoolean(
      get("false", i -> i != null,
        () -> properties.getProperty(DEVELOPMENT_PROPERTY),
        () -> env.get("RATPACK_DEVELOPMENT"),
        () -> {
          String command = properties.getProperty(SUN_JAVA_COMMAND, "");
          return command.startsWith(INTELLIJ_MAIN) && !command.contains(INTELLIJ_JUNIT) ? "true" : null;
        },
        () -> {
          String command = properties.getProperty(SUN_JAVA_COMMAND, "");
          return command.startsWith(GROOVY_MAIN) ? "true" : null;
        },
        () -> isDebuggerAttached() ? "true" : null
      )
    );
  }

  protected static boolean isDebuggerAttached() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
  }

}
