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
import com.google.inject.Singleton;
import io.netty.buffer.ByteBuf;
import ratpack.exec.ExecControl;
import ratpack.exec.Promise;
import ratpack.file.FileSystemBinding;
import ratpack.groovy.script.internal.ScriptEngine;
import ratpack.groovy.templating.TemplatingConfig;
import ratpack.launch.LaunchConfig;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Singleton
public class GroovyTemplateRenderingEngine {

  private final LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache;
  private final TemplateCompiler templateCompiler;
  private final boolean reloadable;
  private final FileSystemBinding templateDir;
  private final ExecControl execControl;

  @Inject
  public GroovyTemplateRenderingEngine(LaunchConfig launchConfig, TemplatingConfig templatingConfig, ExecControl execControl) {
    this.execControl = execControl;

    ScriptEngine<DefaultTemplateScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), templatingConfig.isStaticallyCompile(), DefaultTemplateScript.class);
    templateCompiler = new TemplateCompiler(scriptEngine, launchConfig.getBufferAllocator());
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

  public Promise<ByteBuf> renderTemplate(ByteBuf byteBuf, final String templateId, final Map<String, ?> model) throws Exception {
    Path templateFile = getTemplateFile(templateId);
    return render(byteBuf, toTemplateSource(templateId, templateFile), model);
  }

  private TemplateSource toTemplateSource(String templateId, Path templateFile) throws IOException {
    String id = templateId + (reloadable ? Files.getLastModifiedTime(templateFile) : "0");
    return new TemplateSource(id, templateFile, templateId);
  }

  private Promise<ByteBuf> render(ByteBuf byteBuf, final TemplateSource templateSource, Map<String, ?> model) throws Exception {
    return Render.render(execControl, byteBuf, compiledTemplateCache, templateSource, model, templateName -> toTemplateSource(templateName, getTemplateFile(templateName)));
  }

  private Path getTemplateFile(String templateName) {
    return templateDir.file(templateName);
  }

}
