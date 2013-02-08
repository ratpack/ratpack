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

package org.ratpackframework.groovy.templating;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.jboss.netty.buffer.ChannelBuffer;
import org.ratpackframework.config.LayoutConfig;
import org.ratpackframework.config.TemplatingConfig;
import org.ratpackframework.groovy.ScriptEngine;
import org.ratpackframework.groovy.templating.internal.*;
import org.ratpackframework.handler.Result;
import org.ratpackframework.handler.ResultHandler;
import org.ratpackframework.io.IoUtils;
import org.ratpackframework.templating.TemplateRenderer;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class GroovyTemplateRenderer implements TemplateRenderer {

  private static final String ERROR_TEMPLATE = "error.html";

  private final File templateDir;
  private final Cache<String, CompiledTemplate> compiledTemplateCache;
  private final TemplateCompiler templateCompiler;

  @Inject
  public GroovyTemplateRenderer(LayoutConfig layoutConfig, TemplatingConfig templatingConfig) {
    this.templateDir = new File(layoutConfig.getBaseDir(), templatingConfig.getDir());
    this.compiledTemplateCache = CacheBuilder.newBuilder().maximumSize(templatingConfig.getCacheSize()).build();

    ScriptEngine<TemplateScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), templatingConfig.isStaticallyCompile(), TemplateScript.class);
    templateCompiler = new TemplateCompiler(scriptEngine);
  }

  @Override
  public void renderTemplate(final String templateId, final Map<String, ?> model, final ResultHandler<ChannelBuffer> handler) {
    render(templateId, model, handler, new Callable<ChannelBuffer>() {
      @Override
      public ChannelBuffer call() throws Exception {
        return IoUtils.readFile(getTemplateFile(templateId));
      }
    });
  }

  @Override
  public void renderError(Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
    render(ERROR_TEMPLATE, model, handler, new Callable<ChannelBuffer>() {
      @Override
      public ChannelBuffer call() throws Exception {
        File errorTemplate = getTemplateFile(ERROR_TEMPLATE);
        if (errorTemplate.exists()) {
          return IoUtils.readFile(errorTemplate);
        } else {
          return getResourceBuffer(ERROR_TEMPLATE);
        }
      }
    });
  }

  private void render(final String templateName, Map<String, ?> model, ResultHandler<ChannelBuffer> handler, final Callable<? extends ChannelBuffer> bufferProvider) {
    try {
      CompiledTemplate compiledTemplate = compiledTemplateCache.get(templateName, new Callable<CompiledTemplate>() {
        @Override
        public CompiledTemplate call() throws Exception {
          return templateCompiler.compile(bufferProvider.call(), templateName);
        }
      });
      new Render(templateCompiler, compiledTemplateCache, templateDir, compiledTemplate, model, handler);
    } catch (ExecutionException e) {
      handler.handle(new Result<ChannelBuffer>(e));
    }
  }

  private File getTemplateFile(String templateName) {
    return new File(templateDir, templateName);
  }

  private ChannelBuffer getResourceBuffer(String resourceName) throws IOException {
      return IoUtils.channelBuffer(IOGroovyMethods.getBytes(getClass().getResourceAsStream(resourceName)));
  }

}
