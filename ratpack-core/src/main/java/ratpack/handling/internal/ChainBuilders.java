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

package ratpack.handling.internal;

import io.netty.buffer.ByteBuf;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.launch.LaunchConfig;
import ratpack.reload.internal.ReloadableFileBackedFactory;
import ratpack.util.Action;
import ratpack.util.Transformer;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;
import java.util.LinkedList;
import java.util.List;

import static ratpack.util.ExceptionUtils.uncheck;

public class ChainBuilders {

  public static <T> Handler build(LaunchConfig launchConfig, final Transformer<List<Handler>, ? extends T> toChainBuilder, final Action<? super T> chainBuilderAction) {
    if (launchConfig.isReloadable()) {
      Class<?> chainBuilderActionClass = chainBuilderAction.getClass();
      File classFile = getClassFile(chainBuilderActionClass);
      if (classFile != null) {
        ReloadableFileBackedFactory<Handler[]> factory = new ReloadableFileBackedFactory<>(classFile, true, new ReloadableFileBackedFactory.Delegate<Handler[]>() {
          @Override
          public Handler[] produce(File file, ByteBuf bytes) {
            return create(toChainBuilder, chainBuilderAction);
          }
        });
        return new ReloadingFactoryHandler(factory);
      }
    }

    return Handlers.chain(create(toChainBuilder, chainBuilderAction));
  }

  private static File getClassFile(Class<?> clazz) {
    CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
    if (codeSource != null) {
      URL location = codeSource.getLocation();
      if (location.getProtocol().equals("file")) {
        File codeSourceFile = new File(location.getFile());
        File classFile = new File(codeSourceFile, clazz.getName().replace('.', File.separatorChar).concat(".class"));
        if (classFile.exists()) {
          return classFile;
        }
      }
    }

    return null;
  }

  private static <T> Handler[] create(Transformer<List<Handler>, ? extends T> toChainBuilder, Action<? super T> chainBuilderAction) {
    List<Handler> handlers = new LinkedList<>();
    T chainBuilder = toChainBuilder.transform(handlers);

    try {
      chainBuilderAction.execute(chainBuilder);
    } catch (Exception e) {
      throw uncheck(e);
    }

    return handlers.toArray(new Handler[handlers.size()]);
  }

}
