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
package ratpack.site.assets;


import asset.pipeline.ratpack.internal.ProductionAssetCache;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.handling.Handlers;

import java.util.Map;

public class AssetPipelineModule extends ConfigurableModule<AssetPipelineModule.Config> {

  public static class Config {
    private String sourcePath = "../assets";
    private String url = "assets/";
    private String indexFile = "index.html";
    private Map<String, Object> assets;

    public Config sourcePath(String sourcePath) {
      this.sourcePath = sourcePath;
      return this;
    }

    public String getSourcePath() {
      return this.sourcePath;
    }

    public Config url(String url) {
      this.url = url;
      return this;
    }

    public String getUrl() {
      return this.url;
    }

    public Config indexFile(String indexFile) {
      this.indexFile = indexFile;
      return this;
    }

    public String getIndexFile() {
      return this.indexFile;
    }

    public Config assets(Map<String, Object> assets) {
      this.assets = assets;
      return this;
    }

    public Map<String, Object> getAssets() {
      return this.assets;
    }

  }

  @Override
  protected void configure() {
    bind(AssetPipelineService.class).in(Singleton.class);
    bind(AssetPipelineHandler.class).in(Singleton.class);
    bind(ProductionAssetCache.class).in(Singleton.class);

    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toInstance((registry, rest) ->
      Handlers.chain(rest, Handlers.chain(registry, c -> c.all(AssetPipelineHandler.class)))
    );
  }

}
