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

package org.ratpackframework.templating.internal;

import com.google.common.cache.Cache;
import com.hazelcast.util.ConcurrentHashSet;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.FileSystem;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Render {

  private final Cache<String, CompiledTemplate> compiledTemplateCache;
  private final Set<ExecutingTemplate> executingTemplates = new ConcurrentHashSet<ExecutingTemplate>();
  private final Map<ExecutingTemplate, Exception> templateErrors = new HashMap<ExecutingTemplate, Exception>();

  private final AtomicBoolean completionHandlerFired = new AtomicBoolean();
  private final AtomicBoolean topLevelExecuting = new AtomicBoolean();

  private final FileSystem fileSystem;
  private final TemplateCompiler templateCompiler;
  private final String templateDirPath;

  private final AsyncResultHandler<Buffer> handler;

  private ExecutedTemplate rootTemplate;

  public static class ExecutingTemplate {
  }

  public Render(TemplateCompiler templateCompiler, FileSystem fileSystem, Cache<String, CompiledTemplate> compiledTemplateCache, String templateDirPath, CompiledTemplate template, Map<String, ?> model, final AsyncResultHandler<Buffer> handler) {
    this.templateCompiler = templateCompiler;
    this.fileSystem = fileSystem;
    this.compiledTemplateCache = compiledTemplateCache;
    this.templateDirPath = templateDirPath;
    this.handler = handler;

    topLevelExecuting.set(true);
    try {
      rootTemplate = execute(template, model);
    } catch (Exception e) {
      handler.handle(new AsyncResult<Buffer>(e));
      return;
    } finally {
      topLevelExecuting.set(false);
    }

    if (executingTemplates.isEmpty()) {
      complete();
    }
  }

  private void failTemplate(ExecutingTemplate template, Exception error) {
    //noinspection ThrowableResultOfMethodCallIgnored
    templateErrors.put(template, error);
    completeTemplate(template);
  }

  private void completeTemplate(ExecutingTemplate template) {
    executingTemplates.remove(template);
    if (!topLevelExecuting.get() && executingTemplates.isEmpty() && completionHandlerFired.compareAndSet(false, true)) {
      complete();
    }
  }

  private void executeNested(final String templatePath, final Map<String, ?> model, final NestedTemplate nestedTemplate) {
    final ExecutingTemplate executingTemplate = new ExecutingTemplate(); // token
    executingTemplates.add(executingTemplate);

    CompiledTemplate cachedTemplate = compiledTemplateCache.getIfPresent(templatePath);
    if (cachedTemplate != null) {
      execute(cachedTemplate, model, nestedTemplate, executingTemplate);
      return;
    }

    String absoluteTemplatePath = templateDirPath + File.separator + templatePath;
    fileSystem.readFile(absoluteTemplatePath, new AsyncResultHandler<Buffer>() {
      @Override
      public void handle(AsyncResult<Buffer> event) {
        if (event.failed()) {
          nestedTemplate.handle(new AsyncResult<ExecutedTemplate>(event.exception));
          failTemplate(executingTemplate, event.exception);
        } else {
          CompiledTemplate compiledTemplate;
          try {
            compiledTemplate = templateCompiler.compile(event.result, templatePath);
          } catch (Exception e) {
            nestedTemplate.handle(new AsyncResult<ExecutedTemplate>(e));
            return;
          }

          compiledTemplateCache.put(templatePath, compiledTemplate);
          execute(compiledTemplate, model, nestedTemplate, executingTemplate);
        }
      }
    });
  }

  private void execute(CompiledTemplate compiledTemplate, Map<String, ?> model, NestedTemplate nestedTemplate, ExecutingTemplate executingTemplate) {
    ExecutedTemplate executedTemplate;
    try {
      executedTemplate = execute(compiledTemplate, model);
    } catch (Exception e) {
      nestedTemplate.handle(new AsyncResult<ExecutedTemplate>(e));
      failTemplate(executingTemplate, e);
      return;
    }

    nestedTemplate.handle(new AsyncResult<ExecutedTemplate>(executedTemplate));
    completeTemplate(executingTemplate);
  }

  private ExecutedTemplate execute(CompiledTemplate compiledTemplate, final Map<String, ?> model) {
    return compiledTemplate.execute(model, new NestedRenderer() {
      public NestedTemplate render(String templatePath, Map<String, ?> nestedModel) {
        final NestedTemplate nestedTemplate = new NestedTemplate();
        Map<String, Object> modelCopy = new HashMap<String, Object>(model);
        modelCopy.putAll(nestedModel);
        executeNested(templatePath, modelCopy, nestedTemplate);
        return nestedTemplate;
      }
    });
  }

  private void complete() {
    if (!templateErrors.isEmpty()) {
      handler.handle(new AsyncResult<Buffer>(new CompositeException(templateErrors.values())));
      return;
    }

    Buffer buffer = new Buffer();
    rootTemplate.render(buffer);
    handler.handle(new AsyncResult<Buffer>(buffer));
  }

}