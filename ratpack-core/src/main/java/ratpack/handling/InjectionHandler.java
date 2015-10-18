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

package ratpack.handling;

import com.google.common.reflect.TypeToken;
import ratpack.handling.internal.Extractions;
import ratpack.util.Types;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static ratpack.util.Exceptions.uncheck;

/**
 * A super class that removes the boiler plate of retrieving objects from the context registry by injecting them based on a method signature.
 * <p>
 * Subclasses must implement exactly one method named {@code "handle"} that accepts a {@link Context} as the first parameter,
 * and at least one other parameter of any type.
 * <p>
 * The {@code handle(Context)} method of this class will delegate to the subclass handle method, supplying values for each parameter
 * by retrieving objects from the context and request (which are registries).
 * The context takes precedence over the request.
 * That is, if the context provides a value for the requested type it will be used regardless of whether the request also provides this type.
 * <p>
 * The following two handlers are functionally equivalent:
 * <pre class="java">{@code
 * import ratpack.handling.Context;
 * import ratpack.handling.Handler;
 * import ratpack.handling.InjectionHandler;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   static class Thing {
 *     public final String name;
 *
 *     public Thing(String name) {
 *       this.name = name;
 *     }
 *   }
 *
 *   static class VerboseHandler implements Handler {
 *     public void handle(Context context) {
 *       Thing thing = context.get(Thing.class);
 *       context.render(thing.name);
 *     }
 *   }
 *
 *   static class SuccinctHandler extends InjectionHandler {
 *     public void handle(Context context, Thing thing) {
 *       context.render(thing.name);
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(chain -> chain
 *         .register(r -> r.add(new Thing("foo")))
 *         .get("verbose", new VerboseHandler())
 *         .get("succinct", new SuccinctHandler())
 *     ).test(httpClient -> {
 *       assertEquals("foo", httpClient.getText("verbose"));
 *       assertEquals("foo", httpClient.getText("succinct"));
 *     });
 *   }
 *
 * }
 * }</pre>
 * <p>
 * If the parameters cannot be satisfied, a {@link ratpack.registry.NotInRegistryException} will be thrown.
 * The {@link java.util.Optional} type can be used to inject registry entries that may not exist.
 * <pre class="java">{@code
 * import ratpack.handling.Context;
 * import ratpack.handling.InjectionHandler;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * import java.util.Optional;
 *
 * public class Example {
 *
 *   static class OptionalInjectingHandler extends InjectionHandler {
 *     public void handle(Context context, Optional<String> string, Optional<Integer> integer) {
 *       context.render(string.orElse("missing") + ":" + integer.orElse(0));
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(chain -> chain
 *         .register(r -> r.add("foo")) // no Integer in registry
 *         .get(new OptionalInjectingHandler())
 *     ).test(httpClient -> {
 *       assertEquals("foo:0", httpClient.getText());
 *     });
 *   }
 *
 * }
 * }</pre>
 * <p>
 * If there is no suitable {@code handle(Context, ...)} method, a {@link NoSuitableHandleMethodException} will be thrown at construction time.
 */
public abstract class InjectionHandler implements Handler {

  private final List<TypeToken<?>> types;
  private final Method handleMethod;

  /**
   * Constructor.
   *
   * @throws NoSuitableHandleMethodException if this class doesn't provide a suitable handle method.
   */
  protected InjectionHandler() throws NoSuitableHandleMethodException {
    Class<?> thisClass = this.getClass();

    Method handleMethod = null;
    for (Method method : thisClass.getDeclaredMethods()) {
      if (!method.getName().equals("handle")) {
        continue;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length < 2) {
        continue;
      }

      if (!parameterTypes[0].equals(Context.class)) {
        continue;
      }

      handleMethod = method;
      break;
    }

    if (handleMethod == null) {
      throw new NoSuitableHandleMethodException(thisClass);
    }

    try {
      handleMethod.setAccessible(true);
    } catch (SecurityException e) {
      throw new NoSuitableHandleMethodException(thisClass, e);
    }

    this.handleMethod = handleMethod;

    Type[] parameterTypes = handleMethod.getGenericParameterTypes();
    this.types = new ArrayList<>(parameterTypes.length - 1);

    for (int i = 1; i < parameterTypes.length; ++i) {
      this.types.add(Types.token(parameterTypes[i]));
    }
  }

  /**
   * Invokes the custom "handle" method, extracting necessary parameters from the context to satisfy the call.
   *
   * @param context The context to handle
   */
  public final void handle(Context context) {
    Object[] args = new Object[types.size() + 1];
    args[0] = context;
    Extractions.extract(types, context, args, 1);
    try {
      handleMethod.invoke(this, args);
    } catch (IllegalAccessException e) {
      throw uncheck(e);
    } catch (InvocationTargetException e) {
      Throwable root = e.getTargetException();
      throw uncheck(root);
    }
  }

  /**
   * Exception thrown if the subclass doesn't provide a valid handle method.
   */
  public static class NoSuitableHandleMethodException extends RuntimeException {
    private static final long serialVersionUID = 0;

    private NoSuitableHandleMethodException(Class<?> clazz) {
      super("No injectable handle method found for " + clazz.getName());
    }

    public NoSuitableHandleMethodException(Class<?> clazz, SecurityException cause) {
      super("Unable to make handle method accessible for " + clazz.getName(), cause);
    }
  }

}
