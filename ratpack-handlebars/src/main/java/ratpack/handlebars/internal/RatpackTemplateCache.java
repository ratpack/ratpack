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

package ratpack.handlebars.internal;

import com.github.jknack.handlebars.Parser;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.cache.Cache;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class RatpackTemplateCache implements TemplateCache {

  private final boolean reloadable;
  private final Cache<TemplateKey, Template> cache;

  public RatpackTemplateCache(boolean reloadable, Cache<TemplateKey, Template> cache) {
    this.reloadable = reloadable;
    this.cache = cache;
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }

  @Override
  public void evict(TemplateSource source) {
    cache.invalidate(new TemplateKey(source, reloadable));
  }

  @Override
  public Template get(final TemplateSource source, final Parser parser) throws IOException {
    try {
      return cache.get(new TemplateKey(source, reloadable), new Callable<Template>() {
        @Override
        public Template call() throws Exception {
          return parser.parse(source);
        }
      });
    } catch (ExecutionException e) {
      throw new IOException("Can't parse " + source, e);
    }
  }
}
