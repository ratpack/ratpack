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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ratpackframework.Result;
import org.ratpackframework.ResultHandler;
import org.ratpackframework.util.IoUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Render {

  private final Cache<String, CompiledTemplate> compiledTemplateCache;
  private final TemplateCompiler templateCompiler;
  private final File templateDir;

  public Render(TemplateCompiler templateCompiler, Cache<String, CompiledTemplate> compiledTemplateCache, File templateDir, CompiledTemplate template, Map<String, ?> model, final ResultHandler<ChannelBuffer> handler) {
    this.templateCompiler = templateCompiler;
    this.compiledTemplateCache = compiledTemplateCache;
    this.templateDir = templateDir;

    ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();

    try {
      execute(template, model, buffer);
    } catch (Exception e) {
      handler.handle(new Result<ChannelBuffer>(e));
      return;
    }

    handler.handle(new Result<ChannelBuffer>(buffer));
  }


  private void executeNested(final String templatePath, final Map<String, ?> model, ChannelBuffer buffer) throws Exception {
    CompiledTemplate cachedTemplate = compiledTemplateCache.getIfPresent(templatePath);
    if (cachedTemplate != null) {
      execute(cachedTemplate, model, buffer);
      return;
    }

    File templateFile = new File(templateDir, templatePath);

    CompiledTemplate compiledTemplate;

    ChannelBuffer channelBuffer = IoUtils.readFile(templateFile);
    compiledTemplate = templateCompiler.compile(channelBuffer, templatePath);

    compiledTemplateCache.put(templatePath, compiledTemplate);
    execute(compiledTemplate, model, buffer);
  }


  private void execute(CompiledTemplate compiledTemplate, final Map<String, ?> model, final ChannelBuffer parts) throws Exception {
    compiledTemplate.execute(model, parts, new NestedRenderer() {
      public void render(String templatePath, Map<String, ?> nestedModel) throws Exception {
        Map<String, Object> modelCopy = new HashMap<>(model);
        modelCopy.putAll(nestedModel);
        executeNested(templatePath, modelCopy, parts);
      }
    });
  }

}