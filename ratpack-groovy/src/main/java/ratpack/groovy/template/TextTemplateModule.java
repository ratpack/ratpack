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

package ratpack.groovy.template;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.ExecController;
import ratpack.file.FileSystemBinding;
import ratpack.groovy.template.internal.TextTemplateRenderingEngine;
import ratpack.groovy.template.internal.TextTemplateRenderer;
import ratpack.guice.ConfigurableModule;
import ratpack.server.ServerConfig;

@SuppressWarnings("UnusedDeclaration")
public class TextTemplateModule extends ConfigurableModule<TextTemplateModule.Config> {

  public static class Config {
    private String templatesPath = "templates";
    private boolean staticallyCompile;

    public String getTemplatesPath() {
      return templatesPath;
    }

    public void setTemplatesPath(String templatesPath) {
      this.templatesPath = templatesPath;
    }

    public boolean isStaticallyCompile() {
      return staticallyCompile;
    }

    public void setStaticallyCompile(boolean staticallyCompile) {
      this.staticallyCompile = staticallyCompile;
    }
  }


  @Override
  protected void configure() {
    bind(TextTemplateRenderer.class);
  }

  @Provides
  @Singleton
  TextTemplateRenderingEngine provideGroovyTemplateRenderingEngine(ServerConfig serverConfig, ExecController execController, ByteBufAllocator bufferAllocator, Config config) {
    String templatesPath = config.getTemplatesPath();
    FileSystemBinding templateDir = serverConfig.getBaseDir().binding(templatesPath);
    if (templateDir == null) {
      throw new IllegalStateException("templatesPath '" + templatesPath + "' is outside the file system binding");
    }

    return new TextTemplateRenderingEngine(
      bufferAllocator,
      templateDir,
      serverConfig.isDevelopment(),
      config.staticallyCompile
    );
  }
}
