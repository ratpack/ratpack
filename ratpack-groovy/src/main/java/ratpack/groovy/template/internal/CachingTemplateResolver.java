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

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import groovy.text.markup.MarkupTemplateEngine;
import ratpack.util.ExceptionUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class CachingTemplateResolver extends MarkupTemplateEngine.DefaultTemplateResolver {

  private final LoadingCache<String, URL> urlCache = CacheBuilder.newBuilder().build(new CacheLoader<String, URL>() {
    @Override
    public URL load(String key) throws Exception {
      return doLoad(key);
    }
  });

  private URL doLoad(String key) throws MalformedURLException {
    return templatesDir.resolve(key).toUri().toURL();
  }

  private final Path templatesDir;

  public CachingTemplateResolver(Path templatesDir) {
    this.templatesDir = templatesDir;
  }

  @Override
  public URL resolveTemplate(String templatePath) throws IOException {
    try {
      return urlCache.get(templatePath);
    } catch (ExecutionException e) {
      Throwables.propagateIfInstanceOf(e, IOException.class);
      throw ExceptionUtils.uncheck(e);
    }
  }
}
