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

package ratpack.groovy.internal.capture;

import groovy.lang.Binding;
import groovy.lang.Script;
import ratpack.func.BiFunction;
import ratpack.func.Function;
import ratpack.groovy.Groovy;
import ratpack.groovy.script.internal.ScriptEngine;

import java.nio.file.Path;

public class RatpackDslScriptCapture implements BiFunction<Path, String, RatpackDslClosures> {

  private final boolean compileStatic;
  private final String[] args;
  private final Function<? super RatpackDslClosures, ? extends Groovy.Ratpack> function;

  public RatpackDslScriptCapture(boolean compileStatic, String[] args, Function<? super RatpackDslClosures, ? extends Groovy.Ratpack> function) {
    this.compileStatic = compileStatic;
    this.args = args;
    this.function = function;
  }

  public RatpackDslClosures apply(Path file, String scriptContent) throws Exception {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    ScriptEngine<Script> scriptEngine = new ScriptEngine<>(classLoader, compileStatic, Script.class);
    return RatpackDslClosures.capture(function, file, () -> {
      Script script = scriptEngine.create(file.getFileName().toString(), file, scriptContent);
      script.setBinding(new Binding(args));
      script.run();
    });
  }

}
