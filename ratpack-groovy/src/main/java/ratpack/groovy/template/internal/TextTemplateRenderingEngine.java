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
import ratpack.file.FileSystemBinding;
import ratpack.groovy.script.internal.ScriptEngine;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TextTemplateRenderingEngine {

  private final ConcurrentMap<TextTemplateSource, CompiledTextTemplate> compiledTemplateCache;
  private final TextTemplateCompiler templateCompiler;
  private final ByteBufAllocator byteBufAllocator;
  private final boolean reloadable;
  private final FileSystemBinding templateDir;

  @Inject
  public TextTemplateRenderingEngine(ByteBufAllocator byteBufAllocator, FileSystemBinding templateDir, boolean reloadable, boolean staticCompile) {
    this.byteBufAllocator = byteBufAllocator;
    this.reloadable = reloadable;
    this.templateDir = templateDir;

    ScriptEngine<DefaultTextTemplateScript> scriptEngine = new ScriptEngine<>(getClass().getClassLoader(), staticCompile, DefaultTextTemplateScript.class);
    this.templateCompiler = new TextTemplateCompiler(scriptEngine, byteBufAllocator);
    this.compiledTemplateCache = new ConcurrentHashMap<>();
  }

  private CompiledTextTemplate get(TextTemplateSource templateSource) {
    return compiledTemplateCache.computeIfAbsent(templateSource, this::compile);
  }

  private CompiledTextTemplate compile(TextTemplateSource templateSource) {
    ByteBuf content;
    try {
      content = templateSource.getContent();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    try {
      return templateCompiler.compile(content, templateSource.getName());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      content.release();
    }
  }

  public Promise<ByteBuf> renderTemplate(String templateId, Map<String, ?> model) throws Exception {
    Path templateFile = getTemplateFile(templateId);
    return render(toTemplateSource(templateId, templateFile), model);
  }

  private TextTemplateSource toTemplateSource(String templateId, Path templateFile) throws IOException {
    String id = templateId + (reloadable ? Files.getLastModifiedTime(templateFile) : "0");
    return new TextTemplateSource(byteBufAllocator, id, templateFile, templateId);
  }

  private Promise<ByteBuf> render(final TextTemplateSource templateSource, Map<String, ?> model) throws Exception {
    return Render.render(byteBufAllocator, this::get, templateSource, model, templateName -> toTemplateSource(templateName, getTemplateFile(templateName)));
  }

  private Path getTemplateFile(String templateName) {
    return templateDir.file(templateName);
  }

}
