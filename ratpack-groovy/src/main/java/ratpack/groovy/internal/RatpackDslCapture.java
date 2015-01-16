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

package ratpack.groovy.internal;

import groovy.lang.Closure;
import groovy.lang.Script;
import ratpack.func.Action;
import ratpack.func.BiFunction;
import ratpack.func.Function;
import ratpack.groovy.Groovy;
import ratpack.groovy.script.internal.ScriptEngine;

import java.nio.file.Path;
import java.util.function.Supplier;

import static ratpack.util.ExceptionUtils.uncheck;

public class RatpackDslCapture<T extends Groovy.Ratpack, V> implements BiFunction<Path, String, V> {

  private final Supplier<T> dslBackingFactory;
  private final boolean compileStatic;
  private final Function<? super T, ? extends V> valueFunction;

  public RatpackDslCapture(boolean compileStatic, Supplier<T> dslBackingFactory, Function<? super T, ? extends V> valueFunction) {
    this.dslBackingFactory = dslBackingFactory;
    this.compileStatic = compileStatic;
    this.valueFunction = valueFunction;
  }

  public V apply(Path file, String script) throws Exception {
    ClassLoader classLoader = RatpackDslCapture.class.getClassLoader();
    ScriptEngine<Script> scriptEngine = new ScriptEngine<>(classLoader, compileStatic, Script.class);

    ClosureCaptureAction backing = new ClosureCaptureAction();
    RatpackScriptBacking.withBacking(backing, () ->
        uncheck(() -> scriptEngine.create(file.getFileName().toString(), file, script).run())
    );

    T dslBacking = dslBackingFactory.get();
    Closure<?> closure = backing.closure;
    ClosureUtil.configureDelegateFirst(dslBacking, closure);
    return valueFunction.apply(dslBacking);
  }

  private static class ClosureCaptureAction implements Action<Closure<?>> {
    private Closure<?> closure;

    public void execute(Closure<?> configurer) throws Exception {
      closure = configurer;
    }
  }

}
