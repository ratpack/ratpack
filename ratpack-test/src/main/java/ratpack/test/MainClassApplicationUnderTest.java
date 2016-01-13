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

package ratpack.test;

import ratpack.server.RatpackServer;
import ratpack.server.internal.ServerCapturer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * An application under test fixture that can be used to test a server started by a “main” method.
 *
 * <pre class="java">{@code
 * import ratpack.server.RatpackServer;
 * import ratpack.test.MainClassApplicationUnderTest;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   public static class App {
 *     public static void main(String[] args) throws Exception {
 *       RatpackServer.start(s -> s
 *         .handlers(c -> c
 *           .get(ctx -> ctx.render("Hello world!"))
 *         )
 *       );
 *     }
 *   }
 *
 *   public static void main(String[] args) throws Exception {
 *     new MainClassApplicationUnderTest(App.class).test(testHttpClient ->
 *       assertEquals("Hello world!", testHttpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 * <p>
 * Note that this type implements {@link CloseableApplicationUnderTest}, and should be closed when no longer needed.
 *
 * @see #addImpositions(ratpack.impose.ImpositionsSpec)
 * @see ServerBackedApplicationUnderTest
 */
public class MainClassApplicationUnderTest extends ServerBackedApplicationUnderTest {

  private final Class<?> mainClass;

  /**
   * Creates a new app under test, based on the given main class.
   *
   * @param mainClass a class who's main method starts a Ratpack server
   */
  public MainClassApplicationUnderTest(Class<?> mainClass) {
    this.mainClass = mainClass;
  }

  /**
   * Starts the Ratpack server by invoking the {@code public static void main(String[])} method of the “main class” backing this object.
   *
   * @return the Ratpack server created by the main method
   * @throws Exception if the main method cannot be invoked
   */
  @Override
  protected RatpackServer createServer() throws Exception {
    RatpackServer ratpackServer = ServerCapturer.capture(() -> {
      Method method;
      try {
        method = mainClass.getDeclaredMethod("main", String[].class);
      } catch (NoSuchMethodException e) {
        throw new IllegalStateException("Class" + mainClass.getName() + " does not have a main(String...) class");
      }

      if (!Modifier.isStatic(method.getModifiers())) {
        throw new IllegalStateException(mainClass.getName() + ".main() must be static");
      }

      try {
        method.invoke(null, new Object[]{new String[]{}});
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new IllegalStateException("Could not invoke " + mainClass.getName() + ".main()", e);
      }

    });

    if (ratpackServer == null) {
      throw new IllegalStateException(mainClass.getName() + ".main() did not start a Ratpack server");
    } else {
      return ratpackServer;
    }
  }
}
