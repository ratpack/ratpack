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

import com.google.common.cache.LoadingCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.ratpackframework.util.internal.Result;
import org.ratpackframework.util.internal.ResultAction;
import org.ratpackframework.util.Transformer;

import java.util.HashMap;
import java.util.Map;

public class Render {

  private final LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache;
  private final Transformer<String, TemplateSource> includeTransformer;

  public Render(LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache, TemplateSource templateSource, Map<String, ?> model, final ResultAction<ByteBuf> handler, Transformer<String, TemplateSource> includeTransformer) {
    this.compiledTemplateCache = compiledTemplateCache;
    this.includeTransformer = includeTransformer;

    ByteBuf buffer = Unpooled.buffer();

    try {
      execute(compiledTemplateCache.get(templateSource), model, buffer);
    } catch (Exception e) {
      handler.execute(new Result<ByteBuf>(e));
      return;
    }

    handler.execute(new Result<ByteBuf>(buffer));
  }


  private void executeNested(final String templatePath, final Map<String, ?> model, ByteBuf buffer) throws Exception {
    TemplateSource templateSource = includeTransformer.transform(templatePath);
    CompiledTemplate compiledTemplate = compiledTemplateCache.get(templateSource);
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