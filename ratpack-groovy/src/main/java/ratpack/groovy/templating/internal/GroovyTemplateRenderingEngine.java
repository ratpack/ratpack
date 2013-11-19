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
import ratpack.groovy.script.internal.ScriptEngine;
import ratpack.groovy.templating.TemplatingConfig;
import ratpack.launch.LaunchConfig;
import ratpack.util.Action;
import ratpack.util.Result;
import ratpack.util.Transformer;

import javax.inject.Inject;
import java.io.File;
import java.util.Map;

public class GroovyTemplateRenderingEngine {

  private static final String ERROR_TEMPLATE = "error.html";

  private final LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache;
  private final TemplateCompiler templateCompiler;
  private final boolean reloadable;
  private final File templateDir;
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
    templateDir = new File(launchConfig.getBaseDir(), templatingConfig.getTemplatesPath());
  }

  public void renderTemplate(ByteBuf buffer, final String templateId, final Map<String, ?> model, final Action<Result<ByteBuf>> handler) throws Exception {
    final File templateFile = getTemplateFile(templateId);
    render(buffer, new FileTemplateSource(templateFile, templateId, reloadable), model, handler);
  }

  public void renderError(ByteBuf buffer, Map<String, ?> model, Action<Result<ByteBuf>> handler) throws Exception {
    final File errorTemplate = getTemplateFile(ERROR_TEMPLATE);
    if (errorTemplate.exists()) {
      render(buffer, new FileTemplateSource(errorTemplate, ERROR_TEMPLATE, reloadable), model, handler);
    } else {
      render(buffer, new ResourceTemplateSource(ERROR_TEMPLATE, byteBufAllocator), model, handler);
    }
  }

  private void render(ByteBuf buffer, final TemplateSource templateSource, Map<String, ?> model, Action<Result<ByteBuf>> handler) throws Exception {
    try {
      new Render(buffer, compiledTemplateCache, templateSource, model, handler, new Transformer<String, TemplateSource>() {
        public TemplateSource transform(String templateName) {
          return new FileTemplateSource(new File(templateDir, templateName), templateName, reloadable);
        }
      });
    } catch (Exception e) {
      handler.execute(new Result<ByteBuf>(e));
    }
  }

  private File getTemplateFile(String templateName) {
    return new File(templateDir, templateName);
  }

}
