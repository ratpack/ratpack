/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.groovy.templating.internal;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ratpackframework.Result;
import org.ratpackframework.ResultAction;
import org.ratpackframework.groovy.script.ScriptEngine;
import org.ratpackframework.groovy.templating.TemplatingConfig;
import org.ratpackframework.groovy.templating.internal.CompiledTemplate;
import org.ratpackframework.groovy.templating.internal.Render;
import org.ratpackframework.groovy.templating.internal.TemplateCompiler;
import org.ratpackframework.groovy.templating.internal.TemplateScript;
import org.ratpackframework.util.IoUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class GroovyTemplateRenderingEngine {

  private static final String ERROR_TEMPLATE = "error.html";

  private final Cache<File, CompiledTemplate> compiledTemplateCache;
  private final TemplateCompiler templateCompiler;

  @Inject
  public GroovyTemplateRenderingEngine(TemplatingConfig templatingConfig) {
    this.compiledTemplateCache = CacheBuilder.newBuilder().maximumSize(templatingConfig.getCacheSize()).build();

    ScriptEngine<TemplateScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), templatingConfig.isStaticallyCompile(), TemplateScript.class);
    templateCompiler = new TemplateCompiler(scriptEngine);
  }

  public void renderTemplate(final File templateDir, final String templateId, final Map<String, ?> model, final ResultAction<ChannelBuffer> handler) {
    final File templateFile = getTemplateFile(templateDir, templateId);
    render(templateDir, templateFile, templateId, model, handler, new Callable<ChannelBuffer>() {
      @Override
      public ChannelBuffer call() throws Exception {
        return IoUtils.readFile(templateFile);
      }
    });
  }

  public void renderError(final File templateDir, Map<String, ?> model, ResultAction<ChannelBuffer> handler) {
    final File errorTemplate = getTemplateFile(templateDir, ERROR_TEMPLATE);

    render(templateDir, errorTemplate, ERROR_TEMPLATE, model, handler, new Callable<ChannelBuffer>() {
      @Override
      public ChannelBuffer call() throws Exception {
        if (errorTemplate.exists()) {
          return IoUtils.readFile(errorTemplate);
        } else {
          return getResourceBuffer(ERROR_TEMPLATE);
        }
      }
    });
  }

  private void render(File templateDir, File templateFile, final String templateName, Map<String, ?> model, ResultAction<ChannelBuffer> handler, final Callable<? extends ChannelBuffer> bufferProvider) {
    try {
      CompiledTemplate compiledTemplate = compiledTemplateCache.get(templateFile, new Callable<CompiledTemplate>() {
        @Override
        public CompiledTemplate call() throws Exception {
          return templateCompiler.compile(bufferProvider.call(), templateName);
        }
      });
      new Render(templateCompiler, compiledTemplateCache, templateDir, compiledTemplate, model, handler);
    } catch (ExecutionException e) {
      handler.execute(new Result<ChannelBuffer>(e));
    }
  }

  private File getTemplateFile(File templateDir, String templateName) {
    return new File(templateDir, templateName);
  }

  private ChannelBuffer getResourceBuffer(String resourceName) throws IOException {
    return IoUtils.channelBuffer(IOGroovyMethods.getBytes(getClass().getResourceAsStream(resourceName)));
  }

}
