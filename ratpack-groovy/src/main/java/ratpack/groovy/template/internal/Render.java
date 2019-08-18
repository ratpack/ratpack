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

package ratpack.groovy.template.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ratpack.exec.Promise;
import ratpack.func.Function;

import java.util.HashMap;
import java.util.Map;

public class Render {

  private final ByteBufAllocator bufferAllocator;
  private final Function<TextTemplateSource, CompiledTextTemplate> compiledTemplateCache;
  private final TextTemplateSource templateSource;
  private final Map<String, ?> model;
  private final Function<String, TextTemplateSource> includeTransformer;

  private Render(ByteBufAllocator bufferAllocator, Function<TextTemplateSource, CompiledTextTemplate> compiledTemplateCache, TextTemplateSource templateSource, final Map<String, ?> model, Function<String, TextTemplateSource> includeTransformer) {
    this.bufferAllocator = bufferAllocator;
    this.compiledTemplateCache = compiledTemplateCache;
    this.templateSource = templateSource;
    this.model = model;
    this.includeTransformer = includeTransformer;
  }

  private Promise<ByteBuf> invoke() {
    return Promise.async(f -> {
        ByteBuf byteBuf = bufferAllocator.ioBuffer();
        try {
          CompiledTextTemplate fromCache = getFromCache(compiledTemplateCache, templateSource);
          Render.this.execute(fromCache, model, byteBuf);
          f.success(byteBuf);
        } catch (Throwable e) {
          byteBuf.release();
          f.error(e);
        }
      }
    );
  }

  private CompiledTextTemplate getFromCache(Function<TextTemplateSource, CompiledTextTemplate> compiledTemplateCache, TextTemplateSource templateSource) throws Exception {
    return compiledTemplateCache.apply(templateSource);
  }

  private void executeNested(final String templatePath, final Map<String, ?> model, ByteBuf buffer) throws Exception {
    TextTemplateSource templateSource = includeTransformer.apply(templatePath);
    CompiledTextTemplate compiledTemplate = getFromCache(compiledTemplateCache, templateSource);
    execute(compiledTemplate, model, buffer);
  }

  private void execute(CompiledTextTemplate compiledTemplate, final Map<String, ?> model, final ByteBuf parts) throws Exception {
    compiledTemplate.execute(model, parts, (templatePath, nestedModel) -> {
      Map<String, Object> modelCopy = new HashMap<>(model);
      modelCopy.putAll(nestedModel);
      executeNested(templatePath, modelCopy, parts);
    });
  }

  public static Promise<ByteBuf> render(ByteBufAllocator bufferAllocator, Function<TextTemplateSource, CompiledTextTemplate> compiledTemplateCache, TextTemplateSource templateSource, Map<String, ?> model, Function<String, TextTemplateSource> includeTransformer) throws Exception {
    return new Render(bufferAllocator, compiledTemplateCache, templateSource, model, includeTransformer).invoke();
  }
}
