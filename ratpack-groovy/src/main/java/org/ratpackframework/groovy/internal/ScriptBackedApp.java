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

package org.ratpackframework.groovy.internal;

import com.google.inject.Injector;
import com.google.inject.Module;
import groovy.lang.Closure;
import groovy.lang.Script;
import io.netty.buffer.ByteBuf;
import org.ratpackframework.groovy.Util;
import org.ratpackframework.groovy.script.internal.ScriptEngine;
import org.ratpackframework.guice.ModuleRegistry;
import org.ratpackframework.guice.internal.GuiceBackedHandlerFactory;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.reload.internal.ReloadableFileBackedFactory;
import org.ratpackframework.util.Action;
import org.ratpackframework.util.Transformer;
import org.ratpackframework.util.Factory;
import org.ratpackframework.util.internal.IoUtils;

import java.io.File;

public class ScriptBackedApp implements Handler {

  private final Factory<Handler> reloadHandler;
  private final File script;

  public ScriptBackedApp(File script, final GuiceBackedHandlerFactory appFactory, final Transformer<? super Module, ? extends Injector> moduleTransformer, final boolean staticCompile, boolean reloadable) {
    this.script = script;
    this.reloadHandler = new ReloadableFileBackedFactory<>(script, reloadable, new ReloadableFileBackedFactory.Delegate<Handler>() {
      public Handler produce(final File file, final ByteBuf bytes) {
        try {
          final String string;
          string = IoUtils.utf8String(bytes);
          final ScriptEngine<Script> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), staticCompile, Script.class);

          Runnable runScript = new Runnable() {
            public void run() {
              try {
                scriptEngine.run(file.getName(), string);
              } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
              }
            }
          };

          final DefaultRatpack ratpack = new DefaultRatpack();
          Action<Closure<?>> backing = new Action<Closure<?>>() {
            public void execute(Closure<?> configurer) {
              Util.configureDelegateFirst(ratpack, configurer);
            }
          };

          RatpackScriptBacking.withBacking(backing, runScript);

          Closure<?> modulesConfigurer = ratpack.getModulesConfigurer();
          Closure<?> handlersConfigurer = ratpack.getHandlersConfigurer();

          Action<ModuleRegistry> modulesAction = Util.delegatingAction(ModuleRegistry.class, modulesConfigurer);
          return appFactory.create(modulesAction, moduleTransformer, new InjectorHandlerTransformer(handlersConfigurer));

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });

    new Thread(new Runnable() {
      public void run() {
        try {
          reloadHandler.create();
        } catch (Exception ignore) {
          // ignore
        }
      }
    }).run();
  }

  public void handle(Context context) {
    Handler handler = reloadHandler.create();
    if (handler == null) {
      context.getResponse().send("script file does not exist:" + script.getAbsolutePath());
    } else {
      handler.handle(context);
    }
  }

}
