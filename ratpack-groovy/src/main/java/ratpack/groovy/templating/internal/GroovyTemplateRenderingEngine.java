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
import ratpack.exec.ExecContext;
import ratpack.exec.SuccessOrErrorPromise;
import ratpack.file.FileSystemBinding;
import ratpack.func.Transformer;
import ratpack.groovy.script.internal.ScriptEngine;
import ratpack.groovy.templating.TemplatingConfig;
import ratpack.launch.LaunchConfig;

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
    //noinspection NullableProblems
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

  public SuccessOrErrorPromise<ByteBuf> renderTemplate(ExecContext execContext, ByteBuf byteBuf, final String templateId, final Map<String, ?> model) throws Exception {
    Path templateFile = getTemplateFile(templateId);
    return render(execContext, byteBuf, toTemplateSource(templateId, templateFile), model);
  }

  private PathTemplateSource toTemplateSource(String templateId, Path templateFile) throws IOException {
    String id = templateId + (reloadable ? Files.getLastModifiedTime(templateFile) : "0");
    return new PathTemplateSource(id, templateFile, templateId);
  }

  public SuccessOrErrorPromise<ByteBuf> renderError(ExecContext execContext, ByteBuf byteBuf, Map<String, ?> model) throws Exception {
    final Path errorTemplate = getTemplateFile(ERROR_TEMPLATE);
    if (Files.exists(errorTemplate)) {
      return render(execContext, byteBuf, toTemplateSource(ERROR_TEMPLATE, errorTemplate), model);
    } else {
      return render(execContext, byteBuf, new ResourceTemplateSource(ERROR_TEMPLATE, byteBufAllocator), model);
    }
  }

  private SuccessOrErrorPromise<ByteBuf> render(ExecContext execContext, ByteBuf byteBuf, final TemplateSource templateSource, Map<String, ?> model) throws Exception {
    return Render.render(execContext, byteBuf, compiledTemplateCache, templateSource, model, new Transformer<String, TemplateSource>() {
      public TemplateSource transform(String templateName) throws IOException {
        return toTemplateSource(templateName, getTemplateFile(templateName));
      }
    });
  }

  private Path getTemplateFile(String templateName) {
    return templateDir.file(templateName);
  }

}
