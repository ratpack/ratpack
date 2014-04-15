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

import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import io.netty.buffer.ByteBuf;
import ratpack.exec.ExecContext;
import ratpack.exec.Fulfiller;
import ratpack.exec.SuccessOrErrorPromise;
import ratpack.func.Action;
import ratpack.func.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class Render {

  private final ExecContext execContext;
  private final ByteBuf byteBuf;
  private final LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache;
  private final TemplateSource templateSource;
  private final Map<String, ?> model;
  private final Transformer<String, TemplateSource> includeTransformer;

  public Render(ExecContext execContext, ByteBuf byteBuf, LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache, TemplateSource templateSource, final Map<String, ?> model, Transformer<String, TemplateSource> includeTransformer) {
    this.execContext = execContext;
    this.byteBuf = byteBuf;
    this.compiledTemplateCache = compiledTemplateCache;
    this.templateSource = templateSource;
    this.model = model;
    this.includeTransformer = includeTransformer;
  }

  private SuccessOrErrorPromise<ByteBuf> invoke() {
    return execContext.promise(new Action<Fulfiller<ByteBuf>>() {
      @Override
      public void execute(Fulfiller<ByteBuf> fulfiller) throws Exception {
        try {
          CompiledTemplate fromCache = getFromCache(compiledTemplateCache, templateSource);
          Render.this.execute(fromCache, model, byteBuf);
          fulfiller.success(byteBuf);
        } catch (Throwable e) {
          fulfiller.error(e);
        }
      }
    });
  }

  private CompiledTemplate getFromCache(LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache, TemplateSource templateSource) {
    try {
      return compiledTemplateCache.get(templateSource);
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

  private void executeNested(final String templatePath, final Map<String, ?> model, ByteBuf buffer) throws Exception {
    TemplateSource templateSource = includeTransformer.transform(templatePath);
    CompiledTemplate compiledTemplate = getFromCache(compiledTemplateCache, templateSource);
    execute(compiledTemplate, model, buffer);
  }

  private void execute(CompiledTemplate compiledTemplate, final Map<String, ?> model, final ByteBuf parts) throws Exception {
    compiledTemplate.execute(model, parts, new NestedRenderer() {
      public void render(String templatePath, Map<String, ?> nestedModel) throws Exception {
        Map<String, Object> modelCopy = new HashMap<>(model);
        modelCopy.putAll(nestedModel);
        executeNested(templatePath, modelCopy, parts);
      }
    });
  }

  public static SuccessOrErrorPromise<ByteBuf> render(ExecContext execContext, ByteBuf byteBuf, LoadingCache<TemplateSource, CompiledTemplate> compiledTemplateCache, TemplateSource templateSource, Map<String, ?> model, Transformer<String, TemplateSource> includeTransformer) throws Exception {
    return new Render(execContext, byteBuf, compiledTemplateCache, templateSource, model, includeTransformer).invoke();
  }
}