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

package org.ratpackframework.templating;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;
import org.ratpackframework.config.LayoutConfig;
import org.ratpackframework.config.TemplatingConfig;
import org.ratpackframework.handler.Result;
import org.ratpackframework.handler.ResultHandler;
import org.ratpackframework.io.IoUtils;
import org.ratpackframework.script.internal.ScriptEngine;
import org.ratpackframework.templating.internal.CompiledTemplate;
import org.ratpackframework.templating.internal.Render;
import org.ratpackframework.templating.internal.TemplateCompiler;
import org.ratpackframework.templating.internal.TemplateScript;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class GroovyTemplateRenderer implements TemplateRenderer {

  private final File templateDir;
  private final String errorPageTemplate;

  private final Cache<String, CompiledTemplate> compiledTemplateCache;
  private final boolean staticallyCompile;

  @Inject
  public GroovyTemplateRenderer(LayoutConfig layoutConfig, TemplatingConfig templatingConfig) {
    this.staticallyCompile = templatingConfig.isStaticallyCompile();
    this.templateDir = new File(layoutConfig.getBaseDir(), templatingConfig.getDir());
    this.errorPageTemplate = getResourceText("exception.html");
    this.compiledTemplateCache = CacheBuilder.newBuilder().maximumSize(templatingConfig.getCacheSize()).build();
  }

  @Override
  public void renderFileTemplate(final String templateFileName, final Map<String, ?> model, final ResultHandler<ChannelBuffer> handler) {
    try {
      final TemplateCompiler templateCompiler = createCompiler();
      CompiledTemplate cachedTemplate = compiledTemplateCache.getIfPresent(templateFileName);
      if (cachedTemplate != null) {
        render(templateCompiler, cachedTemplate, model, handler);
      } else {
        File templateFile = getTemplateFile(templateFileName);
        ChannelBuffer templateSource = IoUtils.readFile(templateFile);
        CompiledTemplate compiledTemplate = templateCompiler.compile(templateSource, templateFileName);
        compiledTemplateCache.put(templateFileName, compiledTemplate);
        render(templateCompiler, compiledTemplate, model, handler);
      }
    } catch (IOException e) {
      handler.handle(new Result<ChannelBuffer>(e));
    }
  }

  @Override
  public void renderError(Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
    render(errorPageTemplate, "errorpage", model, handler);
  }

  private void render(String template, String templateName, Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
    try {
      TemplateCompiler templateCompiler = createCompiler();
      ChannelBuffer templateSource = ChannelBuffers.copiedBuffer(template, CharsetUtil.UTF_8);
      CompiledTemplate compiledTemplate = templateCompiler.compile(templateSource, templateName);
      render(templateCompiler, compiledTemplate, model, handler);
    } catch (Exception e) {
      handler.handle(new Result<ChannelBuffer>(e));
    }
  }

  private TemplateCompiler createCompiler() {
    return new TemplateCompiler(new ScriptEngine<>(getClass().getClassLoader(), staticallyCompile, TemplateScript.class));
  }

  private void render(TemplateCompiler templateCompiler, CompiledTemplate compiledTemplate, Map<String, ?> model, ResultHandler<ChannelBuffer> handler) {
    new Render(templateCompiler, compiledTemplateCache, templateDir, compiledTemplate, model, handler);
  }

  private File getTemplateFile(String templateName) {
    return new File(templateDir, templateName);
  }

  private String getResourceText(String resourceName) {
    try {
      return IOGroovyMethods.getText(getClass().getResourceAsStream(resourceName));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
