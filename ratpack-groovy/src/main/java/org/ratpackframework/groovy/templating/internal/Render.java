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

package org.ratpackframework.groovy.templating.internal;

import com.google.common.cache.Cache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.ratpackframework.util.internal.Result;
import org.ratpackframework.util.internal.ResultAction;
import org.ratpackframework.util.internal.IoUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Render {

  private final Cache<String, CompiledTemplate> compiledTemplateCache;
  private final TemplateCompiler templateCompiler;
  private final File templateDir;
  private final boolean checkTimestamp;

  public Render(TemplateCompiler templateCompiler, Cache<String, CompiledTemplate> compiledTemplateCache, boolean checkTimestamp, File templateDir, CompiledTemplate template, Map<String, ?> model, final ResultAction<ByteBuf> handler) {
    this.templateCompiler = templateCompiler;
    this.compiledTemplateCache = compiledTemplateCache;
    this.checkTimestamp = checkTimestamp;
    this.templateDir = templateDir;

    ByteBuf buffer = Unpooled.buffer();

    try {
      execute(template, model, buffer);
    } catch (Exception e) {
      handler.execute(new Result<ByteBuf>(e));
      return;
    }

    handler.execute(new Result<ByteBuf>(buffer));
  }


  private void executeNested(final String templatePath, final Map<String, ?> model, ByteBuf buffer) throws Exception {
    File templateFile = new File(templateDir, templatePath);

    long timeStamp = checkTimestamp ? templateFile.lastModified() : 0;
    String templateKey = templateFile.getPath() + File.separator + timeStamp;

    CompiledTemplate cachedTemplate = compiledTemplateCache.getIfPresent(templateKey);
    if (cachedTemplate != null) {
      execute(cachedTemplate, model, buffer);
      return;
    }

    ByteBuf byteBuf = IoUtils.readFile(templateFile);
    CompiledTemplate compiledTemplate = templateCompiler.compile(byteBuf, templatePath);

    compiledTemplateCache.put(templateKey, compiledTemplate);
    execute(compiledTemplate, model, buffer);
  }


  private void execute(CompiledTemplate compiledTemplate, final Map<String, ?> model, final ByteBuf parts) throws Exception {
    compiledTemplate.execute(model, parts, new NestedRenderer() {
      public void render(String templatePath, Map<String, ?> nestedModel) throws Exception {
        Map<String, Object> modelCopy = new HashMap<String, Object>(model);
        modelCopy.putAll(nestedModel);
        executeNested(templatePath, modelCopy, parts);
      }
    });
  }

}