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

import com.hazelcast.util.ConcurrentHashSet;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.FileSystem;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Render {

  private boolean anyInnerTemplates = false;
  private final Set<ExecutingTemplate> executingTemplates = new ConcurrentHashSet<ExecutingTemplate>();
  private final Map<ExecutingTemplate, Exception> templateErrors = new HashMap<ExecutingTemplate, Exception>();

  private final AtomicBoolean completionHandlerFired = new AtomicBoolean();

  private final FileSystem fileSystem;
  private final TemplateCompiler templateCompiler;
  private final String templateDirPath;

  private final AsyncResultHandler<Buffer> handler;
  private final ClassLoader classLoader;

  private ExecutedTemplate rootTemplate;

  public static class ExecutingTemplate {
    private final String templatePath;
    private final Map<String, ?> model;

    public ExecutingTemplate(String templatePath, Map<String, ?> model) {
      this.templatePath = templatePath;
      this.model = model;
    }
  }

  public Render(ClassLoader classLoader, FileSystem fileSystem, String templateDirPath, Buffer template, String templateName, Map<String, ?> model, final AsyncResultHandler<Buffer> handler) {
    this.classLoader = classLoader;
    this.fileSystem = fileSystem;
    this.templateCompiler = new TemplateCompiler();
    this.templateDirPath = templateDirPath;
    this.handler = handler;

    try {
      rootTemplate = execute(templateName, template, model);
    } catch (Exception e) {
      handler.handle(new AsyncResult<Buffer>(e));
      return;
    }

    if (!anyInnerTemplates) {
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
    if (executingTemplates.isEmpty() && completionHandlerFired.compareAndSet(false, true)) {
      complete();
    }
  }

  private void executeNested(final String templatePath, final Map<String, ?> model, final NestedTemplate nestedTemplate) {
    final ExecutingTemplate executingTemplate = new ExecutingTemplate(templatePath, model);
    executingTemplates.add(executingTemplate);

    String absoluteTemplatePath = templateDirPath + File.separator + templatePath;
    fileSystem.readFile(absoluteTemplatePath, new AsyncResultHandler<Buffer>() {
      @Override
      public void handle(AsyncResult<Buffer> event) {
        if (event.failed()) {
          nestedTemplate.handle(new AsyncResult<ExecutedTemplate>(event.exception));
          failTemplate(executingTemplate, event.exception);
        } else {
          ExecutedTemplate executedTemplate;
          try {
            executedTemplate = execute(templatePath, event.result, model);
          } catch (Exception e) {
            nestedTemplate.handle(new AsyncResult<ExecutedTemplate>(e));
            failTemplate(executingTemplate, e);
            return;
          }

          nestedTemplate.handle(new AsyncResult<ExecutedTemplate>(executedTemplate));
          completeTemplate(executingTemplate);
        }
      }
    });
  }

  private ExecutedTemplate execute(String templatePath, Buffer templateSource, Map<String, ?> model) throws IOException {
    CompiledTemplate compiledTemplate = templateCompiler.compile(classLoader, templateSource, templatePath);
    return compiledTemplate.execute(model, new NestedRenderer() {
      public NestedTemplate render(String templatePath, Map<String, ?> model) {
        anyInnerTemplates = true;
        final NestedTemplate nestedTemplate = new NestedTemplate();
        executeNested(templatePath, model, nestedTemplate);
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