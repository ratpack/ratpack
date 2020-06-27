/*
 * Copyright 2020 the original author or authors.
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

package ratpack.func;

import com.google.common.collect.Iterables;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Utils {

  public static final String DEVELOPMENT_PROPERTY = "ratpack.development";
  public static final String INTELLIJ_MAIN = "com.intellij.rt.execution.application.AppMain";
  public static final String INTELLIJ_JUNIT = "com.intellij.rt.execution.junit.JUnitStarter";
  public static final String GROOVY_MAIN = "org.codehaus.groovy.tools.GroovyStarter";
  public static final String SUN_JAVA_COMMAND = "sun.java.command";

  static Map<String, String> env = System.getenv();
  static Properties properties = System.getProperties();

  private Utils() {}

  //TODO-v2 this is pretty rigid. Makes overriding this behavior difficult. This is only used down in ratpack-func
  //so that TypeCaching can determine which implementation to use.
  //This is then delegated to by methods in ratpack-config Environment
  public static boolean isDevelopment() {
    return Boolean.parseBoolean(
      get("false", Objects::nonNull,
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

  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> T get(T defaultValue, Predicate<? super T> accept, Supplier<T>... suppliers) {
    return Iterables.find(Iterables.transform(Arrays.asList(suppliers), Supplier::get), accept::test, defaultValue);
  }

  public static boolean isDebuggerAttached() {
    return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
  }
}
