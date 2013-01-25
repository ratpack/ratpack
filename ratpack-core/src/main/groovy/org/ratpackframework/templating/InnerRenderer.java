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

import com.hazelcast.util.ConcurrentHashSet;
import groovy.lang.Closure;
import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.file.FileSystem;

import java.io.File;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class InnerRenderer {

  private final Map<TemplateId, RenderingTemplate> allTemplates = new HashMap<TemplateId, RenderingTemplate>();
  private final List<RenderingTemplate> topLevelTemplates = new LinkedList<RenderingTemplate>();
  private final Set<RenderingTemplate> processingTemplates = new ConcurrentHashSet<RenderingTemplate>();
  private final Map<RenderingTemplate, Exception> templateErrors = new HashMap<RenderingTemplate, Exception>();

  private final AtomicBoolean completionHandlerFired = new AtomicBoolean();

  private final FileSystem fileSystem;
  private final SimpleTemplateEngine engine;
  private final String templateDirPath;

  private final AsyncResultHandler<Buffer> handler;

  private Writable rootTemplate;

  private final Writer nullWriter = new NullWriter();

  private class RenderFunction extends Closure {

    private final RenderingTemplate parent;

    private RenderFunction(RenderingTemplate parent) {
      super(InnerRenderer.this);
      this.parent = parent;
    }

    @SuppressWarnings("UnusedDeclaration")
    public RenderingTemplate doCall(String templateName) {
      return doCall(Collections.emptyMap(), templateName);
    }

    public RenderingTemplate doCall(Map<?, ?> model, String templateName) {
      TemplateId id = new TemplateId(templateName, model);
      RenderingTemplate template = allTemplates.get(id);
      if (template == null) {
        template = createTemplate(parent, templateName, model);
        allTemplates.put(id, template);
      }

      return template;
    }
  }

  private RenderingTemplate createTemplate(final RenderingTemplate parent, String templateName, final Map<?, ?> model) {
    final RenderingTemplate template = new RenderingTemplate(new TemplateId(templateName, model));
    allTemplates.put(template.getId(), template);
    if (parent == null) {
      topLevelTemplates.add(template);
    } else {
      parent.getChildren().add(template);
    }
    processingTemplates.add(template);

    fileSystem.readFile(templateDirPath + File.separator + templateName, new AsyncResultHandler<Buffer>() {
      @Override
      public void handle(AsyncResult<Buffer> event) {
        if (event.failed()) {
          failTemplate(template, event.exception);
        } else {
          Buffer buffer = event.result;
          try {
            Writable writable = render(template, buffer, model);
            writable.writeTo(nullWriter);
            template.handle(writable);
            completeTemplate(template);
          } catch (Exception e) {
            failTemplate(template, e);
          }
        }
      }
    });

    return template;
  }

  public InnerRenderer(ClassLoader classLoader, FileSystem fileSystem, String templateDirPath, Buffer template, Map<?, ?> model, AsyncResultHandler<Buffer> handler) {
    this.fileSystem = fileSystem;
    this.engine = new SimpleTemplateEngine(classLoader);
    this.templateDirPath = templateDirPath;
    this.handler = handler;

    try {
      rootTemplate = render(null, template, model);
      rootTemplate.writeTo(nullWriter);
    } catch (Exception e) {
      handler.handle(new AsyncResult<Buffer>(e));
    }

    if (allTemplates.isEmpty()) {
      complete();
    }
  }

  private void failTemplate(RenderingTemplate template, Exception error) {
    //noinspection ThrowableResultOfMethodCallIgnored
    templateErrors.put(template, error);
    completeTemplate(template);
  }

  private void completeTemplate(RenderingTemplate template) {
    processingTemplates.remove(template);
    if (processingTemplates.isEmpty() && completionHandlerFired.compareAndSet(false, true)) {
      complete();
    }
  }

  private Writable render(RenderingTemplate parent, Buffer template, Map<?, ?> model) throws Exception {
    Map<String, Object> binding = new HashMap<String, Object>();
    binding.put("render", new RenderFunction(parent));
    binding.put("model", model);
    return engine.createTemplate(template.toString()).make(binding);
  }

  private void complete() {
    if (!templateErrors.isEmpty()) {
      handler.handle(new AsyncResult<Buffer>(new CompositeException(templateErrors.values())));
      return;
    }

    for (RenderingTemplate template : topLevelTemplates) {
      template.complete();
    }

    Buffer buffer = new Buffer();

    try {
      rootTemplate.writeTo(new BufferWriter(buffer));
    } catch (Exception e) {
      handler.handle(new AsyncResult<Buffer>(e));
      return;
    }

    handler.handle(new AsyncResult<Buffer>(buffer));
  }

}