/*
 * Copyright 2015 the original author or authors.
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

package ratpack.site;

import ratpack.codahale.metrics.CodaHaleMetricsModule;
import ratpack.func.Block;
import ratpack.func.Pair;
import ratpack.groovy.template.MarkupTemplateModule;
import ratpack.groovy.template.TextTemplateModule;
import ratpack.guice.Guice;
import ratpack.jackson.guice.JacksonModule;
import ratpack.newrelic.NewRelicModule;
import ratpack.registry.Registry;
import ratpack.rx.RxRatpack;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfig;
import ratpack.site.github.GitHubApi;
import ratpack.site.github.GitHubData;
import ratpack.site.github.RatpackVersion;
import ratpack.site.github.RatpackVersions;
import asset.pipeline.ratpack.AssetPipelineModule;

import static ratpack.groovy.Groovy.groovyMarkupTemplate;

public class SiteMain {
  public static void main(String... args) throws Exception {
    RatpackServer.start(b -> {
        RxRatpack.initialize();
        ServerConfig serverConfig = ServerConfig.findBaseDir()
          .env()
          .require("/github", SiteModule.GitHubConfig.class)
          .build();

        b
          .serverConfig(serverConfig)
          .registry(
            Guice.registry(s -> s
                .module(JacksonModule.class)
                .module(NewRelicModule.class)
                .module(new AssetPipelineModule())
                .module(new CodaHaleMetricsModule(), c ->
                    c.csv(csv -> csv.enable(false))
                )
                .module(SiteModule.class)
                .module(MarkupTemplateModule.class, conf -> {
                  conf.setAutoNewLine(true);
                  conf.setUseDoubleQuotes(true);
                  conf.setAutoIndent(true);
                })
                .module(TextTemplateModule.class, conf ->
                    conf.setStaticallyCompile(true)
                )
            )
          )
          .handlers(c -> {

            int longCache = 60 * 60 * 24 * 365; // one year
            int shortCache = 60 * 10; // ten mins

            c
              .all(ctx -> {
                //noinspection ConstantConditions
                String host = ctx.getRequest().getHeaders().get("host");
                if (host != null && (host.endsWith("ratpack-framework.org") || host.equals("www.ratpack.io"))) {
                  ctx.redirect(301, "http://ratpack.io" + ctx.getRequest().getRawUri());
                  return;
                }

                if (ctx.getRequest().getPath().isEmpty() || ctx.getRequest().getPath().equals("index.html")) {
                  ctx.getResponse().getHeaders().set("X-UA-Compatible", "IE=edge,chrome=1");
                }

                ctx.next();
              })

              .prefix("assets", assets -> assets
                  .all(ctx -> {
                    int cacheFor = ctx.getRequest().getQuery().isEmpty() ? shortCache : longCache;
                    ctx.getResponse().getHeaders().add("Cache-Control", "max-age=" + cacheFor + ", public");
                    ctx.next();
                  })
                  .files(f -> f.dir("assets").indexFiles("index.html"))
              )

              .get("index.html", ctx -> {
                ctx.redirect(301, "/");
              })

              .get(ctx -> ctx.render(groovyMarkupTemplate("index.gtpl")))

              .path("reset", ctx -> {
                GitHubApi gitHubApi = ctx.get(GitHubApi.class);
                ctx.byMethod(methods -> {
                  Block impl = () -> {
                    gitHubApi.invalidateCache();
                    ctx.render("ok");
                  };
                  if (ctx.getServerConfig().isDevelopment()) {
                    methods.get(impl);
                  }
                  methods.post(impl);
                });
              })

              .prefix("versions", v -> v
                  .get(ctx ->
                      ctx.render(
                        ctx.get(RatpackVersions.class).getAll()
                          .map(all -> groovyMarkupTemplate("versions.gtpl", m -> m.put("versions", all)))
                      )
                  )
                  .get(":version", ctx ->
                      ctx.render(
                        ctx.get(RatpackVersions.class).getAll()
                          .map(all -> all.version(ctx.getAllPathTokens().get("version")))
                          .onNull(() -> ctx.clientError(404))
                          .flatMap(version -> ctx.get(GitHubData.class).closed(version).map(i -> Pair.of(version, i)))
                          .map(p -> groovyMarkupTemplate("version.gtpl", m -> m.put("version", p.left).put("issues", p.right)))
                      )
                  )
              )

              .prefix("manual", c1 -> c1
                  .fileSystem("manual", c2 -> c2
                      .get(ctx -> ctx.redirect(301, "manual/current"))
                      .prefix(":label", c3 -> c3
                          .all(ctx -> {
                            String label = ctx.getPathTokens().get("label");

                            ctx.get(RatpackVersions.class).getAll().then(all -> {
                              if (label.equals("current") || all.isReleased(label)) {
                                ctx.getResponse().getHeaders().add("Cache-Control", "max-age=" + longCache + ", public");
                              } else if (label.equals("snapshot") || all.isUnreleased(label)) {
                                ctx.getResponse().getHeaders().add("Cache-Control", "max-age=" + shortCache + ", public");
                              }

                              RatpackVersion version;
                              switch (label) {
                                case "current":
                                  version = all.getCurrent();
                                  break;
                                case "snapshot":
                                  version = all.getSnapshot();
                                  break;
                                default:
                                  version = all.version(label);
                                  if (version == null) {
                                    ctx.clientError(404);
                                    return;
                                  }
                                  break;
                              }

                              ctx.next(Registry.single(ctx.getFileSystemBinding().binding(version.getVersion())));
                            });
                          })
                          .files(f -> f.indexFiles("index.html"))
                      )
                  )

              )

              .get("favicon.ico", ctx -> {
                ctx.getResponse().getHeaders().add("Cache-Control", "max-age=" + longCache + ", public");
                ctx.next();
              })
              .files(f -> f.dir("public").indexFiles("index.html"));
          });
      }
    );
  }
}
