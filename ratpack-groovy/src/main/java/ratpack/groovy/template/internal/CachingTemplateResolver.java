/*
 * Copyright 2014 the original author or authors.
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

import groovy.text.markup.MarkupTemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingTemplateResolver extends MarkupTemplateEngine.DefaultTemplateResolver {

  private final ConcurrentMap<String, URL> urlCache = new ConcurrentHashMap<>();

  private URL doLoad(String key) {
    try {
      return templatesDir.resolve(key).toUri().toURL();
    } catch (MalformedURLException e) {
      throw new UncheckedIOException(e);
    }
  }

  private final Path templatesDir;

  public CachingTemplateResolver(Path templatesDir) {
    this.templatesDir = templatesDir;
  }

  @Override
  public URL resolveTemplate(String templatePath) throws IOException {
    return urlCache.computeIfAbsent(templatePath, this::doLoad);
  }
}
