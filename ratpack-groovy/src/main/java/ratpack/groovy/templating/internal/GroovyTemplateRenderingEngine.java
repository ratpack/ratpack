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

package ratpack.groovy.templating.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ratpack.file.FileSystemBinding;
import ratpack.groovy.script.internal.ScriptEngine;
import ratpack.groovy.templating.TemplatingConfig;
import ratpack.launch.LaunchConfig;
import ratpack.util.Action;
import ratpack.util.Result;
import ratpack.util.Transformer;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class GroovyTemplateRenderingEngine {

  private static final String ERROR_TEMPLATE = "error.html";

  private final LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache;
  private final TemplateCompiler templateCompiler;
  private final boolean reloadable;
  private final FileSystemBinding templateDir;
  private final ByteBufAllocator byteBufAllocator;

  @Inject
  public GroovyTemplateRenderingEngine(LaunchConfig launchConfig, TemplatingConfig templatingConfig) {
    this.byteBufAllocator = launchConfig.getBufferAllocator();

    ScriptEngine<DefaultTemplateScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), templatingConfig.isStaticallyCompile(), DefaultTemplateScript.class);
    templateCompiler = new TemplateCompiler(scriptEngine, byteBufAllocator);
    this.compiledTemplateCache = CacheBuilder.newBuilder().maximumSize(templatingConfig.getCacheSize()).build(new CacheLoader<TemplateSource, CompiledTemplate>() {
      @Override
      public CompiledTemplate load(TemplateSource templateSource) throws Exception {
        ByteBuf content = templateSource.getContent();
        try {
          return templateCompiler.compile(content, templateSource.getName());
        } finally {
          content.release();
        }
      }
    });

    reloadable = templatingConfig.isReloadable();
    String templatesPath = templatingConfig.getTemplatesPath();
    templateDir = launchConfig.getBaseDir().binding(templatesPath);
    if (templateDir == null) {
      throw new IllegalStateException("templatesPath '" + templatesPath + "' is outside the file system binding");
    }
  }

  public void renderTemplate(ByteBuf buffer, final String templateId, final Map<String, ?> model, final Action<Result<ByteBuf>> handler) throws Exception {
    Path templateFile = getTemplateFile(templateId);
    render(buffer, toTemplateSource(templateId, templateFile), model, handler);
  }

  private PathTemplateSource toTemplateSource(String templateId, Path templateFile) throws IOException {
    String id = templateId + (reloadable ? Files.getLastModifiedTime(templateFile) : "0");
    return new PathTemplateSource(id, templateFile, templateId);
  }

  public void renderError(ByteBuf buffer, Map<String, ?> model, Action<Result<ByteBuf>> handler) throws Exception {
    final Path errorTemplate = getTemplateFile(ERROR_TEMPLATE);
    if (Files.exists(errorTemplate)) {
      render(buffer, toTemplateSource(ERROR_TEMPLATE, errorTemplate), model, handler);
    } else {
      render(buffer, new ResourceTemplateSource(ERROR_TEMPLATE, byteBufAllocator), model, handler);
    }
  }

  private void render(ByteBuf buffer, final TemplateSource templateSource, Map<String, ?> model, Action<Result<ByteBuf>> handler) throws Exception {
    try {
      new Render(buffer, compiledTemplateCache, templateSource, model, handler, new Transformer<String, TemplateSource>() {
        public TemplateSource transform(String templateName) throws IOException {
          return toTemplateSource(templateName, getTemplateFile(templateName));
        }
      });
    } catch (Exception e) {
      handler.execute(new Result<ByteBuf>(e));
    }
  }

  private Path getTemplateFile(String templateName) {
    return templateDir.file(templateName);
  }

}
